package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"os"
	"os/signal"
	"path/filepath"
	"time"

	"github.com/spf13/cobra"
	"skein-strand-cli/internal/client"
	"skein-strand-cli/internal/config"
)

type server struct{ meta client.MillMetadata }

func main() {
	root := &cobra.Command{Use: "mill", Short: "Skein local router"}
	root.AddCommand(&cobra.Command{Use: "start", Short: "Start mill in the foreground", RunE: func(cmd *cobra.Command, args []string) error {
		return start()
	}})
	root.AddCommand(&cobra.Command{Use: "status", Short: "Check the active mill", RunE: func(cmd *cobra.Command, args []string) error {
		result, err := client.MillStatus()
		if err != nil {
			return err
		}
		return json.NewEncoder(os.Stdout).Encode(result)
	}})
	if err := root.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}

func start() error {
	root, err := config.StateRoot()
	if err != nil {
		return err
	}
	if err := os.MkdirAll(root, 0o755); err != nil {
		return err
	}
	socketPath := filepath.Join(root, config.MillSocketFileName)
	metadataPath := filepath.Join(root, config.MillMetadataFileName)
	listener, err := net.Listen("unix", socketPath)
	if err != nil {
		return err
	}
	defer listener.Close()
	defer os.Remove(socketPath)
	defer os.Remove(metadataPath)

	meta := client.MillMetadata{ProtocolVersion: client.MillProtocolVersion, PID: os.Getpid(), MillID: fmt.Sprintf("mill-%d-%d", os.Getpid(), time.Now().UnixNano()), StateRoot: root, SocketPath: socketPath, StartedAt: time.Now().UTC().Format(time.RFC3339Nano)}
	b, err := json.MarshalIndent(meta, "", "  ")
	if err != nil {
		return err
	}
	if err := os.WriteFile(metadataPath, append(b, '\n'), 0o644); err != nil {
		return err
	}

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt)
	go func() { <-sig; listener.Close() }()
	s := server{meta: meta}
	for {
		conn, err := listener.Accept()
		if err != nil {
			if errors.Is(err, net.ErrClosed) {
				return nil
			}
			return err
		}
		go s.handle(conn)
	}
}

func (s server) handle(conn net.Conn) {
	defer conn.Close()
	var req client.MillRequest
	if err := json.NewDecoder(conn).Decode(&req); err != nil {
		_ = json.NewEncoder(conn).Encode(errorResponse("", "protocol", "mill/protocol", "malformed mill request", err.Error()))
		return
	}
	if req.ProtocolVersion != client.MillProtocolVersion || req.RequestID == "" || req.MillID != s.meta.MillID {
		_ = json.NewEncoder(conn).Encode(errorResponse(req.RequestID, "protocol", "mill/identity", "mill request identity mismatch", ""))
		return
	}
	switch req.Operation {
	case "status", "ping":
		_ = json.NewEncoder(conn).Encode(client.MillResponse{ProtocolVersion: client.MillProtocolVersion, RequestID: req.RequestID, OK: true, Result: map[string]any{"healthy": true, "protocol_version": client.MillProtocolVersion, "pid": s.meta.PID, "mill_id": s.meta.MillID, "state_root": s.meta.StateRoot, "socket_path": s.meta.SocketPath, "started_at": s.meta.StartedAt}})
	case "init":
		world, err := config.BootstrapWorld(req.World.CWD, req.World.ConfigDir, req.World.Source)
		if err != nil {
			_ = json.NewEncoder(conn).Encode(errorResponse(req.RequestID, "domain", "mill/init-failed", "strand init failed", err.Error()))
			return
		}
		_ = json.NewEncoder(conn).Encode(client.MillResponse{ProtocolVersion: client.MillProtocolVersion, RequestID: req.RequestID, OK: true, Result: map[string]any{"config_dir": world.ConfigDir, "config_file": world.ConfigFile}})
	default:
		_ = json.NewEncoder(conn).Encode(errorResponse(req.RequestID, "protocol", "mill/unknown-operation", "unknown mill operation", req.Operation))
	}
}

func errorResponse(requestID, typ, code, message, detail string) client.MillResponse {
	return client.MillResponse{ProtocolVersion: client.MillProtocolVersion, RequestID: requestID, OK: false, Error: &client.ResponseError{Type: typ, Code: code, Message: message, Details: map[string]any{"detail": detail}}}
}
