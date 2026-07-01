package main

import "skein-strand-cli/internal/client"

func (s *server) forwardToWeaver(req client.MillWorldRequest, operation string, payload map[string]any) (any, error) {
	world, err := resolveLifecycleWorld(req)
	if err != nil {
		return nil, err
	}
	status, stale := readStatus(world)
	if status == nil {
		return nil, &client.ResponseError{Type: "domain", Code: "mill/no-selected-weaver", Message: "no running weaver for selected workspace; start one with: strand weaver start", Details: map[string]any{"config_dir": world.ConfigDir}}
	}
	if stale {
		return nil, &client.ResponseError{Type: "transport", Code: "mill/stale-selected-weaver", Message: "stale selected workspace weaver metadata", Details: map[string]any{"config_dir": world.ConfigDir, "stale_reason": status["stale_reason"]}}
	}
	return client.New(client.Config{ConfigDir: world.ConfigDir, StateDir: world.StateDir}).Call(operation, payload)
}
