package client

import (
	"bufio"
	"context"
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
	ConfigDir string
	StateDir  string
	Format    string
}

type Metadata struct {
	ProtocolVersion int    `json:"protocol_version"`
	PID             int    `json:"pid"`
	DatabasePath    string `json:"database_path"`
	DaemonID        string `json:"daemon_id"`
	ConfigDir       string `json:"config_dir"`
	DataDir         string `json:"data_dir"`
	SocketPath      string `json:"socket_path"`
	StartedAt       string `json:"started_at"`
	NREPL           struct {
		Host string `json:"host"`
		Port int    `json:"port"`
	} `json:"nrepl"`
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
		names := []string{}
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
	meta, metadataFile, err := c.metadata()
	if err != nil {
		return nil, err
	}
	requestID := fmt.Sprintf("%d", time.Now().UnixNano())
	req := map[string]any{"protocol_version": protocolVersion, "request_id": requestID, "daemon_id": meta.DaemonID, "operation": operation, "arguments": arguments, "options": map[string]any{"format": c.Config.Format}}
	ctx, cancel := context.WithTimeout(context.Background(), c.DialTimeout)
	defer cancel()
	conn, err := (&net.Dialer{}).DialContext(ctx, "unix", meta.SocketPath)
	if err != nil {
		return nil, fmt.Errorf("daemon socket unreachable for state dir %s: %w", c.Config.StateDir, err)
	}
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(c.RequestDeadline))
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
	if err := validateLifecycleResult(operation, resp.Result, meta); err != nil {
		return nil, err
	}
	if operation == "stop" {
		if err := waitForCleanup(metadataFile, meta.SocketPath); err != nil {
			return nil, err
		}
	}
	return resp.Result, nil
}

func (c *SocketClient) metadata() (Metadata, string, error) {
	if c.Config.StateDir == "" {
		return Metadata{}, "", errors.New("state dir is required")
	}
	file := filepath.Join(c.Config.StateDir, "daemon.json")
	b, err := os.ReadFile(file)
	if os.IsNotExist(err) {
		return Metadata{}, "", fmt.Errorf("no running daemon for state dir %s (start one with: todo daemon start)", c.Config.StateDir)
	}
	if err != nil {
		return Metadata{}, "", err
	}
	var m Metadata
	if err := json.Unmarshal(b, &m); err != nil {
		return Metadata{}, "", fmt.Errorf("malformed daemon metadata: %w", err)
	}
	if m.ProtocolVersion != protocolVersion || m.PID == 0 || m.DatabasePath == "" || m.DaemonID == "" || m.ConfigDir == "" || m.DataDir == "" || m.SocketPath == "" || m.StartedAt == "" || m.NREPL.Host == "" || m.NREPL.Port == 0 {
		return Metadata{}, "", errors.New("malformed daemon metadata: missing required fields")
	}
	if c.Config.ConfigDir != "" && filepath.Clean(m.ConfigDir) != filepath.Clean(c.Config.ConfigDir) {
		return Metadata{}, "", fmt.Errorf("daemon metadata config dir mismatch: %s", m.ConfigDir)
	}
	if filepath.Clean(m.SocketPath) != filepath.Join(c.Config.StateDir, "daemon.sock") {
		return Metadata{}, "", fmt.Errorf("daemon metadata socket mismatch: %s", m.SocketPath)
	}
	if !pidAlive(m.PID) {
		return Metadata{}, "", fmt.Errorf("stale daemon metadata: pid %d is not alive", m.PID)
	}
	return m, file, nil
}

func validateLifecycleResult(operation string, result any, meta Metadata) error {
	switch operation {
	case "status":
		m, ok := result.(map[string]any)
		if !ok || m["healthy"] != true || m["protocol_version"] != float64(protocolVersion) || !samePositivePID(m["pid"], meta.PID) || m["database_path"] != meta.DatabasePath || m["daemon_id"] != meta.DaemonID || m["socket_path"] != meta.SocketPath || m["config_dir"] != meta.ConfigDir || m["data_dir"] != meta.DataDir || m["started_at"] != meta.StartedAt || !validNREPL(m["nrepl"]) {
			return errors.New("malformed daemon response: invalid status result")
		}
	case "stop":
		m, ok := result.(map[string]any)
		if !ok || m["stopping"] != true || !samePositivePID(m["pid"], meta.PID) || m["daemon_id"] != meta.DaemonID {
			return errors.New("malformed daemon response: invalid stop result")
		}
	}
	return nil
}

func validNREPL(v any) bool {
	m, ok := v.(map[string]any)
	if !ok {
		return false
	}
	host, ok := m["host"].(string)
	if !ok || host == "" {
		return false
	}
	port, ok := m["port"].(float64)
	return ok && port > 0
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
	ednFile := filepath.Join(filepath.Dir(metadataFile), "daemon.edn")
	deadline := time.Now().Add(10 * time.Second)
	for time.Now().Before(deadline) {
		metadataGone, err := missing(metadataFile)
		if err != nil {
			return err
		}
		socketGone, err := missing(socketPath)
		if err != nil {
			return err
		}
		ednGone, err := missing(ednFile)
		if err != nil {
			return err
		}
		if metadataGone && ednGone && socketGone {
			return nil
		}
		time.Sleep(100 * time.Millisecond)
	}
	return errors.New("daemon stop did not clean up runtime metadata/socket")
}
func missing(path string) (bool, error) {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return true, nil
	} else if err != nil {
		return false, err
	}
	return false, nil
}
func pidAlive(pid int) bool {
	p, err := os.FindProcess(pid)
	if err != nil {
		return false
	}
	return p.Signal(syscall.Signal(0)) == nil
}
