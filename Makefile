.PHONY: all build install bootstrap open-config

all: build install bootstrap

GO_CLI := ./cli/cmd/strand
BIN := ./cli/bin/strand
CONFIG_HOME ?= $(if $(XDG_CONFIG_HOME),$(XDG_CONFIG_HOME),$(HOME)/.config)
SKEIN_CONFIG ?= $(CONFIG_HOME)/skein
CONFIG_DIR := $(SKEIN_CONFIG)
CONFIG_FILE := $(CONFIG_DIR)/config.json
AGENTS_FILE := $(CONFIG_DIR)/AGENTS.md
CLAUDE_FILE := $(CONFIG_DIR)/CLAUDE.md

build:
	go build -o $(BIN) $(GO_CLI)

install:
	go install $(GO_CLI)

# Quick bootstrap matching README.md setup steps.
bootstrap:
	@if ! command -v jq >/dev/null 2>&1; then \
		echo "jq is required for bootstrap (required by README.md)" >&2; \
		exit 1; \
	fi
	go install $(GO_CLI)
	mkdir -p "$(CONFIG_DIR)"
	printf '{"configFormat":"alpha","source":"%s"}\n' "$(CURDIR)" | jq . > "$(CONFIG_FILE)"
	@if [ ! -e "$(AGENTS_FILE)" ] && [ ! -L "$(AGENTS_FILE)" ]; then \
		printf '%s\n' 'Always read <source-dir>/docs/skein.md where source-dir = !`cat config.json | jq '\''.source'\''` first.' > "$(AGENTS_FILE)"; \
	fi
	@if [ ! -e "$(CLAUDE_FILE)" ] && [ ! -L "$(CLAUDE_FILE)" ]; then \
		ln -s AGENTS.md "$(CLAUDE_FILE)"; \
	fi

open-config: bootstrap
	@if [ -z "$(EDITOR)" ]; then \
		echo "EDITOR is not set" >&2; \
		exit 1; \
	fi
	$(EDITOR) "$(CONFIG_FILE)"
