package config

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

const ConfigFileName = "config.json"
const DefaultDBFileName = "skein.sqlite"

var allowedKeys = map[string]bool{"configFormat": true, "source": true}

type World struct {
	ConfigDir  string
	StateDir   string
	DataDir    string
	ConfigFile string
	DBPath     string
}

type Config struct {
	ConfigFormat string `json:"configFormat"`
	Source       string `json:"source"`
}

func RepoWorld() (World, error) {
	cwd, err := os.Getwd()
	if err != nil {
		return World{}, err
	}
	for dir := filepath.Clean(cwd); ; dir = filepath.Dir(dir) {
		candidate := filepath.Join(dir, ".skein")
		if st, err := os.Stat(candidate); err == nil && st.IsDir() {
			return isolatedWorld(candidate)
		} else if err != nil && !os.IsNotExist(err) {
			return World{}, err
		}
		parent := filepath.Dir(dir)
		if parent == dir {
			break
		}
	}
	return World{}, fmt.Errorf("no .skein directory found from current directory upward; run `strand init` or pass --config-dir")
}

func SelectedWorld(configDir string) (World, error) {
	if configDir != "" {
		return isolatedWorld(configDir)
	}
	return RepoWorld()
}

func InitWorld(configDir string) (World, error) {
	if configDir != "" {
		return isolatedWorld(configDir)
	}
	cwd, err := os.Getwd()
	if err != nil {
		return World{}, err
	}
	root := cwd
	cmd := exec.Command("git", "rev-parse", "--show-toplevel")
	cmd.Dir = cwd
	if out, err := cmd.CombinedOutput(); err == nil {
		root = strings.TrimSpace(string(out))
	} else if exit, ok := err.(*exec.ExitError); !ok || exit.ExitCode() != 128 || !strings.Contains(string(out), "not a git repository") {
		return World{}, fmt.Errorf("failed to discover Git root for init: %w", err)
	}
	return isolatedWorld(filepath.Join(root, ".skein"))
}

func isolatedWorld(configDir string) (World, error) {
	abs, err := filepath.Abs(configDir)
	if err != nil {
		return World{}, err
	}
	if real, err := filepath.EvalSymlinks(abs); err == nil {
		abs = real
	}
	abs = filepath.Clean(abs)
	return world(abs, filepath.Join(abs, "state"), filepath.Join(abs, "data")), nil
}

func world(configDir, stateDir, dataDir string) World {
	return World{ConfigDir: configDir, StateDir: stateDir, DataDir: dataDir, ConfigFile: filepath.Join(configDir, ConfigFileName), DBPath: filepath.Join(dataDir, DefaultDBFileName)}
}

func Load(configDir string) (Config, World, error) {
	w, err := SelectedWorld(configDir)
	if err != nil {
		return Config{}, World{}, err
	}
	b, err := os.ReadFile(w.ConfigFile)
	if os.IsNotExist(err) {
		return Config{}, w, nil
	}
	if err != nil {
		return Config{}, World{}, err
	}
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(b, &raw); err != nil {
		return Config{}, World{}, fmt.Errorf("malformed client config: %w", err)
	}
	for k := range raw {
		if !allowedKeys[k] {
			return Config{}, World{}, fmt.Errorf("unsupported client config key: %s", k)
		}
	}
	var c Config
	if v, ok := raw["configFormat"]; ok {
		if err := json.Unmarshal(v, &c.ConfigFormat); err != nil {
			return Config{}, World{}, fmt.Errorf("client config configFormat must be a string")
		}
	} else {
		return Config{}, World{}, fmt.Errorf("client config configFormat is required")
	}
	if c.ConfigFormat != "alpha" {
		return Config{}, World{}, fmt.Errorf("unsupported client config configFormat: %s", c.ConfigFormat)
	}
	if v, ok := raw["source"]; ok {
		if err := json.Unmarshal(v, &c.Source); err != nil {
			return Config{}, World{}, fmt.Errorf("client config source must be a string")
		}
	}
	return c, w, nil
}

func ResolveSource(source string) (string, error) {
	if source == "" {
		return "", fmt.Errorf("client config source is required for weaver lifecycle commands; set source in %s", ConfigFileName)
	}
	resolvedSource := source
	if source == "~" || strings.HasPrefix(source, "~/") {
		home, err := os.UserHomeDir()
		if err != nil {
			return "", err
		}
		if source == "~" {
			resolvedSource = home
		} else {
			resolvedSource = filepath.Join(home, source[2:])
		}
	}
	if !filepath.IsAbs(resolvedSource) {
		return "", fmt.Errorf("client config source must be an absolute path: %s", resolvedSource)
	}
	if st, err := os.Stat(resolvedSource); err != nil || !st.IsDir() {
		return "", fmt.Errorf("client config source must be an existing directory: %s", resolvedSource)
	}
	if st, err := os.Stat(filepath.Join(resolvedSource, "deps.edn")); err != nil || st.IsDir() {
		return "", fmt.Errorf("client config source must contain deps.edn: %s", resolvedSource)
	}
	return resolvedSource, nil
}
