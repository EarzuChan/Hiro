import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.processToHiroJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

// TIPS：本机库打包的是我们地地道道的修复版

val skikoJar = processToHiroJar("skiko") {
    // 版本须与本机库所移植的 Skiko 一致，否则 JNI 签名对不上
    artifact("org.jetbrains.skiko:skiko-android:0.150.2")

    // 处理：本机库已裁掉这些包装的 JNI，且 Hiro 与 Compose 均不引用，直接剔除，使误用在编译期暴露

    dropPathPrefix(
        "org/jetbrains/skia/skottie/",
        "org/jetbrains/skia/sksg/",
        "org/jetbrains/skia/Codec.class", "org/jetbrains/skia/Codec$", "org/jetbrains/skia/CodecKt",
        "org/jetbrains/skia/DynamicMemoryWStream.class", "org/jetbrains/skia/DynamicMemoryWStream$", "org/jetbrains/skia/DynamicMemoryWStreamKt",
        "org/jetbrains/skia/Graphics.class", "org/jetbrains/skia/Graphics$", "org/jetbrains/skia/GraphicsKt",
        "org/jetbrains/skia/OutputWStream.class", "org/jetbrains/skia/OutputWStream$", "org/jetbrains/skia/OutputWStreamKt",
        "org/jetbrains/skia/PaintFilterCanvas.class", "org/jetbrains/skia/PaintFilterCanvas$", "org/jetbrains/skia/PaintFilterCanvasKt", "org/jetbrains/skia/PaintFilterCanvas_jvmKt",
        "org/jetbrains/skia/PathUtils.class", "org/jetbrains/skia/PathUtils$", "org/jetbrains/skia/PathUtilsKt",
        "org/jetbrains/skia/TextBlobBuilder.class", "org/jetbrains/skia/TextBlobBuilder$", "org/jetbrains/skia/TextBlobBuilderKt",
        "org/jetbrains/skia/shaper/JavaTextBidiRunIterator",
        "org/jetbrains/skia/shaper/TextBlobBuilderRunHandler",
        "org/jetbrains/skia/shaper/TrivialBidiRunIterator",
        "org/jetbrains/skia/shaper/TrivialFontRunIterator",
        "org/jetbrains/skia/shaper/TrivialScriptRunIterator",
        "org/jetbrains/skia/svg/SVGCanvas",
        "org/jetbrains/skiko/AngleApi",
        "org/jetbrains/skiko/AngleSupport_jvmKt",
        "org/jetbrains/skiko/NativeApplicationKt"
    )

    // 校验

    requireJarEntry(
        "org/jetbrains/skia/Canvas.class",
        "org/jetbrains/skia/Image.class",
        "org/jetbrains/skia/RuntimeShaderBuilder.class",
        "org/jetbrains/skia/paragraph/Paragraph.class",
        "org/jetbrains/skiko/Library.class"
    )

    forbidJarEntryFragment("org/jetbrains/skia/skottie/", "org/jetbrains/skia/Codec.class", "org/jetbrains/skia/TextBlobBuilder.class")
}

android {
    namespace = "${HiroBuildConfig.namespace}.skia"
    compileSdk = HiroBuildConfig.androidCompileSdk

    defaultConfig {
        minSdk = HiroBuildConfig.androidMinSdk
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging.jniLibs.keepDebugSymbols += "**/*.so"

    publishing { singleVariant("release") { withSourcesJar() } }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.moduleName.set("hiro-skia")
    compilerOptions.optIn.add("me.earzuchan.hiro.log.HiroInternalApi")
}

dependencies {
    // Skiko 经处理后随 Hiro Skia 发布，用户无需再引入 JetBrains Compose Dev 仓库
    api(skikoJar.files)

    // Skiko 运行时所需
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "skia"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}

tasks.named("check") { dependsOn(skikoJar) }
