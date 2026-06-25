package client

import "fmt"

type Config struct {
	DB     string
	Format string
}

type SocketClient struct{ Config Config }

func New(cfg Config) *SocketClient { return &SocketClient{Config: cfg} }

func (c *SocketClient) Call(operation string, arguments map[string]any) (any, error) {
	return nil, fmt.Errorf("%s is not wired to the daemon socket yet", operation)
}
