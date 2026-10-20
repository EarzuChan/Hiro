# Hiro

![Version](https://img.shields.io/badge/Version-1.9.0--alpha--cmp1.12.2-red?style=flat-square)
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

1. Because this relies on a self-provided Skia implementation, packaging the corresponding `.so` (dynamic shared libraries) and related resources is unavoidable. This will increase your APK size by approximately 6MB when `bundling both x86_64 and arm64-v8a native libraries and resources`. Efforts have been made to minimize the footprint through stripping and optimization.
2. The integration system is not yet fully complete. There may be bugs or missing features in niche edge cases, and performance might not be optimal in all scenarios. See [Known Limitations](#known-limitations) below.

## What is Provided

- **Hiro Skia**: A packaging of Skia (Skiko) that provides `SkiaLayer` and `SkiaSurfaceView` on Android. This serves as the infrastructure of Hiro Compose.
- **Hiro Compose**: Built upon the Skiko Desktop versions of Compose Multiplatform's UI/Runtime/Foundation and related components as a base. It has been extensively modified to adapt the Android experience (handling various input events, providing `HiroComposeView`, etc.).
- **Hiro Material 3**: A packaging of Compose Multiplatform's Material 3 for Skia (Skiko)/Desktop (JVM), adapted for a consistent Android experience.
- **Hiro Gradle Plugin**: Helps users seamlessly transition third-party Compose packages to use Skia (Skiko)/Desktop modules (redirecting Android to use Skia (Skiko)/Desktop while blocking incoming dependencies on AndroidX Compose or the Android target of Compose Multiplatform). It also relinks desktop platform types in those packages (such as `SavedState`) to their Android counterparts, and can generate JetBrains Compose resources (`Res`) for Android-only modules.
- **Examples**, several sample projects: These serve not only as reference planes for my development and testing but also allow you to deeply experience and understand Hiro's capabilities across the board.
- **[Whole App Example](https://github.com/EarzuChan/HiroWholeAppExample)**: A simple yet structurally complete application demonstrating the feasibility of using Hiro to develop functional applications.

## Getting Started

1. Create a standard Android project (AGP 9, Kotlin 2.2 or newer, minSdk 28).
2. Configure Maven repositories: Add the following configuration to the **`settings.gradle.kts`** file in your project root:
   Add the repository in the `repositories` block of both `pluginManagement` and `dependencyResolutionManagement`:
   ```kotlin
   maven("https://earzuchan.github.io/Maven/") // Note: Please ensure the "M" in "Maven" is capitalized
   ```
   Skiko ships inside Hiro Skia, so the JetBrains Compose Dev repository is no longer needed.
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
   * **Hiro Skia**: `hiro:compose` already exposes its APIs transitively, so you do not need to declare it when using Hiro Compose. Add it explicitly only when using Skia on its own.
   * **Hiro Material 3**: Material 3 are not bundled in `hiro:compose`. To use them, you must add this module.
   ```kotlin
   // Add only when needed:
   implementation("me.earzuchan.hiro:skia:<VERSION>") // To use Hiro Skia on its own
   implementation("me.earzuchan.hiro:material3:<VERSION>") // To use Material 3 components
   ```
5. Usage: Import `me.earzuchan.hiro.compose.setHiroComposeContent` in your Activity, then use it inside `onCreate`:
   ```kotlin
   setHiroComposeContent {
      // Compose content
   }
   ```

## Resources

- **Android resources (R and assets)**: Hiro provides the AndroidX-named APIs such as `stringResource`, `pluralStringResource`, `colorResource`, `dimensionResource`, `painterResource`, `ImageVector.vectorResource`, `ImageBitmap.imageResource`, plus `LocalContext`, `LocalConfiguration` and `LocalResources`. Configuration changes such as locale or dark mode recompose automatically.
  - `painterResource` supports vector drawables (gradients included), bitmaps, nine-patches (.9.png) and shape drawables. For animated vectors, use `AnimatedImageVector.animatedVectorResource` with `rememberAnimatedVectorPainter`.
  - Fonts work with `Font(R.font.xxx)` and `Font("fonts/xxx.ttf", assets)`.
- **JetBrains Compose resources (Res)**: Resources in KMP shared modules and third-party libraries work as usual. For an Android-only module, apply the Compose plugin of the version Hiro follows and put files in `src/main/composeResources`; the Hiro Gradle Plugin generates `Res` (package `<group>.<module>.generated.resources` by default; the package and class name can be customized via `compose.resources {}`). The plugin serves only as a build-time resource compiler and configuration entry, and brings no official Compose runtime libraries:
  ```kotlin
  plugins {
      id("org.jetbrains.compose") version "1.12.2"
  }
  ```

## Logging

In non-debuggable (release) apps Hiro only logs warnings and errors, and R8 strips the rest together with their strings. Debuggable apps log at debug level by default, which you can adjust:

```kotlin
import me.earzuchan.hiro.log.HiroLogLevel
import me.earzuchan.hiro.log.HiroLogging

HiroLogging.level = HiroLogLevel.Verbose // per-event logs such as touches and ripples
```

## Special Notes

- The internal upstream dependencies of Compose and Skiko in this project are updated alongside the major stable versions of Compose Multiplatform (CMP). When using Hiro Compose or Hiro Material 3, you do not need to introduce official (original) Compose dependencies (whether AndroidX or CMP), which ensures that all Compose content is resolved based on my specific classpath (Hiro's exclusive Android Compose Skiko). My Hiro Gradle Plugin (HGP) will also help prevent you and your dependencies from introducing original Compose additionally.
- The Compose and Skia/GL of Hiro Compose run on an independent rendering thread, communicating with the Android main thread through a state machine model. It completes basic Android event inputs (clicks, navigation integration, etc.), Lifecycle, and SavedState adaptations (and provides out-of-the-box support for `Kotlinx Serialization @Serializable` in SavedState as a bonus, as well as user-optional (requiring the user to implement an interface) custom type SeDes (serialization and deserialization) SavedState support). For more about Hiro Compose, it is recommended to check the [Architecture Documentation](docs/hiro-compose-arch.md).
- Hiro Skia is basically built for Hiro Compose. Instead of directly relying on JetBrains' official Android code which has known bugs (such as passing `width, width` as parameters to `Rect.makeWH`), I implemented a lightweight set of Skia Layer and SurfaceView, which is basically sufficient for the display of Hiro Compose.
- Based on the underlying principles of the AOSP Material 3 ripple animation, I recreated the effect using the Skia graphics stack. This enables a consistent Material 3 ripple experience even on lower-version Android and heavily modified ROMs. This recreation is included in Hiro Material 3, and I might contribute it separately to upstream CMP later.
- I fixed the `JNI Critical Section Violation: using JNI after critical get` issue present in JetBrains Skiko's native code, and Hiro Skia contains the fixed native library. The [PR](https://github.com/JetBrains/skiko/pull/1235) for this fix has been merged by Skiko, and Hiro will no longer need to patch it itself once it is released with an upstream version; in addition, the array type issue of BreakIterator has already been fixed by upstream itself.

## Known Limitations

- **Multi-threading and Component Isolation**:
    - Since Compose runs on Hiro's independent rendering thread, `LocalContext` and the like should only be used for thread-safe operations such as reading resources; **for operations that require the main thread (such as `Toast`), please switch back to the main thread yourself**.
    - The `ViewModelStore` in the Hiro Compose world does not interoperate with the one in the Android world (such as Activity) (this is a deliberate design choice for performance considerations).
- **Native Library Architectures and Graphics Backends**:
    - The native libraries only include `arm64-v8a` (Arm64) and `x86_64` (x64). On x86 emulators that rely on ARM translation, such as LDPlayer, it may crash because the translation layer fails to load the Skiko native library (the same applies to the official library); for those, please use `x86_64`.
    - Out of considerations for package size, device compatibility, and implementation logic complexity, the Skia graphics backend currently only interfaces with OpenGL ES and does not introduce Vulkan (supporting Vulkan is quite troublesome and is left for future consideration).
- **Skia Trimming and Standalone Usage Limitations**:
    - The native library of Hiro Skia is built with trimming according to what Compose requires—wrappers such as `Codec`, `Skottie`, and `TextBlobBuilder` have been removed; although types such as `TextBlob`, `TextLine`, `Pixmap`, `Drawable`, `Blender`, and `Shaper` are still retained, calling them will throw `UnsatisfiedLinkError`.
    - Using Hiro Skia for standalone Skia hosting purposes outside of Hiro Compose has not been deeply tested.
- **Currently Unsupported System Features and Resources**:
    - Accessibility (screen readers like TalkBack cannot read Hiro content) and drag-and-drop are not yet integrated.
    - `painterResource` currently does not support other XML Drawables such as `selector` and `layer-list`.
    - XML font families (including downloadable fonts) are currently not supported.

## Known Issues & Progress

Please refer to my [Todo List (Chinese only)](docs/todo.md).

## Closing Remarks

- Contact: kontakt@earzuchan.me

### About this project

- Open-sourced under the [MIT](LICENSE) license. If it is helpful to you, please give it a Star. For included and modified third-party works and acknowledgments, see [Third-Party Notices (Chinese only)](THIRD_PARTY_NOTICES.md).
- Assisted by artificial intelligence such as OpenAI GPT and Anthropic Claude during the development process.
- Is experimental in nature. I make no guarantees of complete usability in production environments. Please evaluate for yourself whether to adopt it in a production environment, and you must bear the risks of use yourself.
- Is still under continuous optimization, in an effort to do my best to provide you with a "functional and usable" experience.