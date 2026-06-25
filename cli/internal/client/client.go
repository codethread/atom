package client

import (
	"bufio"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strings"
	"syscall"
	"time"
)

const protocolVersion = 1

type Config struct {
	DB     string
	Format string
}

type Metadata struct {
	ProtocolVersion int    `json:"protocol_version"`
	PID             int    `json:"pid"`
	DatabasePath    string `json:"database_path"`
	DaemonID        string `json:"daemon_id"`
	SocketPath      string `json:"socket_path"`
}

type SocketClient struct {
	Config          Config
	DialTimeout     time.Duration
	RequestDeadline time.Duration
}

type ResponseError struct {
	Type    string         `json:"type"`
	Code    string         `json:"code"`
	Message string         `json:"message"`
	Details map[string]any `json:"details"`
}

type response struct {
	ProtocolVersion int            `json:"protocol_version"`
	RequestID       string         `json:"request_id"`
	OK              bool           `json:"ok"`
	Result          any            `json:"result"`
	Error           *ResponseError `json:"error"`
}

func New(cfg Config) *SocketClient {
	return &SocketClient{Config: cfg, DialTimeout: time.Second, RequestDeadline: 10 * time.Second}
}

func (e *ResponseError) Error() string {
	if e == nil {
		return "daemon error"
	}
	message := e.Message
	if query, ok := e.Details["canonical-query"].(string); ok && query != "" {
		message = fmt.Sprintf("%s: %s", message, query)
	}
	if available, ok := e.Details["available"].([]any); ok && len(available) > 0 {
		names := make([]string, 0, len(available))
		for _, v := range available {
			if s, ok := v.(string); ok {
				names = append(names, s)
			}
		}
		if len(names) > 0 {
			message = fmt.Sprintf("%s (available: %s)", message, strings.Join(names, ", "))
		}
	}
	if e.Code == "database/not-initialized" {
		return message
	}
	if e.Code != "" {
		return fmt.Sprintf("daemon %s error (%s): %s", e.Type, e.Code, message)
	}
	return fmt.Sprintf("daemon %s error: %s", e.Type, message)
}

func validResponseError(e *ResponseError) bool {
	if e.Code == "" || e.Message == "" || e.Details == nil {
		return false
	}
	switch e.Type {
	case "domain", "protocol", "transport":
		return true
	default:
		return false
	}
}

func (c *SocketClient) Call(operation string, arguments map[string]any) (any, error) {
	meta, canonical, metadataFile, err := c.metadata()
	if err != nil {
		return nil, err
	}
	requestID := fmt.Sprintf("%d", time.Now().UnixNano())
	req := map[string]any{
		"protocol_version": protocolVersion,
		"request_id":       requestID,
		"daemon_id":        meta.DaemonID,
		"database_path":    canonical,
		"operation":        operation,
		"arguments":        arguments,
		"options":          map[string]any{"format": c.Config.Format},
	}
	ctx, cancel := context.WithTimeout(context.Background(), c.DialTimeout)
	defer cancel()
	dialer := net.Dialer{}
	conn, err := dialer.DialContext(ctx, "unix", meta.SocketPath)
	if err != nil {
		return nil, fmt.Errorf("daemon socket unreachable: %w", err)
	}
	defer conn.Close()
	deadline := time.Now().Add(c.RequestDeadline)
	_ = conn.SetDeadline(deadline)
	if err := json.NewEncoder(conn).Encode(req); err != nil {
		return nil, fmt.Errorf("daemon socket write failed: %w", err)
	}
	var resp response
	if err := json.NewDecoder(bufio.NewReader(conn)).Decode(&resp); err != nil {
		return nil, fmt.Errorf("malformed daemon response: %w", err)
	}
	if resp.ProtocolVersion != protocolVersion || resp.RequestID != requestID {
		return nil, errors.New("malformed daemon response: protocol version or request id mismatch")
	}
	if !resp.OK {
		if resp.Error == nil || !validResponseError(resp.Error) || resp.Result != nil {
			return nil, errors.New("malformed daemon response: error envelope does not match protocol")
		}
		return nil, resp.Error
	}
	if resp.Error != nil {
		return nil, errors.New("malformed daemon response: success envelope includes error")
	}
	if err := validateLifecycleResult(operation, resp.Result, meta, canonical); err != nil {
		return nil, err
	}
	if operation == "stop" {
		if err := waitForCleanup(metadataFile, meta.SocketPath); err != nil {
			return nil, err
		}
	}
	return resp.Result, nil
}

func (c *SocketClient) metadata() (Metadata, string, string, error) {
	canonical, err := canonicalPath(c.Config.DB)
	if err != nil {
		return Metadata{}, "", "", err
	}
	file := filepath.Join(os.TempDir(), "todo-daemon", stableHash(canonical)+".json")
	b, err := os.ReadFile(file)
	if os.IsNotExist(err) {
		return Metadata{}, "", "", fmt.Errorf("no running daemon for %s (start one with: todo daemon start, using the same --config-path if set)", canonical)
	}
	if err != nil {
		return Metadata{}, "", "", err
	}
	var m Metadata
	if err := json.Unmarshal(b, &m); err != nil {
		return Metadata{}, "", "", fmt.Errorf("malformed daemon metadata: %w", err)
	}
	if m.ProtocolVersion != protocolVersion || m.PID == 0 || m.DatabasePath == "" || m.DaemonID == "" || m.SocketPath == "" {
		return Metadata{}, "", "", errors.New("malformed daemon metadata: missing required fields")
	}
	if m.DatabasePath != canonical {
		return Metadata{}, "", "", fmt.Errorf("daemon metadata database mismatch: %s", m.DatabasePath)
	}
	if !pidAlive(m.PID) {
		return Metadata{}, "", "", fmt.Errorf("stale daemon metadata: pid %d is not alive", m.PID)
	}
	return m, canonical, file, nil
}

func validateLifecycleResult(operation string, result any, meta Metadata, canonical string) error {
	switch operation {
	case "status":
		m, ok := result.(map[string]any)
		if !ok || m["healthy"] != true || !samePositivePID(m["pid"], meta.PID) || m["database_path"] != canonical || m["daemon_id"] != meta.DaemonID || m["socket_path"] != meta.SocketPath {
			return errors.New("malformed daemon response: invalid status result")
		}
	case "stop":
		m, ok := result.(map[string]any)
		if !ok || m["stopping"] != true || !samePositivePID(m["pid"], meta.PID) || m["database_path"] != canonical || m["daemon_id"] != meta.DaemonID {
			return errors.New("malformed daemon response: invalid stop result")
		}
	}
	return nil
}

func samePositivePID(v any, expected int) bool {
	switch n := v.(type) {
	case float64:
		return n > 0 && int(n) == expected
	case int:
		return n > 0 && n == expected
	default:
		return false
	}
}

func waitForCleanup(metadataFile, socketPath string) error {
	ednFile := strings.TrimSuffix(metadataFile, filepath.Ext(metadataFile)) + ".edn"
	deadline := time.Now().Add(10 * time.Second)
	for time.Now().Before(deadline) {
		metadataGone := false
		if _, err := os.Stat(metadataFile); os.IsNotExist(err) {
			metadataGone = true
		} else if err != nil {
			return err
		}
		socketGone := false
		if _, err := os.Stat(socketPath); os.IsNotExist(err) {
			socketGone = true
		} else if err != nil {
			return err
		}
		ednGone := false
		if _, err := os.Stat(ednFile); os.IsNotExist(err) {
			ednGone = true
		} else if err != nil {
			return err
		}
		if metadataGone && ednGone && socketGone {
			return nil
		}
		time.Sleep(100 * time.Millisecond)
	}
	return errors.New("daemon stop did not clean up runtime metadata/socket")
}

func canonicalPath(p string) (string, error) {
	if p == "" {
		return "", errors.New("db path is required")
	}
	abs, err := filepath.Abs(p)
	if err != nil {
		return "", err
	}
	if real, err := filepath.EvalSymlinks(abs); err == nil {
		return filepath.Clean(real), nil
	}
	parent := filepath.Dir(abs)
	realParent, err := filepath.EvalSymlinks(parent)
	if err != nil {
		return filepath.Clean(abs), nil
	}
	return filepath.Clean(filepath.Join(realParent, filepath.Base(abs))), nil
}

func stableHash(s string) string { h := sha256.Sum256([]byte(s)); return hex.EncodeToString(h[:]) }

func pidAlive(pid int) bool {
	p, err := os.FindProcess(pid)
	if err != nil {
		return false
	}
	return p.Signal(syscall.Signal(0)) == nil
}
