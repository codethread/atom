package main

import (
	"fmt"
	"os"

	"skein-strand-cli/internal/command"
)

func main() {
	if err := command.New(os.Stdout, os.Stderr).Run(os.Args[1:]); err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		if exit, ok := err.(interface{ ExitCode() int }); ok {
			os.Exit(exit.ExitCode())
		}
		os.Exit(1)
	}
}
