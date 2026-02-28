# Currere Development Guidelines


## Commands

# Add commands for Kotlin 2.0.21

## Code Style

Kotlin 2.0.21: Follow standard conventions

## Development Rules

- Follow KISS, SOLID, DRY, and YAGNI principles
- Keep comments to a minimum — let the code explain itself
- Write tests for new business logic
- Check linting after implementation and fix any issues
- Verify the app builds: `./gradlew assembleDebug`
- Run all tests and ensure they pass: `./gradlew test`

## Tool Restrictions

- **Never use the Dart MCP tools** in this project. This is a Kotlin/Android project, not a Dart/Flutter project.
