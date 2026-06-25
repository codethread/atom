package command

import (
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"atom-todo-cli/internal/client"
	"atom-todo-cli/internal/config"
)

type App struct{ Stdout, Stderr io.Writer }
type Options struct{ DB, Format, ClientConfig string }
type ExitError struct {
	Code int
	Err  error
}

func (e *ExitError) Error() string { return e.Err.Error() }
func (e *ExitError) ExitCode() int { return e.Code }

type Caller interface {
	Call(string, map[string]any) (any, error)
}

var newClient = func(o Options) Caller { return client.New(client.Config{DB: o.DB, Format: o.Format}) }

func New(out, err io.Writer) *App { return &App{Stdout: out, Stderr: err} }

func (a *App) Run(args []string) error {
	if a.Stdout == nil {
		a.Stdout = os.Stdout
	}
	if a.Stderr == nil {
		a.Stderr = os.Stderr
	}
	opts, rest, err := Resolve(args)
	if err != nil {
		return err
	}
	if len(rest) == 0 {
		usage(a.Stdout)
		return nil
	}
	return a.runCommand(opts, rest)
}

func Resolve(args []string) (Options, []string, error) {
	opts, rest, err := parseGlobal(args)
	if err != nil {
		return opts, rest, err
	}
	cfg, err := config.Load(opts.ClientConfig)
	if err != nil {
		return opts, rest, err
	}
	if opts.DB == "" {
		opts.DB = cfg.DB
	}
	if opts.DB == "" {
		opts.DB = config.DefaultDB
	}
	if opts.Format == "" {
		opts.Format = cfg.Format
	}
	if opts.Format == "" {
		opts.Format = config.DefaultFormat
	}
	if opts.Format != "human" && opts.Format != "json" {
		return opts, rest, fmt.Errorf("unsupported format: %s", opts.Format)
	}
	return opts, rest, nil
}

func parseGlobal(args []string) (Options, []string, error) {
	var o Options
	for i := 0; i < len(args); i++ {
		s := args[i]
		if !strings.HasPrefix(s, "-") {
			return o, args[i:], nil
		}
		switch s {
		case "--db":
			i++
			if i >= len(args) {
				return o, nil, errors.New("--db requires a value")
			}
			o.DB = args[i]
		case "--format":
			i++
			if i >= len(args) {
				return o, nil, errors.New("--format requires a value")
			}
			o.Format = args[i]
		case "--client-config":
			i++
			if i >= len(args) {
				return o, nil, errors.New("--client-config requires a value")
			}
			o.ClientConfig = args[i]
		case "--where":
			return o, nil, errors.New("--where is not supported by the Go CLI; use --query")
		case "-h", "--help":
			return o, []string{"help"}, nil
		default:
			return o, nil, fmt.Errorf("unknown global option: %s", s)
		}
	}
	return o, nil, nil
}

func (a *App) runCommand(o Options, args []string) error {
	switch args[0] {
	case "help":
		usage(a.Stdout)
		return nil
	case "init":
		if err := noArgs(args[1:]); err != nil {
			return err
		}
		return a.call(o, "init", map[string]any{})
	case "add":
		return a.parseAdd(o, args[1:])
	case "update":
		return a.parseUpdate(o, args[1:])
	case "show":
		if len(args) != 2 {
			return errors.New("show requires exactly one id")
		}
		return a.call(o, "show", map[string]any{"id": args[1]})
	case "list":
		return a.parseQueryish(o, "list", args[1:])
	case "ready":
		return a.parseQueryish(o, "ready", args[1:])
	case "daemon":
		return a.parseDaemon(o, args[1:])
	default:
		return fmt.Errorf("unknown command: %s", args[0])
	}
}

func usage(w io.Writer) {
	fmt.Fprintln(w, `Usage: todo [--db path] [--client-config path] [--format human|json] <command> [args]

Commands:
  init
  add <title> [--status todo|done|failed|cancelled] [--attr key=value ...]
  update <id> [--title title] [--status todo|done|failed|cancelled] [--attr key=value ...] [--edge edge-type:to-id ...]
  show <id>
  list [--query name] [--param key=value ...]
  ready [--query name] [--param key=value ...]
  daemon start [--config trusted.edn]
  daemon status
  daemon stop`)
}

func (a *App) parseAdd(o Options, args []string) error {
	if len(args) == 0 {
		return errors.New("add requires a title")
	}
	fs := flag.NewFlagSet("add", flag.ContinueOnError)
	fs.SetOutput(io.Discard)
	status := fs.String("status", "todo", "")
	attrs := multiFlag{}
	fs.Var(&attrs, "attr", "")
	if err := fs.Parse(args[1:]); err != nil {
		return err
	}
	if fs.NArg() != 0 {
		return errors.New("add received unexpected arguments")
	}
	if err := validStatus(*status); err != nil {
		return err
	}
	am, err := parseKV(attrs, "--attr")
	if err != nil {
		return err
	}
	return a.call(o, "add", map[string]any{"title": args[0], "status": *status, "attributes": am})
}
func (a *App) parseUpdate(o Options, args []string) error {
	if len(args) == 0 {
		return errors.New("update requires an id")
	}
	fs := flag.NewFlagSet("update", flag.ContinueOnError)
	fs.SetOutput(io.Discard)
	status := fs.String("status", "", "")
	title := fs.String("title", "", "")
	attrs := multiFlag{}
	edges := multiFlag{}
	fs.Var(&attrs, "attr", "")
	fs.Var(&edges, "edge", "")
	if err := fs.Parse(args[1:]); err != nil {
		return err
	}
	if fs.NArg() != 0 {
		return errors.New("update received unexpected arguments")
	}
	if *status != "" {
		if err := validStatus(*status); err != nil {
			return err
		}
	}
	am, err := parseKV(attrs, "--attr")
	if err != nil {
		return err
	}
	edgeRows := make([]map[string]any, 0, len(edges))
	for _, v := range edges {
		left, right, ok := strings.Cut(v, ":")
		if !ok || left == "" || right == "" {
			return fmt.Errorf("malformed --edge: %s", v)
		}
		edgeRows = append(edgeRows, map[string]any{"type": left, "to": right})
	}
	titleSet := false
	fs.Visit(func(f *flag.Flag) {
		if f.Name == "title" {
			titleSet = true
		}
	})
	var titleArg any
	if titleSet {
		titleArg = *title
	}
	var statusArg any
	if *status != "" {
		statusArg = *status
	}
	var attrArg any
	if len(attrs) > 0 {
		attrArg = am
	}
	return a.call(o, "update", map[string]any{"id": args[0], "title": titleArg, "status": statusArg, "attributes": attrArg, "edges": edgeRows})
}

func (a *App) call(o Options, op string, args map[string]any) error {
	result, err := newClient(o).Call(op, args)
	if err != nil {
		return err
	}
	if o.Format == "json" {
		b, err := json.Marshal(result)
		if err != nil {
			return err
		}
		_, err = fmt.Fprintln(a.Stdout, string(b))
		return err
	}
	switch op {
	case "status", "stop":
		return a.writeHumanJSON(result)
	case "add":
		if m, ok := result.(map[string]any); ok {
			if id, ok := m["id"]; ok {
				_, err := fmt.Fprintln(a.Stdout, id)
				return err
			}
		}
	case "show":
		return a.writeHumanJSON(result)
	case "list", "ready", "list-query", "ready-query":
		return a.writeHumanRows(result)
	}
	return nil
}

func (a *App) writeHumanJSON(result any) error {
	if result == nil {
		return nil
	}
	b, err := json.Marshal(result)
	if err != nil {
		return err
	}
	_, err = fmt.Fprintln(a.Stdout, string(b))
	return err
}

func (a *App) writeHumanRows(result any) error {
	rows, ok := result.([]any)
	if !ok {
		return a.writeHumanJSON(result)
	}
	if len(rows) == 0 {
		_, err := fmt.Fprintln(a.Stdout, "(no rows)")
		return err
	}
	for _, row := range rows {
		if err := a.writeHumanJSON(row); err != nil {
			return err
		}
	}
	return nil
}

func (a *App) parseQueryish(o Options, op string, args []string) error {
	fs := flag.NewFlagSet(op, flag.ContinueOnError)
	fs.SetOutput(io.Discard)
	query := fs.String("query", "", "")
	params := multiFlag{}
	fs.Var(&params, "param", "")
	fs.String("where", "", "")
	if err := fs.Parse(args); err != nil {
		return err
	}
	whereSet := false
	querySet := false
	fs.Visit(func(f *flag.Flag) {
		if f.Name == "where" {
			whereSet = true
		}
		if f.Name == "query" {
			querySet = true
		}
	})
	if whereSet {
		return errors.New("--where is not supported by the Go CLI; use --query")
	}
	if fs.NArg() != 0 {
		return fmt.Errorf("%s received unexpected arguments", op)
	}
	pm, err := parseKV(params, "--param")
	if err != nil {
		return err
	}
	if *query == "" {
		if querySet {
			return errors.New("--query requires a non-empty name")
		}
		if len(params) > 0 {
			return errors.New("--param requires --query")
		}
		return a.call(o, op, map[string]any{})
	}
	return a.call(o, op+"-query", map[string]any{"query": *query, "params": pm})
}
func (a *App) parseDaemon(o Options, args []string) error {
	if len(args) == 0 {
		return errors.New("daemon requires start, status, or stop")
	}
	switch args[0] {
	case "start":
		fs := flag.NewFlagSet("daemon start", flag.ContinueOnError)
		fs.SetOutput(io.Discard)
		configFile := fs.String("config", "", "")
		if err := fs.Parse(args[1:]); err != nil {
			return err
		}
		if fs.NArg() != 0 {
			return errors.New("daemon start received unexpected arguments")
		}
		return a.launchDaemon(o, *configFile)
	case "status", "stop":
		if len(args) != 1 {
			return fmt.Errorf("daemon %s received unexpected arguments", args[0])
		}
		return a.call(o, args[0], map[string]any{})
	default:
		return fmt.Errorf("unknown daemon command: %s", args[0])
	}
}

func (a *App) launchDaemon(o Options, configFile string) error {
	dbPath, err := filepath.Abs(o.DB)
	if err != nil {
		return err
	}
	args := []string{"-M:todo", "--db", dbPath, "daemon", "start"}
	if configFile != "" {
		configPath, err := filepath.Abs(configFile)
		if err != nil {
			return err
		}
		args = append(args, "--config", configPath)
	}
	cmd := exec.Command("clojure", args...)
	if _, err := os.Stat("deps.edn"); err != nil {
		if _, parentErr := os.Stat(filepath.Join("..", "deps.edn")); parentErr == nil {
			cmd.Dir = ".."
		}
	}
	cmd.Stdin = os.Stdin
	cmd.Stdout = a.Stdout
	cmd.Stderr = a.Stderr
	if err := cmd.Run(); err != nil {
		if exit, ok := err.(*exec.ExitError); ok {
			return &ExitError{Code: exit.ExitCode(), Err: err}
		}
		return err
	}
	return nil
}
func noArgs(args []string) error {
	if len(args) > 0 {
		return errors.New("unexpected arguments")
	}
	return nil
}
func validStatus(s string) error {
	switch s {
	case "todo", "done", "failed", "cancelled":
		return nil
	}
	return fmt.Errorf("invalid status: %s", s)
}
func parseKV(vals []string, name string) (map[string]any, error) {
	m := map[string]any{}
	for _, s := range vals {
		k, v, ok := strings.Cut(s, "=")
		if !ok || k == "" {
			return nil, fmt.Errorf("malformed %s: %s", name, s)
		}
		m[k] = v
	}
	return m, nil
}

type multiFlag []string

func (m *multiFlag) String() string     { return strings.Join(*m, ",") }
func (m *multiFlag) Set(v string) error { *m = append(*m, v); return nil }
