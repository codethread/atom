package cli_test

import (
	"bytes"
	"encoding/json"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"
)

func TestGoDaemonLifecycleCommands(t *testing.T) {
	dir := t.TempDir()
	db := filepath.Join(dir, "todo.sqlite")
	trusted := filepath.Join(dir, "trusted.clj")
	loaded := filepath.Join(dir, "loaded.txt")
	if err := os.WriteFile(trusted, []byte(`(spit `+strconv.Quote(loaded)+` "ok")`), 0644); err != nil {
		t.Fatal(err)
	}
	cfg := filepath.Join(dir, "daemon.edn")
	if err := os.WriteFile(cfg, []byte(`{:load-files ["trusted.clj"]}`), 0644); err != nil {
		t.Fatal(err)
	}

	daemon := exec.Command("go", "run", "./cmd/todo", "--db", db, "daemon", "start", "--config", cfg)
	var daemonOut bytes.Buffer
	daemon.Stdout = &daemonOut
	daemon.Stderr = &daemonOut
	if err := daemon.Start(); err != nil {
		t.Fatalf("start daemon: %v", err)
	}
	t.Cleanup(func() {
		_ = daemon.Process.Kill()
		_, _ = daemon.Process.Wait()
	})
	waitForStatus(t, db, &daemonOut)
	if _, err := os.Stat(loaded); err != nil {
		t.Fatalf("trusted config did not load: %v", err)
	}
	out, err := outputTodo(db, "--format", "json", "daemon", "status")
	if err != nil {
		t.Fatal(err)
	}
	var status map[string]any
	if err := json.Unmarshal([]byte(out), &status); err != nil {
		t.Fatalf("status is not json: %v\n%s", err, out)
	}
	if status["healthy"] != true || status["database_path"] == "" || status["daemon_id"] == "" || status["socket_path"] == "" || status["pid"].(float64) <= 0 {
		t.Fatalf("unexpected status payload: %#v", status)
	}
	if nrepl, ok := status["nrepl"].(map[string]any); !ok || nrepl["host"] == "" || nrepl["port"].(float64) <= 0 {
		t.Fatalf("unexpected nrepl payload: %#v", status["nrepl"])
	}
	if err := runTodo(db, "daemon", "stop"); err != nil {
		t.Fatal(err)
	}
	if err := daemon.Wait(); err != nil {
		t.Fatalf("daemon did not exit cleanly: %v\n%s", err, daemonOut.String())
	}
	if _, err := outputTodo(db, "daemon", "status"); err == nil {
		t.Fatal("expected status to fail after stop cleanup")
	}
}

func TestGoDaemonStartConfigFailurePublishesNoMetadata(t *testing.T) {
	dir := t.TempDir()
	db := filepath.Join(dir, "todo.sqlite")
	cfg := filepath.Join(dir, "bad.edn")
	if err := os.WriteFile(cfg, []byte(`{:load-files ["missing.clj"]}`), 0644); err != nil {
		t.Fatal(err)
	}
	out, err := outputTodo(db, "daemon", "start", "--config", cfg)
	if err == nil {
		t.Fatalf("expected startup config failure, got success: %s", out)
	}
	if _, err := outputTodo(db, "daemon", "status"); err == nil {
		t.Fatal("expected no metadata after failed startup")
	}
}

func TestGoListNamedQueryAgainstRealDaemon(t *testing.T) {
	dir := t.TempDir()
	db := filepath.Join(dir, "todo.sqlite")
	trusted := filepath.Join(dir, "trusted.clj")
	if err := os.WriteFile(trusted, []byte(`(require '[todo.daemon.runtime :as runtime]
         '[todo.daemon.api :as api])
(api/register-query @runtime/current-runtime 'by-owner '{:params [:owner] :where [:= [:attr :owner] [:param :owner]]})
`), 0644); err != nil {
		t.Fatal(err)
	}
	cfg := filepath.Join(dir, "daemon.edn")
	if err := os.WriteFile(cfg, []byte(`{:load-files ["trusted.clj"]}`), 0644); err != nil {
		t.Fatal(err)
	}

	daemon := exec.Command("clojure", "-M:todo", "--db", db, "daemon", "start", "--config", cfg)
	daemon.Dir = ".."
	var daemonErr bytes.Buffer
	daemon.Stdout = &daemonErr
	daemon.Stderr = &daemonErr
	if err := daemon.Start(); err != nil {
		t.Fatalf("start daemon: %v", err)
	}
	t.Cleanup(func() {
		stop := exec.Command("clojure", "-M:todo", "--db", db, "daemon", "stop")
		stop.Dir = ".."
		_ = stop.Run()
		_ = daemon.Process.Kill()
		_, _ = daemon.Process.Wait()
	})
	waitForDaemonAndInit(t, db, &daemonErr)

	if err := runTodo(db, "add", "Agent task", "--attr", "owner=agent"); err != nil {
		t.Fatal(err)
	}
	if err := runTodo(db, "add", "Human task", "--attr", "owner=human"); err != nil {
		t.Fatal(err)
	}
	out, err := outputTodo(db, "--format", "json", "list", "--query", ":by-owner", "--param", "owner=agent")
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(out, "Agent task") || strings.Contains(out, "Human task") {
		t.Fatalf("unexpected query output: %s", out)
	}
	out, err = outputTodo(db, "--format", "json", "ready", "--query", "by-owner", "--param", "owner=agent")
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(out, "Agent task") || strings.Contains(out, "Human task") {
		t.Fatalf("unexpected ready query output: %s", out)
	}
}

func waitForStatus(t *testing.T, db string, daemonErr *bytes.Buffer) {
	t.Helper()
	deadline := time.Now().Add(20 * time.Second)
	var lastErr error
	for time.Now().Before(deadline) {
		if _, err := outputTodo(db, "daemon", "status"); err == nil {
			return
		} else {
			lastErr = err
		}
		time.Sleep(250 * time.Millisecond)
	}
	t.Fatalf("daemon did not become ready: %v\n%s", lastErr, daemonErr.String())
}

func waitForDaemonAndInit(t *testing.T, db string, daemonErr *bytes.Buffer) {
	t.Helper()
	deadline := time.Now().Add(20 * time.Second)
	var lastErr error
	for time.Now().Before(deadline) {
		if err := runTodo(db, "init"); err == nil {
			return
		} else {
			lastErr = err
		}
		time.Sleep(250 * time.Millisecond)
	}
	t.Fatalf("daemon did not become ready: %v\n%s", lastErr, daemonErr.String())
}

func runTodo(db string, args ...string) error {
	_, err := outputTodo(db, args...)
	return err
}

func outputTodo(db string, args ...string) (string, error) {
	full := append([]string{"run", "./cmd/todo", "--db", db}, args...)
	cmd := exec.Command("go", full...)
	var out bytes.Buffer
	cmd.Stdout = &out
	cmd.Stderr = &out
	if err := cmd.Run(); err != nil {
		return out.String(), err
	}
	return out.String(), nil
}
