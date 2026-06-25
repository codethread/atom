package main

import (
	"fmt"
	"os"

	"atom-todo-cli/internal/command"
)

func main() {
	if err := command.New(os.Stdout, os.Stderr).Run(os.Args[1:]); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		os.Exit(1)
	}
}
