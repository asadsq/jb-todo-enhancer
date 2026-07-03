# Asad's Technical README

Quick-reference sheet: the two-sentence technical description of every code file in the
project, mirrored from the file headers. If a file changes significantly, update its
header comment and this list together.

## Main source (`src/main/kotlin`)

- **`TodoEnhancerBundle.kt`** — Provides access to localized UI strings via IntelliJ's
  DynamicBundle, backed by `messages/TodoEnhancerBundle.properties`. Exposes `get`,
  `message`, and `messagePointer` helpers so UI code can look up display text by
  property key.
- **`model/TodoEntry.kt`** — Immutable data class representing a single TODO found in the
  project, combining parsed metadata (type, assignee, priority, tags, due date) with its
  file location. Includes derived display properties (`location`, `tagsText`) consumed by
  the tool window table.
- **`model/TodoPriority.kt`** — Enum of TODO priority levels (P1, P2, P3, NONE) with a
  numeric rank used for sorting, where NONE sorts last. The `fromToken` factory maps
  user-written tokens such as `p1`, `high`, or `low` to the matching level.
- **`parser/TodoMetadataParser.kt`** — Regex-based parser that converts raw TODO comment
  text into a `ParsedTodo` holding the type, assignee, priority, tags, due date, and
  cleaned description. All metadata tokens are optional and order-independent, and
  malformed values are ignored gracefully.
- **`services/TodoScanService.kt`** — Project-level service that collects every TODO
  occurrence using IntelliJ's built-in TODO index (`PsiTodoSearchHelper`) and enriches
  each one via `TodoMetadataParser`. Produces `TodoEntry` objects with project-relative
  paths, 1-based line numbers, and character offsets; `scan()` must run inside a read
  action.
- **`toolWindow/TodoEnhancerPanel.kt`** — Swing UI for the tool window: a sortable
  `TableView` of `TodoEntry` rows with toolbar filters for free text, type, priority, and
  "only mine". Fetches data from `TodoScanService` on a background read action and
  auto-refreshes with a debounce whenever the PSI tree changes.
- **`toolWindow/TodoEnhancerToolWindowFactory.kt`** — Factory registered in plugin.xml
  that builds the "TODO Enhancer" tool window when the IDE first opens it. Creates a
  `TodoEnhancerPanel` tied to the tool window's disposable so its listeners are cleaned
  up automatically.

## Tests (`src/test/kotlin`)

- **`parser/TodoMetadataParserTest.kt`** — JUnit 4 unit tests for `TodoMetadataParser`,
  covering each metadata token (assignee, priority, tags, due date) individually and in
  combination. Also exercises edge cases such as invalid dates and email addresses that
  must not be mistaken for assignees.
- **`services/TodoScanServiceTest.kt`** — Platform-level test (`BasePlatformTestCase`)
  that runs `TodoScanService.scan()` against in-memory Java files in a headless IDE
  fixture. Verifies that TODO/FIXME comments are indexed, metadata is parsed into
  `TodoEntry` fields, and files without TODOs yield an empty list.

## Resources (`src/main/resources`)

- **`META-INF/plugin.xml`** — Plugin descriptor declaring the plugin id, name, vendor,
  Marketplace description, and its dependency on the core IntelliJ platform module.
  Registers the `messages.TodoEnhancerBundle` resource bundle and the bottom-anchored
  "TODO Enhancer" tool window backed by `TodoEnhancerToolWindowFactory`.
- **`messages/TodoEnhancerBundle.properties`** — Message bundle holding every
  user-visible string (tool window title, filter labels, column headers, status
  messages) keyed by property name. Loaded through `TodoEnhancerBundle.kt` so the UI
  never hard-codes display text.

## Build configuration

- **`build.gradle.kts`** — Gradle build script wiring the Kotlin JVM (Java 21 toolchain)
  and IntelliJ Platform Gradle plugins to build the plugin against IntelliJ IDEA 2025.2.
  Also configures plugin signing and Marketplace publishing from environment variables,
  plugin verification, and the changelog plugin.
- **`settings.gradle.kts`** — Gradle settings script that names the root project and pins
  the versions of the Kotlin, changelog, foojay toolchain resolver, and IntelliJ Platform
  settings plugins. Also configures dependency resolution to use Maven Central plus the
  default JetBrains IntelliJ Platform repositories.
- **`gradle.properties`** — Central Gradle coordinates for the plugin: the group,
  version, and repository URL consumed by `build.gradle.kts`. Also enables Gradle
  configuration/build caching and opts out of bundling the Kotlin standard library.

## CI (`.github/workflows`)

- **`build.yml`** — GitHub Actions workflow that builds the plugin ZIP, runs the test
  suite (`check`), and runs the JetBrains Plugin Verifier on every push to `main` and
  every pull request. On non-PR runs it also prepares a draft GitHub release from the
  unreleased changelog section.
- **`release.yml`** — GitHub Actions workflow triggered when a GitHub release is
  published; it signs and publishes the plugin to the JetBrains Marketplace using
  repository secrets. Afterwards it uploads the built ZIP as a release asset and opens a
  pull request that patches `CHANGELOG.md` for the released version.
