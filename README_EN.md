# Hiro

![Version](https://img.shields.io/badge/Version-1.5.3--alpha--cmp1.11.1-red?style=flat-square)
[![Maven](https://img.shields.io/badge/Maven-EarzuChan-blue?style=flat-square)](https://earzuchan.github.io/maven/)
[![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)](https://opensource.org/license/MIT)

[中文](README.md)

Run the Skia (Skiko) version of Compose Multiplatform on Android.

![Hiro](art/banner.png)

> The project name is inspired by the character **二階堂ヒロ** from **魔法少女ノ魔女裁判**

## Benefits

1. Enables GPU rendering and SkSL Shaders on lower-version Android devices (API 24+), bringing special effects (such as Liquid Glass or complex texture materials) to older devices that otherwise lack native support.
2. Allows style creators to provide a single set of styles (SkSL Shaders, etc.) targetting `skiko` or `desktop` (which inherits from `skiko`). Users can apply these effects on Android without requiring you to modify your library or maintain a separate AndroidX (AGSL) version.

## Caveats or Limitations

1. Since this relies on Skia, bundling the corresponding `.so` (dynamic shared libraries) is unavoidable, which increases the APK size by approximately 20MB.
2. The integration framework is still in its early stages. There may be bugs or unimplemented features in less-common use cases, and performance is not yet fully optimized.

## Project Structure

- Hiro Skia: Packages Skia (Skiko) to provide `SkiaLayer` and `SkiaSurfaceView` on Android. This also serves as the infrastructure for Hiro Compose.
- Hiro Compose: Packages the Skia (Skiko) / Desktop (JVM) components of Compose Multiplatform (UI, Runtime, Foundation, etc.) and adapts the Android runtime environment (handling input events and providing `HiroComposeView`, etc.).
- Hiro Material 3: Packages and adapts Compose Multiplatform's Material 3 for Skia (Skiko) / Desktop on Android.
- Hiro Gradle Plugin: Helps users seamlessly redirect third-party Compose package dependencies from Android targets to Skia (Skiko) / Desktop targets, preventing unintended dependencies on AndroidX Compose or Compose Multiplatform's original Android targets.
- Examples, Sample implementations: Including basic Hiro Compose, Hiro Compose Material 3, and integration with third-party libraries.

## Get Started

1. Create a standard Android project.
2. Configure Maven Repositories: Add the following configuration to the **`settings.gradle.kts`** file in your project's root directory:
   Add my repository to the `repositories` block in both `pluginManagement` and `dependencyResolutionManagement`:
   ```kotlin
   maven("https://earzuchan.github.io/Maven/") // Note: Make sure the letter "M" in "Maven" is capitalized
   ```
   Add the JetBrains Compose Dev repository to the `repositories` block in `dependencyResolutionManagement`:
   ```kotlin
   maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
   ```
3. Apply the Gradle plugin: In your root `build.gradle.kts` file's `plugins` block, add:
   ```kotlin
   id("me.earzuchan.hiro") version "<VERSION>" apply false
   ```
   In your Android application module's `build.gradle.kts` file's `plugins` block, add:
   ```kotlin
   id("me.earzuchan.hiro")
   ```
4. Add the dependency: In your Android module's `build.gradle.kts` file's `dependencies` block, add:
   ```kotlin
   implementation("me.earzuchan.hiro:compose:<VERSION>")
   ```
   *Note on additional modules:*
    * Hiro Skia: `hiro:compose` uses `hiro:skia` internally, but does not expose its APIs to your project. If you need direct access to Skia APIs (advanced usage), you must add it explicitly.
    * Hiro Material 3: Material 3 components are not bundled with `hiro:compose`. If you want to use them, you must add this module.
   ```kotlin
   // Add these only if needed:
   implementation("me.earzuchan.hiro:skia:<VERSION>") // For direct Hiro Skia API access
   implementation("me.earzuchan.hiro:material-3:<VERSION>") // For Material 3 components
   ```
5. Usage: Import `me.earzuchan.hiro.compose.setHiroComposeContent` in your Activity, then use it inside `onCreate`:
   ```kotlin
   setHiroComposeContent {
      // Compose Content
   }
   ```

## Notes

- **Upstream Updates & Dependencies**: The internal upstream dependencies for Compose and Skiko in this project are updated alongside major stable releases of Compose Multiplatform (CMP). Additionally, when using Hiro Compose or Hiro Material 3, you do not need to include the official (original) Compose dependencies (either AndroidX or CMP). This ensures all the Compose content resolves against our specific classpath (Hiro's Compose Skiko for Android). The Hiro Gradle Plugin (HGP) is also designed to help prevent the accidental inclusion of original Compose dependencies
- **Material 3 Ripple Reproduction**: We have replicated the AOSP Material 3 ripple animation for Compose Skiko based on its underlying mechanics. This provides a consistent Material 3 ripple experience even on older Android versions or highly customized ROMs. This implementation is included in Hiro Material 3, and we may consider contributing it upstream to CMP in the future
- **JNI Fix**: We patched a `JNI Critical Section Violation: using JNI after critical get` issue in the JetBrains Skiko native library on Android. Hiro Skia bundles our custom-built native libraries (supporting x64 and Arm64) containing this fix. We may also look into contributing this fix back to the upstream repository
- **Hiro Skia Limitations**: Hiro Skia is primarily designed to support Hiro Compose. Rather than relying on the official JetBrains Android examples—which contain known bugs—we implemented our own lightweight Skia Layer and SurfaceView. While this custom implementation is sufficient for rendering Hiro Compose, its use for standalone Skia rendering has not been thoroughly tested

## Known Issues & Progress

Please refer to the [Todo List](docs/todo.md).

## One Last Word

- Contact: kontakt@earzuchan.me

### About This Project

- Released under the MIT License. If this project helps you, stars are always appreciated
- Developed with assistance from AI tools, including OpenAI Codex
- It's experimental. No guarantees are made regarding production readiness. Please evaluate it carefully before using it in production, and use it at your own risk
- Development is ongoing to improve usability and stability