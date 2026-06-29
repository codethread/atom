package client

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"skein-strand-cli/internal/config"
)

func TestReadMillMetadataRejectsMissingMalformedAndMismatched(t *testing.T) {
	xdg := filepath.Join(t.TempDir(), "state")
	t.Setenv("XDG_STATE_HOME", xdg)
	if _, err := ReadMillMetadata(); err == nil || !strings.Contains(err.Error(), "no running mill") {
		t.Fatalf("expected missing metadata error, got %v", err)
	}
	root, err := config.StateRoot()
	if err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(root, 0o755); err != nil {
		t.Fatal(err)
	}
	metadataPath := filepath.Join(root, config.MillMetadataFileName)
	if err := os.WriteFile(metadataPath, []byte("{"), 0o644); err != nil {
		t.Fatal(err)
	}
	if _, err := ReadMillMetadata(); err == nil || !strings.Contains(err.Error(), "malformed mill metadata") {
		t.Fatalf("expected malformed metadata error, got %v", err)
	}

	writeMillMetadata(t, metadataPath, MillMetadata{ProtocolVersion: MillProtocolVersion, PID: os.Getpid(), MillID: "mill-test", StateRoot: filepath.Join(root, "other"), SocketPath: filepath.Join(root, config.MillSocketFileName), StartedAt: time.Now().UTC().Format(time.RFC3339Nano)})
	if _, err := ReadMillMetadata(); err == nil || !strings.Contains(err.Error(), "state root mismatch") {
		t.Fatalf("expected state root mismatch, got %v", err)
	}

	writeMillMetadata(t, metadataPath, MillMetadata{ProtocolVersion: MillProtocolVersion, PID: os.Getpid(), MillID: "mill-test", StateRoot: root, SocketPath: filepath.Join(root, "other.sock"), StartedAt: time.Now().UTC().Format(time.RFC3339Nano)})
	if _, err := ReadMillMetadata(); err == nil || !strings.Contains(err.Error(), "socket mismatch") {
		t.Fatalf("expected socket mismatch, got %v", err)
	}

	writeMillMetadata(t, metadataPath, MillMetadata{ProtocolVersion: MillProtocolVersion, PID: 0, MillID: "mill-test", StateRoot: root, SocketPath: filepath.Join(root, config.MillSocketFileName), StartedAt: time.Now().UTC().Format(time.RFC3339Nano)})
	if _, err := ReadMillMetadata(); err == nil || !strings.Contains(err.Error(), "missing required fields") {
		t.Fatalf("expected missing required fields, got %v", err)
	}

	writeMillMetadata(t, metadataPath, MillMetadata{ProtocolVersion: MillProtocolVersion, PID: -1, MillID: "mill-test", StateRoot: root, SocketPath: filepath.Join(root, config.MillSocketFileName), StartedAt: time.Now().UTC().Format(time.RFC3339Nano)})
	if _, err := ReadMillMetadata(); err == nil || !strings.Contains(err.Error(), "stale mill metadata") {
		t.Fatalf("expected stale metadata, got %v", err)
	}
}

func writeMillMetadata(t *testing.T, path string, metadata MillMetadata) {
	t.Helper()
	b, err := json.Marshal(metadata)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, b, 0o644); err != nil {
		t.Fatal(err)
	}
}
