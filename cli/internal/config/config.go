package config

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
)

const DefaultDB = "todo.sqlite"
const DefaultFormat = "human"

var allowedKeys = map[string]bool{"db": true, "format": true}

type Config struct {
	DB     string `json:"db"`
	Format string `json:"format"`
}

func DefaultPath() (string, error) {
	if xdg := os.Getenv("XDG_CONFIG_HOME"); xdg != "" {
		return filepath.Join(xdg, "todo", "config.json"), nil
	}
	home, err := os.UserHomeDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(home, ".config", "todo", "config.json"), nil
}

func Load(path string) (Config, error) {
	if path == "" {
		var err error
		path, err = DefaultPath()
		if err != nil {
			return Config{}, err
		}
	}
	b, err := os.ReadFile(path)
	if os.IsNotExist(err) {
		return Config{}, nil
	}
	if err != nil {
		return Config{}, err
	}
	var raw map[string]json.RawMessage
	if err := json.Unmarshal(b, &raw); err != nil {
		return Config{}, fmt.Errorf("malformed client config: %w", err)
	}
	for k := range raw {
		if !allowedKeys[k] {
			return Config{}, fmt.Errorf("unsupported client config key: %s", k)
		}
	}
	var c Config
	if v, ok := raw["db"]; ok {
		if err := json.Unmarshal(v, &c.DB); err != nil {
			return Config{}, fmt.Errorf("client config db must be a string")
		}
	}
	if v, ok := raw["format"]; ok {
		if err := json.Unmarshal(v, &c.Format); err != nil {
			return Config{}, fmt.Errorf("client config format must be a string")
		}
	}
	if c.Format != "" && c.Format != "human" && c.Format != "json" {
		return Config{}, fmt.Errorf("unsupported format: %s", c.Format)
	}
	return c, nil
}
