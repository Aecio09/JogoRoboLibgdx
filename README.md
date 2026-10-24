# ProjetoAlgoritimos

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.
- `android`: Android platform, used to generate the APK.

## Android (APK)

Prerequisites:

- JDK 17+ (the project builds with Java 21).
- Android SDK with `platforms;android-36` and `build-tools;36.0.0`.
  Point to it either with the `ANDROID_HOME` environment variable or with a
  `local.properties` file in the project root containing `sdk.dir=/path/to/sdk`.

Build the debug APK:

```sh
./gradlew :android:assembleDebug
```

The APK is written to `android/build/outputs/apk/debug/android-debug.apk`.
Install it on a connected device or emulator with:

```sh
adb install -r android/build/outputs/apk/debug/android-debug.apk
```

Or do both in one step (installs and launches the game):

```sh
./gradlew :android:run
```

A release APK can be generated with `./gradlew :android:assembleRelease`; for
publishing you still need to configure a signing key in `android/build.gradle`.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
