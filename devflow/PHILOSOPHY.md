# Devflow Philosophy

The tool's runtime model is closer to Emacs than to a stateless command-line utility.

Start the daemon as the local application core. It owns the live database connection and runtime state for that daemon lifetime. Users who want customized runtime behavior should load trusted Clojure config at daemon startup, reload their own files while working, or experiment through the REPL.

The CLI is a convenience surface for common, scriptable operations. It should stay small, predictable, and suitable for lower-privilege workers that should use known structures without receiving broad REPL access. Do not grow the CLI into a parallel configuration or extension system when the daemon config and REPL already provide that role.

Design implications:

- Runtime customization belongs in trusted startup files and REPL workflows.
- CLI commands should cover common task operations and safe consumption of existing daemon state.
- Prefer daemon-owned in-memory runtime state over ad hoc per-client state when multiple clients need to share behavior during one daemon lifetime.
- Do not persist runtime behavior unless the feature explicitly calls for durable storage.
- Keep user-authored behavior data-first where possible, and fail loudly when a CLI worker references daemon state that has not been loaded.
