# Hiro

![Version](https://img.shields.io/badge/Version-1.6.1--alpha--cmp1.11.1-red?style=flat-square)
[![Maven](https://img.shields.io/badge/Maven-EarzuChan-blue?style=flat-square)](https://earzuchan.github.io/maven/)
[![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)](https://opensource.org/license/MIT)

[简体中文](README.md)

Using the Skia (Skiko) edition of Compose Multiplatform on Android.

![Hiro](art/banner.png)

> The project name is inspired by the character **Nikaido Hiro** from *Magical Girl Witch Trials* (魔法少女ノ魔女裁判).

## Benefits

1. Allows older Android devices (API 28, Android 9) to utilize GPU rendering and SkSL shaders, addressing the limitation of these devices not being able to run special visual effects (such as the Liquid Glass or rich textured materials).
2. This allows you (the style creator) to provide a single set of styles (such as SkSL shaders) compatible with `skiko` or `desktop` (which inherits from `skiko`). You do not need to modify your library or provide an AndroidX (AGSL) version to let users apply the full visual effects on Android.

## Notes & Trade-offs

1. Because this relies on a self-provided Skia implementation, packaging the corresponding `.so` (dynamic shared libraries) and related resources is unavoidable. This will increase your APK size by approximately 6MB when `bundling both x86_84 and arm64-v8a native libraries and resources`. Efforts have been made to minimize the footprint through stripping and optimization.
2. The integration system is not yet fully complete. There may be bugs or missing features in niche edge cases, and performance might not be optimal in all scenarios.

## What is Provided

- **Hiro Skia**: A packaging of Skia (Skiko) that provides `SkiaLayer` and `SkiaSurfaceView` on Android. This serves as the infrastructure of Hiro Compose.
- **Hiro Compose**: Built upon the Skiko Desktop versions of Compose Multiplatform's UI/Runtime/Foundation and related components as a base. It has been extensively modified to adapt the Android experience (handling various input events, providing `HiroComposeView`, etc.).
- **Hiro Material 3**: A packaging of Compose Multiplatform's Material 3 for Skia (Skiko)/Desktop (JVM), adapted for a consistent Android experience.
- **Hiro Gradle Plugin**: Helps users seamlessly transition third-party Compose packages to use Skia (Skiko)/Desktop modules (redirecting Android to use Skia (Skiko)/Desktop while blocking incoming dependencies on AndroidX Compose or the Android target of Compose Multiplatform).
- **Examples**: Several sample projects demonstrating basic Hiro Compose, Hiro Compose Material 3, and Hiro Compose integrated with third-party libraries.
- **[Whole App Example](https://github.com/EarzuChan/HiroWholeAppExample)**: A simple yet structurally complete application demonstrating the feasibility of using Hiro to develop functional applications.

## Getting Started

1. Create a standard Android project.
2. Configure Maven repositories: Add the following configuration to the **`settings.gradle.kts`** file in your project root:
   Add the repository in the `repositories` block of both `pluginManagement` and `dependencyResolutionManagement`:
   ```kotlin
   maven("https://earzuchan.github.io/Maven/") // Note: Please ensure the "M" in "Maven" is capitalized
   ```
   Also, add the JetBrains Compose Dev repository in the `repositories` block of `dependencyResolutionManagement`:
   ```kotlin
   maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
   ```
3. Apply the Gradle plugin: Add the following to the `plugins` block of your root `build.gradle.kts` file:
   ```kotlin
   id("me.earzuchan.hiro") version "<VERSION>" apply false
   ```
   Then, add the following to the `plugins` block of your Android module's `build.gradle.kts` file:
   ```kotlin
   id("me.earzuchan.hiro")
   ```
4. Add dependencies: Add the following to the `dependencies` block of your Android module's `build.gradle.kts` file:
   ```kotlin
   implementation("me.earzuchan.hiro:compose:<VERSION>")
   ```
   *Notes on other modules:*
   * **Hiro Skia**: `hiro:compose` internally uses `hiro:skia`, but does not expose its APIs to your project. If you need to access the Skia APIs directly (advanced usage), you must explicitly add this dependency.
   * **Hiro Material 3**: Material 3 are not bundled in `hiro:compose`. To use them, you must add this module.
   ```kotlin
   // Add only when needed:
   implementation("me.earzuchan.hiro:skia:<VERSION>") // To directly call the Hiro Skia API
   implementation("me.earzuchan.hiro:material-3:<VERSION>") // To use Material 3 components
   ```
5. Usage: Import `me.earzuchan.hiro.compose.setHiroComposeContent` in your Activity, then use it inside `onCreate`:
   ```kotlin
   setHiroComposeContent {
      // Compose content
   }
   ```

## Technical Notes

- The internal upstream dependencies of Compose and Skiko in this project are planned to be updated alongside major stable releases of Compose Multiplatform (CMP). Additionally, when using Hiro Compose or Hiro Material 3, you should not import official (original) Compose dependencies (either AndroidX or CMP). This ensures that all Compose content is resolved based on the Hiro-specific classpath (Hiro's exclusive Android Compose Skiko). The Hiro Gradle Plugin (HGP) also helps prevent you and your dependencies from introducing original Compose artifacts.
- For the time being, Skia is integrated only with OpenGL, and Vulkan is not included. This decision was made considering APK size, device compatibility, and implementation complexity.
- The ripple effect has been recreated based on the underlying principles of the AOSP Material 3 ripple animation, using the Skia graphics stack. This ensures a consistent Material 3 ripple experience even on older Android versions or custom ROMs. This recreation is included in Hiro Material 3 and may eventually be contributed back upstream to CMP.
- Issues in JetBrains Skiko's native code regarding `JNI Critical Section Violation: using JNI after critical get` and JVM array type mismatches in `BreakIterator` have been addressed. Hiro Skia includes the native libraries (x64 and Arm64) built with these fixes. A [pull request for the former issue](https://github.com/JetBrains/skiko/pull/1235) has been submitted to Skiko, while the latter is being evaluated for a future contribution.
- Hiro Skia is designed primarily to support Hiro Compose. Rather than relying on the official JetBrains Android codes—which contain known bugs (such as passing `width, width` to `Rect.makeWH`)—this project implements a lightweight custom Skia Layer and SurfaceView. While this implementation is functional for Hiro Compose, standalone Skia rendering use cases have not been extensively tested.
- In Hiro Compose, Compose and Skia/GL run on an independent thread, communicating with the Android main thread via state machine models. The `ViewModelStore` within Hiro Compose is isolated from the Android host (e.g., Activity) by design for performance reasons. Hiro Compose adapts essential Android inputs (clicks, navigation integration, etc.), Lifecycle, and SavedState. It also includes out-of-the-box support for `Kotlinx Serialization @Serializable` in SavedState, alongside optional support for custom-typed serializer/deserializer (SeDes) SavedState implementations. For more details on Hiro Compose, please refer to the [Architecture Documentation (Chinese only)](docs/hiro-compose-arch.md).

## Known Issues & Progress

Please refer to my [Todo List (Chinese only)](docs/todo.md).

## Closing Remarks

- Contact: kontakt@earzuchan.me

### About this project

- Open-sourced under the MIT License. If this project helps you, please consider giving it a star.
- Developed with the assistance of artificial intelligence tools such as OpenAI Codex.
- This project is experimental. No guarantees are made regarding production readiness; please evaluate and use it in production environments at your own risk.
- Active optimization is ongoing to provide a usable and functional experience.