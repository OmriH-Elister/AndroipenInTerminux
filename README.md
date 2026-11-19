# Open in Terminal

An Android helper that adds an **Open in terminal** action when one folder is selected in Samsung My Files. It reads the selected folder path through a narrowly scoped accessibility workflow and opens a Termux session in that directory.

This project is unofficial and is not affiliated with or endorsed by Samsung Electronics or the Termux project.

## Current status

- Version: 1.0.2
- Tested on: Samsung Galaxy S22+, Android 14
- Package: `com.omri.openinterminal`
- Minimum SDK: 26
- Target SDK: 34
- Repository status: private development repository

## How it works

1. Long-press one folder in Samsung My Files.
2. Tap the **Open in terminal** overlay.
3. The accessibility service opens My Files' Details panel and reads the displayed path.
4. The app starts a Termux session using Termux's `RUN_COMMAND` API.

The app has no Internet permission and does not retain or transmit selected paths.

## Requirements

- Samsung My Files (`com.sec.android.app.myfiles`)
- Termux with `allow-external-apps = true` in `~/.termux/termux.properties`
- Termux command permission granted to this app
- The app's accessibility service enabled by the user

## Tests

The deterministic path and overlay-isolation logic have plain Java tests:

```sh
mkdir -p build/test-classes
javac -d build/test-classes \
  src/com/omri/openinterminal/EventPolicy.java \
  tests/com/omri/openinterminal/EventPolicyTest.java \
  src/com/omri/openinterminal/PathParser.java \
  tests/com/omri/openinterminal/PathParserTest.java
java -cp build/test-classes com.omri.openinterminal.EventPolicyTest
java -cp build/test-classes com.omri.openinterminal.PathParserTest
```

## Publishing notes

The current build is suitable for local testing, not store distribution. Before publishing:

- migrate to a standard Gradle Android project;
- target the API level currently required by the store;
- add a dedicated Accessibility API disclosure and affirmative consent screen;
- prepare the Play Console Accessibility API declaration and demonstration video;
- use a private release-signing key stored outside the repository;
- review branding, privacy policy, store listing, and data-safety declarations.

## Source layout

- `src/` — Java application source
- `res/` — Android resources
- `tests/` — plain Java regression tests
- `AndroidManifest.xml` — application and accessibility-service declarations
