package client

import (
	"bufio"
	"encoding/json"
	"net"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func writeMeta(t *testing.T, db, sock string, pid int) {
	t.Helper()
	canon, err := canonicalPath(db)
	if err != nil {
		t.Fatal(err)
	}
	dir := filepath.Join(os.TempDir(), "todo-daemon")
	if err := os.MkdirAll(dir, 0755); err != nil {
		t.Fatal(err)
	}
	m := Metadata{ProtocolVersion: 1, PID: pid, DatabasePath: canon, DaemonID: "daemon-1", SocketPath: sock}
	b, _ := json.Marshal(m)
	if err := os.WriteFile(filepath.Join(dir, stableHash(canon)+".json"), b, 0644); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { os.Remove(filepath.Join(dir, stableHash(canon)+".json")) })
}

func serve(t *testing.T, handler func(map[string]any) map[string]any) string {
	t.Helper()
	dir, err := os.MkdirTemp(os.TempDir(), "td-")
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { os.RemoveAll(dir) })
	sock := filepath.Join(dir, "s.sock")
	ln, err := net.Listen("unix", sock)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { ln.Close() })
	go func() {
		c, err := ln.Accept()
		if err != nil {
			return
		}
		defer c.Close()
		var req map[string]any
		_ = json.NewDecoder(bufio.NewReader(c)).Decode(&req)
		_ = json.NewEncoder(c).Encode(handler(req))
	}()
	return sock
}

func TestCallSuccessAndDaemonError(t *testing.T) {
	db := filepath.Join(t.TempDir(), "todo.sqlite")
	canon, _ := canonicalPath(db)
	sock := serve(t, func(req map[string]any) map[string]any {
		if req["protocol_version"] != float64(1) || req["operation"] != "show" || req["daemon_id"] != "daemon-1" || req["database_path"] != canon {
			t.Fatalf("bad envelope: %#v", req)
		}
		if got := req["arguments"].(map[string]any)["id"]; got != "t1" {
			t.Fatalf("bad args: %#v", req["arguments"])
		}
		if got := req["options"].(map[string]any)["format"]; got != "json" {
			t.Fatalf("bad options: %#v", req["options"])
		}
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": true, "result": map[string]any{"id": "t1"}, "error": nil}
	})
	writeMeta(t, db, sock, os.Getpid())
	got, err := New(Config{DB: db, Format: "json"}).Call("show", map[string]any{"id": "t1"})
	if err != nil {
		t.Fatal(err)
	}
	if got.(map[string]any)["id"] != "t1" {
		t.Fatalf("got %#v", got)
	}

	sock = serve(t, func(req map[string]any) map[string]any {
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": false, "result": nil, "error": map[string]any{"type": "domain", "code": "task/not-found", "message": "Task not found", "details": map[string]any{}}}
	})
	writeMeta(t, db, sock, os.Getpid())
	_, err = New(Config{DB: db, Format: "json"}).Call("show", map[string]any{"id": "missing"})
	if err == nil || !strings.Contains(err.Error(), "task/not-found") {
		t.Fatalf("expected daemon error, got %v", err)
	}

	sock = serve(t, func(req map[string]any) map[string]any {
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": false, "result": nil, "error": map[string]any{"type": "domain", "code": "domain/error", "message": "Query not found", "details": map[string]any{"canonical-query": "missing", "available": []any{"mine", "ready"}}}}
	})
	writeMeta(t, db, sock, os.Getpid())
	_, err = New(Config{DB: db, Format: "json"}).Call("list-query", map[string]any{"query": "missing", "params": map[string]string{}})
	if err == nil || !strings.Contains(err.Error(), "missing") || !strings.Contains(err.Error(), "mine, ready") {
		t.Fatalf("expected query details, got %v", err)
	}

	sock = serve(t, func(req map[string]any) map[string]any {
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": false, "result": nil, "error": map[string]any{"type": "domain", "code": "database/not-initialized", "message": "Database is not initialized; run `todo init` first", "details": map[string]any{}}}
	})
	writeMeta(t, db, sock, os.Getpid())
	_, err = New(Config{DB: db, Format: "json"}).Call("list", map[string]any{})
	if err == nil || err.Error() != "Database is not initialized; run `todo init` first" {
		t.Fatalf("expected init guidance, got %v", err)
	}
}

func TestMetadataAndTransportFailures(t *testing.T) {
	db := filepath.Join(t.TempDir(), "missing.sqlite")
	_, err := New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "no running daemon") || !strings.Contains(err.Error(), "daemon start") {
		t.Fatalf("expected missing daemon startup guidance, got %v", err)
	}

	writeMeta(t, db, filepath.Join(t.TempDir(), "no.sock"), os.Getpid())
	_, err = New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "daemon socket unreachable") {
		t.Fatalf("expected unreachable socket, got %v", err)
	}

	writeMeta(t, db, filepath.Join(t.TempDir(), "no.sock"), 999999)
	_, err = New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "stale daemon metadata") {
		t.Fatalf("expected stale metadata, got %v", err)
	}
}

func TestMalformedMetadataAndIdentityMismatch(t *testing.T) {
	db := filepath.Join(t.TempDir(), "todo.sqlite")
	canon, err := canonicalPath(db)
	if err != nil {
		t.Fatal(err)
	}
	dir := filepath.Join(os.TempDir(), "todo-daemon")
	if err := os.MkdirAll(dir, 0755); err != nil {
		t.Fatal(err)
	}
	metaFile := filepath.Join(dir, stableHash(canon)+".json")
	t.Cleanup(func() { os.Remove(metaFile) })
	if err := os.WriteFile(metaFile, []byte(`{"protocol_version":1}`), 0644); err != nil {
		t.Fatal(err)
	}
	_, err = New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "malformed daemon metadata") {
		t.Fatalf("expected malformed metadata, got %v", err)
	}

	other := filepath.Join(t.TempDir(), "other.sqlite")
	otherCanon, _ := canonicalPath(other)
	m := Metadata{ProtocolVersion: 1, PID: os.Getpid(), DatabasePath: otherCanon, DaemonID: "daemon-1", SocketPath: filepath.Join(t.TempDir(), "no.sock")}
	b, _ := json.Marshal(m)
	if err := os.WriteFile(metaFile, b, 0644); err != nil {
		t.Fatal(err)
	}
	_, err = New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "database mismatch") {
		t.Fatalf("expected metadata database mismatch, got %v", err)
	}

	sock := serve(t, func(req map[string]any) map[string]any {
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": false, "result": nil, "error": map[string]any{"type": "protocol", "code": "protocol/identity-mismatch", "message": "Daemon identity mismatch", "details": map[string]any{}}}
	})
	writeMeta(t, db, sock, os.Getpid())
	_, err = New(Config{DB: db, Format: "json"}).Call("status", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "protocol/identity-mismatch") {
		t.Fatalf("expected identity mismatch, got %v", err)
	}
}

func TestMalformedLifecycleResults(t *testing.T) {
	db := filepath.Join(t.TempDir(), "todo.sqlite")
	sock := serve(t, func(req map[string]any) map[string]any {
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": true, "result": map[string]any{"healthy": true}, "error": nil}
	})
	writeMeta(t, db, sock, os.Getpid())
	_, err := New(Config{DB: db, Format: "json"}).Call("status", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "invalid status result") {
		t.Fatalf("expected malformed status result, got %v", err)
	}
}

func TestMalformedResponse(t *testing.T) {
	db := filepath.Join(t.TempDir(), "todo.sqlite")
	sock := filepath.Join(t.TempDir(), "s.sock")
	ln, err := net.Listen("unix", sock)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { ln.Close() })
	go func() {
		c, err := ln.Accept()
		if err != nil {
			return
		}
		defer c.Close()
		_, _ = c.Write([]byte("not-json\n"))
	}()
	writeMeta(t, db, sock, os.Getpid())
	_, err = New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "malformed daemon response") {
		t.Fatalf("expected malformed response, got %v", err)
	}
}

func TestMalformedErrorEnvelope(t *testing.T) {
	db := filepath.Join(t.TempDir(), "todo.sqlite")
	sock := serve(t, func(req map[string]any) map[string]any {
		return map[string]any{"protocol_version": 1, "request_id": req["request_id"], "ok": false, "result": nil, "error": map[string]any{"type": "domain"}}
	})
	writeMeta(t, db, sock, os.Getpid())
	_, err := New(Config{DB: db, Format: "json"}).Call("init", map[string]any{})
	if err == nil || !strings.Contains(err.Error(), "malformed daemon response") {
		t.Fatalf("expected malformed envelope, got %v", err)
	}
}
