package main

import (
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"testing"
	"time"

	"skein-strand-cli/internal/client"
	"skein-strand-cli/internal/config"
)

func TestWeaverLifecycleWithFakeLauncher(t *testing.T) {
	t.Setenv("XDG_STATE_HOME", filepath.Join(t.TempDir(), "state"))
	source := tempSource(t)
	cfg := tempConfig(t, source)
	req := client.MillWorldRequest{CWD: t.TempDir(), ConfigDir: cfg}

	orig := launchWeaver
	var launches int
	launchWeaver = func(source string, args []string, out, errOut io.Writer) (*exec.Cmd, error) {
		launches++
		cmd := exec.Command("sleep", "60")
		if err := cmd.Start(); err != nil {
			return nil, err
		}
		world, err := config.RuntimeWorld(cfg)
		if err != nil {
			t.Fatal(err)
		}
		writeWeaverMetadata(t, world, cmd.Process.Pid, "weaver-one")
		return cmd, nil
	}
	t.Cleanup(func() { launchWeaver = orig })

	s := server{children: map[string]*weaverChild{}}
	status, err := s.startWeaver(req)
	if err != nil {
		t.Fatal(err)
	}
	if status["state"] != "running" || status["pid"] == nil || status["weaver_id"] != "weaver-one" || status["socket_path"] == nil || status["nrepl"] == nil {
		t.Fatalf("running status missing required fields: %#v", status)
	}
	if status["config_dir"] == "" || status["state_dir"] == "" || status["data_dir"] == "" || status["database_path"] == "" {
		t.Fatalf("running status missing identity/path fields: %#v", status)
	}
	status, err = s.startWeaver(req)
	if err != nil {
		t.Fatal(err)
	}
	if launches != 1 || status["weaver_id"] != "weaver-one" {
		t.Fatalf("second start should be idempotent after identity check launches=%d status=%#v", launches, status)
	}
	status, err = s.weaverStatus(req)
	if err != nil || status["state"] != "running" {
		t.Fatalf("bad status %#v err=%v", status, err)
	}
	status, err = s.stopWeaver(req)
	if err != nil || status["state"] != "stopped" {
		t.Fatalf("bad stop %#v err=%v", status, err)
	}
}

func TestDifferentReposHaveDistinctRuntimeDirsAndStopSelectedOnly(t *testing.T) {
	t.Setenv("XDG_STATE_HOME", filepath.Join(t.TempDir(), "state"))
	source := tempSource(t)
	cfgA := tempConfig(t, source)
	cfgB := tempConfig(t, source)
	orig := launchWeaver
	launchWeaver = func(source string, args []string, out, errOut io.Writer) (*exec.Cmd, error) {
		cmd := exec.Command("sleep", "60")
		if err := cmd.Start(); err != nil {
			return nil, err
		}
		return cmd, nil
	}
	t.Cleanup(func() { launchWeaver = orig })
	s := server{children: map[string]*weaverChild{}}
	stA, err := s.startWeaver(client.MillWorldRequest{CWD: t.TempDir(), ConfigDir: cfgA})
	if err != nil {
		t.Fatal(err)
	}
	stB, err := s.startWeaver(client.MillWorldRequest{CWD: t.TempDir(), ConfigDir: cfgB})
	if err != nil {
		t.Fatal(err)
	}
	if stA["state_dir"] == stB["state_dir"] || stA["data_dir"] == stB["data_dir"] {
		t.Fatalf("different config worlds share dirs: A=%#v B=%#v", stA, stB)
	}
	if _, err := s.stopWeaver(client.MillWorldRequest{CWD: t.TempDir(), ConfigDir: cfgA}); err != nil {
		t.Fatal(err)
	}
	worldB, err := config.RuntimeWorld(cfgB)
	if err != nil {
		t.Fatal(err)
	}
	if child := s.children[worldB.ConfigDir]; child == nil || !processAlive(child.cmd.Process.Pid) {
		t.Fatalf("stopping A should not stop B: %#v", s.children)
	}
	s.stopAll()
}

func tempSource(t *testing.T) string {
	t.Helper()
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "deps.edn"), []byte("{}"), 0o644); err != nil {
		t.Fatal(err)
	}
	return dir
}

func tempConfig(t *testing.T, source string) string {
	t.Helper()
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "config.json"), []byte(`{"configFormat":"alpha","source":"`+source+`"}`), 0o644); err != nil {
		t.Fatal(err)
	}
	real, err := filepath.EvalSymlinks(dir)
	if err != nil {
		t.Fatal(err)
	}
	return real
}

func writeWeaverMetadata(t *testing.T, world config.World, pid int, id string) {
	t.Helper()
	if err := os.MkdirAll(world.StateDir, 0o755); err != nil {
		t.Fatal(err)
	}
	meta := `{"protocol_version":1,"pid":` + intString(pid) + `,"database_path":"` + world.DBPath + `","weaver_id":"` + id + `","config_dir":"` + world.ConfigDir + `","data_dir":"` + world.DataDir + `","socket_path":"` + filepath.Join(world.StateDir, "weaver.sock") + `","started_at":"` + time.Now().UTC().Format(time.RFC3339Nano) + `","nrepl":{"host":"127.0.0.1","port":5555}}`
	if err := os.WriteFile(filepath.Join(world.StateDir, "weaver.json"), []byte(meta), 0o644); err != nil {
		t.Fatal(err)
	}
}

func intString(i int) string { return strconv.Itoa(i) }
