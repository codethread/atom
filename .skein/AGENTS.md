Always read <source-dir>/docs/skein.md where source-dir = !`cat .skein/config.json | jq '.source'` first.

Always smoke test changes after completion in a separate `--config-dir`
Once smoke passes, `reload!` my current weaver over `repl`
