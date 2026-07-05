# Hiro Gradle 骨架

版本：1.11.1。

## 构建基线

- Gradle 使用 9 系列；当前已用`9.5.1`验证
- Android Gradle Plugin 使用`9.0.1`
- Kotlin 当前保留`2.2.20`，供`hiro-gradle-plugin`的 JVM 插件和 Compose 编译器插件使用
- 普通 Android 库和示例应用采用 AGP 9 内建 Kotlin，不再应用`org.jetbrains.kotlin.android`
- 当前公开包按 Android AAR 骨架发布；如果后续模块需要成为 KMP 模块，则按 AGP 9 最佳实践改用`com.android.kotlin.multiplatform.library`

## 公有坐标：
- `me.earzuchan.hiro:skiko:<VER>`：Skiko Android等代码
- `me.earzuchan.hiro:compose:<VER>`：Compose 界面层、运行时等依赖加我们的胶水代码
- `me.earzuchan.hiro:material3:<VER>`
- `me.earzuchan.hiro:hiro:<VER>`：引入这个等同引入 `skiko + compose`
- `me.earzuchan.hiro:hiro-gradle-plugin:<VER>`

`hiro` 只是把 `skiko + compose` 合了（以便用户化简引入），本身没有业务逻辑。`material3` 是独立的，Legacy Material 因没啥人用我们不提供。
