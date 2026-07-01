package config

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
)

const DefaultInitCLJ = "(require '[skein.runtime.alpha :as runtime])\n\n(runtime/sync!)\n"
const DefaultSkeinGitignore = "config.json\ninit.local.clj\nlibs.local.edn\nstate/\ndata/\nweaver.*\n*.sqlite\n*.sqlite-*\n"

func BootstrapWorld(cwd, configDir, source string) (World, error) {
	world, err := BootstrapTargetWorld(cwd, configDir)
	if err != nil {
		return World{}, err
	}
	if err := os.MkdirAll(filepath.Join(world.ConfigDir, "libs"), 0o755); err != nil {
		return World{}, err
	}
	if _, err := os.Stat(world.ConfigFile); os.IsNotExist(err) {
		data, err := json.Marshal(Config{ConfigFormat: "alpha"})
		if err != nil {
			return World{}, err
		}
		if err := os.WriteFile(world.ConfigFile, append(data, '\n'), 0o644); err != nil {
			return World{}, err
		}
	} else if err != nil {
		return World{}, err
	}
	if err := writeMissing(filepath.Join(world.ConfigDir, "libs.edn"), "{:libs {}}\n"); err != nil {
		return World{}, err
	}
	if err := writeMissing(filepath.Join(world.ConfigDir, "init.clj"), DefaultInitCLJ); err != nil {
		return World{}, err
	}
	if err := writeMissing(filepath.Join(world.ConfigDir, ".gitignore"), DefaultSkeinGitignore); err != nil {
		return World{}, err
	}
	if _, _, err := Load(world.ConfigDir); err != nil {
		return World{}, err
	}
	return world, nil
}

func BootstrapTargetWorld(cwd, configDir string) (World, error) {
	if configDir != "" {
		if !filepath.IsAbs(configDir) {
			if cwd == "" {
				var err error
				cwd, err = os.Getwd()
				if err != nil {
					return World{}, err
				}
			}
			if realCWD, err := filepath.EvalSymlinks(cwd); err == nil {
				cwd = realCWD
			}
			configDir = filepath.Join(cwd, configDir)
		}
		return isolatedWorld(configDir)
	}
	root, err := GitRoot(cwd)
	if err != nil {
		return World{}, err
	}
	return isolatedWorld(filepath.Join(root, ".skein"))
}

func GitRoot(cwd string) (string, error) {
	if cwd == "" {
		var err error
		cwd, err = os.Getwd()
		if err != nil {
			return "", err
		}
	}
	cmd := exec.Command("git", "rev-parse", "--path-format=absolute", "--git-common-dir")
	cmd.Dir = cwd
	out, err := cmd.CombinedOutput()
	if err != nil {
		return "", fmt.Errorf("default Skein world requires cwd inside a supported non-bare Git worktree; run `git init` or pass --config-dir: %w", err)
	}
	commonDir := filepath.Clean(strings.TrimSpace(string(out)))
	if !filepath.IsAbs(commonDir) {
		return "", fmt.Errorf("Git returned non-absolute common dir for default Skein world: %s", commonDir)
	}
	if filepath.Base(commonDir) != ".git" {
		return "", fmt.Errorf("unsupported Git layout for default Skein world: common Git dir must be a repository .git directory, got %s", commonDir)
	}
	return filepath.Dir(commonDir), nil
}

func writeMissing(path, content string) error {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return os.WriteFile(path, []byte(content), 0o644)
	} else {
		return err
	}
}
