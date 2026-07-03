<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# TODO Enhancer Changelog

## [Unreleased]

### Added

- Enhanced TODO tool window that lists every TODO/FIXME in the project on top of IntelliJ's built-in TODO index.
- Structured metadata parsing in comments: assignee `@name` (or `TODO(name)`), priority `!p1`/`!high`, tags `#tag`, and due dates `due:YYYY-MM-DD`.
- Sort and filter the TODO table by type, priority, assignee, tag, or free text, plus an "Only mine" filter.
- Double-click (or Enter) to jump straight to a TODO in the editor.
- Debounced automatic refresh as you edit, plus a manual Refresh action.
