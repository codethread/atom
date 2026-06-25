package command

import (
	"bytes"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func run(args ...string) (string, error) {
	var out, er bytes.Buffer
	err := New(&out, &er).Run(args)
	return out.String() + er.String(), err
}

func TestHelpIncludesCommandTree(t *testing.T) {
	out, err := run("--help")
	if err != nil {
		t.Fatal(err)
	}
	for _, want := range []string{"init", "add <title>", "update <id>", "show <id>", "list [--query name]", "ready [--query name]", "daemon start [--config trusted.edn]", "daemon status", "daemon stop"} {
		if !strings.Contains(out, want) {
			t.Fatalf("help missing %q in:\n%s", want, out)
		}
	}
}

func TestRejectsRemovedAndMalformedInputs(t *testing.T) {
	cases := [][]string{
		{"--format", "edn", "list"},
		{"list", "--where", "[:= :status \"todo\"]"},
		{"list", "--where", ""},
		{"list", "extra"},
		{"ready", "--query", "q", "extra"},
		{"add", "x", "extra"},
		{"add", "x", "--status", "bogus"},
		{"add", "x", "--attr", "novalue"},
		{"update", "id", "extra"},
		{"update", "id", "--edge", "depends-on"},
		{"update", "id", "--edge", ":target"},
		{"update", "id", "--edge", "depends-on:"},
		{"list", "--param", "novalue"},
	}
	for _, c := range cases {
		if _, err := run(c...); err == nil {
			t.Fatalf("expected error for %v", c)
		}
	}
}

func TestClientConfigPrecedenceAndValidation(t *testing.T) {
	dir := t.TempDir()
	cfg := filepath.Join(dir, "config.json")
	if err := os.WriteFile(cfg, []byte(`{"db":"from-config.sqlite","format":"json"}`), 0644); err != nil {
		t.Fatal(err)
	}
	opts, rest, err := Resolve([]string{"--client-config", cfg, "--db", "from-flag.sqlite", "--format", "human", "list"})
	if err != nil {
		t.Fatal(err)
	}
	if opts.DB != "from-flag.sqlite" || opts.Format != "human" || len(rest) != 1 || rest[0] != "list" {
		t.Fatalf("unexpected resolved options/rest: %#v %#v", opts, rest)
	}

	bad := filepath.Join(dir, "bad.json")
	if err := os.WriteFile(bad, []byte(`{"where":"nope"}`), 0644); err != nil {
		t.Fatal(err)
	}
	if _, err := run("--client-config", bad, "list"); err == nil || !strings.Contains(err.Error(), "unsupported client config key") {
		t.Fatalf("expected unsupported key error, got %v", err)
	}

	malformed := filepath.Join(dir, "malformed.json")
	if err := os.WriteFile(malformed, []byte(`{"db":`), 0644); err != nil {
		t.Fatal(err)
	}
	if _, err := run("--client-config", malformed, "list"); err == nil || !strings.Contains(err.Error(), "malformed client config") {
		t.Fatalf("expected malformed config error, got %v", err)
	}

	wrongType := filepath.Join(dir, "wrong-type.json")
	if err := os.WriteFile(wrongType, []byte(`{"db":123}`), 0644); err != nil {
		t.Fatal(err)
	}
	if _, err := run("--client-config", wrongType, "list"); err == nil || !strings.Contains(err.Error(), "client config db must be a string") {
		t.Fatalf("expected wrong type config error, got %v", err)
	}
}

func TestXDGConfigLoading(t *testing.T) {
	dir := t.TempDir()
	t.Setenv("XDG_CONFIG_HOME", dir)
	path := filepath.Join(dir, "todo")
	if err := os.MkdirAll(path, 0755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(path, "config.json"), []byte(`{"format":"json"}`), 0644); err != nil {
		t.Fatal(err)
	}
	_, err := run("list")
	if err == nil || !strings.Contains(err.Error(), "list is not wired") {
		t.Fatalf("expected xdg config to parse and reach stub, got %v", err)
	}
}
