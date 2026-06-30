package main

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"syscall"

	"skein-strand-cli/internal/client"
	"skein-strand-cli/internal/config"
)

func resolveLifecycleWorld(req client.MillWorldRequest) (config.World, config.Config, error) {
	world, err := config.BootstrapTargetWorld(req.CWD, req.ConfigDir)
	if err != nil {
		return config.World{}, config.Config{}, err
	}
	cfg, loaded, err := config.Load(world.ConfigDir)
	if err != nil {
		return config.World{}, config.Config{}, err
	}
	return loaded, cfg, nil
}

func weaverArgs(world config.World) []string {
	return []string{"-M:skein", "-m", "skein.weaver.runtime", "--config-dir", world.ConfigDir, "--state-dir", world.StateDir, "--data-dir", world.DataDir}
}

func (s *server) startWeaver(req client.MillWorldRequest) (map[string]any, error) {
	world, cfg, err := resolveLifecycleWorld(req)
	if err != nil {
		return nil, err
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if child := s.children[world.ConfigDir]; child != nil && child.cmd.Process != nil && processAlive(child.cmd.Process.Pid) {
		status, _ := readStatus(world)
		if status == nil {
			status = baseStatus(world, "starting")
			status["pid"] = child.cmd.Process.Pid
		}
		return status, nil
	}
	source, err := config.ResolveSource(cfg.Source)
	if err != nil {
		return nil, err
	}
	if err := os.MkdirAll(world.StateDir, 0o755); err != nil {
		return nil, err
	}
	if err := os.MkdirAll(world.DataDir, 0o755); err != nil {
		return nil, err
	}
	cmd, err := launchWeaver(source, weaverArgs(world), os.Stdout, os.Stderr)
	if err != nil {
		return nil, err
	}
	s.children[world.ConfigDir] = &weaverChild{cmd: cmd, world: world}
	go func() { _ = cmd.Wait() }()
	status, _ := readStatus(world)
	if status == nil {
		status = baseStatus(world, "starting")
		status["pid"] = cmd.Process.Pid
	}
	return status, nil
}

func (s *server) weaverStatus(req client.MillWorldRequest) (map[string]any, error) {
	world, _, err := resolveLifecycleWorld(req)
	if err != nil {
		return nil, err
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if status, stale := readStatus(world); status != nil {
		if stale {
			status["state"] = "stale"
		}
		return status, nil
	}
	if child := s.children[world.ConfigDir]; child != nil {
		if child.cmd.Process != nil && processAlive(child.cmd.Process.Pid) {
			status := baseStatus(world, "starting")
			status["pid"] = child.cmd.Process.Pid
			return status, nil
		}
		status := baseStatus(world, "stopped")
		return status, nil
	}
	return baseStatus(world, "none"), nil
}

func (s *server) stopWeaver(req client.MillWorldRequest) (map[string]any, error) {
	world, _, err := resolveLifecycleWorld(req)
	if err != nil {
		return nil, err
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	child := s.children[world.ConfigDir]
	if child == nil || child.cmd.Process == nil || !processAlive(child.cmd.Process.Pid) {
		delete(s.children, world.ConfigDir)
		return baseStatus(world, "stopped"), nil
	}
	pid := child.cmd.Process.Pid
	_ = syscall.Kill(-pid, syscall.SIGTERM)
	_ = child.cmd.Process.Kill()
	delete(s.children, world.ConfigDir)
	status := baseStatus(world, "stopped")
	status["pid"] = pid
	return status, nil
}

func (s *server) stopAll() {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, child := range s.children {
		if child.cmd != nil && child.cmd.Process != nil && processAlive(child.cmd.Process.Pid) {
			pid := child.cmd.Process.Pid
			_ = syscall.Kill(-pid, syscall.SIGTERM)
			_ = child.cmd.Process.Kill()
		}
	}
}

func readStatus(world config.World) (map[string]any, bool) {
	metadataPath := filepath.Join(world.StateDir, "weaver.json")
	b, err := os.ReadFile(metadataPath)
	if err != nil {
		return nil, false
	}
	var m client.Metadata
	if err := json.Unmarshal(b, &m); err != nil {
		st := baseStatus(world, "stale")
		st["stale_reason"] = fmt.Sprintf("malformed weaver metadata: %v", err)
		return st, true
	}
	st := baseStatus(world, "running")
	st["pid"] = m.PID
	st["weaver_id"] = m.DaemonID
	st["socket_path"] = m.SocketPath
	st["nrepl"] = m.NREPL
	st["started_at"] = m.StartedAt
	st["database_path"] = m.DatabasePath
	if !processAlive(m.PID) {
		st["stale_reason"] = fmt.Sprintf("pid %d is not alive", m.PID)
		return st, true
	}
	return st, false
}

func baseStatus(world config.World, state string) map[string]any {
	return map[string]any{
		"state":         state,
		"config_dir":    world.ConfigDir,
		"state_dir":     world.StateDir,
		"data_dir":      world.DataDir,
		"database_path": world.DBPath,
	}
}

func processAlive(pid int) bool {
	if pid <= 0 {
		return false
	}
	p, err := os.FindProcess(pid)
	if err != nil {
		return false
	}
	return p.Signal(syscall.Signal(0)) == nil
}
