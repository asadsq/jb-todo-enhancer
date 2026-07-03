# TODO Enhancer

![Build](https://github.com/asadsq/jb-todo-enhancer/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
**TODO Enhancer** supercharges the way you track `TODO` and `FIXME` comments in your project.

On top of IntelliJ's built-in TODO index, it adds a dedicated **tool window** that understands
structured metadata written directly in your comments:

| Metadata  | Syntax                        | Example                                     |
|-----------|-------------------------------|---------------------------------------------|
| Assignee  | `@name` or `TODO(name)`       | `// TODO(@asad) fix the parser`             |
| Priority  | `!p1`..`!p3`, `!high`/`!low`  | `// TODO !p1 ship this first`               |
| Tags      | `#tag`                        | `// TODO #backend #perf tighten the query`  |
| Due date  | `due:YYYY-MM-DD`              | `// TODO due:2026-07-10 finalize notes`     |

Put it together and a single comment carries everything:

```kotlin
// TODO(@asad) !p1 #backend due:2026-07-10 - refactor the query builder
```

From the tool window you can **sort and filter** every TODO by type, priority, assignee, tag, or
free text, filter to just your own with **Only mine**, and **double-click (or press Enter)** to jump
straight to the code. Plain `// TODO` comments keep working exactly as before — all metadata is
optional.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "TODO Enhancer"</kbd> >
  <kbd>Install</kbd>

- Manually:

  Download the [latest release](https://github.com/asadsq/jb-todo-enhancer/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

1. Open the **TODO Enhancer** tool window (bottom tool window stripe).
2. It lists every TODO/FIXME in the project. Click a column header to sort; use the filters to narrow
   by text, type, priority, or assignee.
3. Double-click a row (or select it and press <kbd>Enter</kbd>) to navigate to the comment.

## Development

```sh
./gradlew test          # run unit + platform tests
./gradlew runIde        # launch a sandbox IDE with the plugin installed
./gradlew buildPlugin   # produce build/distributions/*.zip
./gradlew verifyPlugin  # run the JetBrains Plugin Verifier
```

## Publishing

See [PUBLISHING.md](./PUBLISHING.md) for the full end-to-end guide to releasing on the JetBrains Marketplace.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
