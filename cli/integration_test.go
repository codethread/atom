package cli_test

import (
	"bytes"
	"encoding/json"
	"os"
	"os/exec"
	"path/filepath"
	"testing"
	"time"
)

func TestGoDaemonLifecycleCommands(t *testing.T) {
	dir, err := os.MkdirTemp("/tmp", "td-")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { os.RemoveAll(dir) })
	writeClientConfig(t, dir)
	daemon := exec.Command("go", "run", "./cmd/todo", "--config-dir", dir, "daemon", "start")
	var daemonOut bytes.Buffer
	daemon.Stdout = &daemonOut
	daemon.Stderr = &daemonOut
	if err := daemon.Start(); err != nil {
		t.Fatalf("start daemon: %v", err)
	}
	t.Cleanup(func() { _ = daemon.Process.Kill(); _, _ = daemon.Process.Wait() })
	waitForStatus(t, dir, &daemonOut)
	out, err := outputTodo(dir, "--format", "json", "daemon", "status")
	if err != nil {
		t.Fatal(err)
	}
	var status map[string]any
	if err := json.Unmarshal([]byte(out), &status); err != nil {
		t.Fatalf("status is not json: %v\n%s", err, out)
	}
	realDir, err := filepath.EvalSymlinks(dir)
	if err != nil {
		t.Fatal(err)
	}
	if status["healthy"] != true || status["database_path"] == "" || status["config_dir"] != realDir || status["data_dir"] == "" || status["daemon_id"] == "" || status["socket_path"] != filepath.Join(realDir, "state", "daemon.sock") || status["pid"].(float64) <= 0 {
		t.Fatalf("unexpected status payload: %#v", status)
	}
	if err := runTodo(dir, "daemon", "stop"); err != nil {
		t.Fatal(err)
	}
	if err := daemon.Wait(); err != nil {
		t.Fatalf("daemon did not exit cleanly: %v\n%s", err, daemonOut.String())
	}
	if _, err := outputTodo(dir, "daemon", "status"); err == nil {
		t.Fatal("expected status to fail after stop cleanup")
	}
}

func writeClientConfig(t *testing.T, dir string) {
	t.Helper()
	source, err := filepath.Abs("..")
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(dir, "config.json"), []byte(`{"source":`+quote(source)+`,"format":"human"}`), 0644); err != nil {
		t.Fatal(err)
	}
}
func quote(s string) string { b, _ := json.Marshal(s); return string(b) }
func waitForStatus(t *testing.T, configDir string, daemonErr *bytes.Buffer) {
	t.Helper()
	deadline := time.Now().Add(20 * time.Second)
	var lastErr error
	for time.Now().Before(deadline) {
		if _, err := outputTodo(configDir, "daemon", "status"); err == nil {
			return
		} else {
			lastErr = err
		}
		time.Sleep(250 * time.Millisecond)
	}
	t.Fatalf("daemon did not become ready: %v\n%s", lastErr, daemonErr.String())
}
func waitForDaemonAndInit(t *testing.T, configDir string, daemonErr *bytes.Buffer) {
	t.Helper()
	deadline := time.Now().Add(20 * time.Second)
	var lastErr error
	for time.Now().Before(deadline) {
		if err := runTodo(configDir, "init"); err == nil {
			return
		} else {
			lastErr = err
		}
		time.Sleep(250 * time.Millisecond)
	}
	t.Fatalf("daemon did not become ready: %v\n%s", lastErr, daemonErr.String())
}
func runTodo(configDir string, args ...string) error {
	_, err := outputTodo(configDir, args...)
	return err
}
func outputTodo(configDir string, args ...string) (string, error) {
	full := append([]string{"run", "./cmd/todo", "--config-dir", configDir}, args...)
	cmd := exec.Command("go", full...)
	var out bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &out
	if err := cmd.Run(); err != nil {
		return out.String(), err
	}
	return out.String(), nil
}
