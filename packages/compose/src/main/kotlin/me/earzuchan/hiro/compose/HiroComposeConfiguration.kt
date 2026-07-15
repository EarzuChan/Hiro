package me.earzuchan.hiro.compose

import androidx.compose.ui.SystemTheme
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration
import me.earzuchan.hiro.compose.windowinsets.HiroWindowInsetsSnapshot
import me.earzuchan.hiro.compose.windowinsets.normalized

/** 限制 Hiro Compose 配置 DSL 的接收者作用域 */
@DslMarker
annotation class HiroComposeConfigurationDsl

/** 单个 HiroComposeView 使用的不可变配置 */
class HiroComposeConfiguration private constructor(
    internal val savableStateConfiguration: HiroSavableStateConfiguration,
    internal val environment: HiroEnvironmentConfiguration,
) {
    companion object {
        @JvmField
        val DEFAULT = HiroComposeConfigurationBuilder().build()

        internal fun create(savableStateConfiguration: HiroSavableStateConfiguration, environment: HiroEnvironmentConfiguration) = HiroComposeConfiguration(
            savableStateConfiguration = savableStateConfiguration,
            environment = environment,
        )
    }
}

/** 构造 HiroComposeConfiguration 的 DSL 接收者 */
@HiroComposeConfigurationDsl
class HiroComposeConfigurationBuilder internal constructor() {
    private var savableStateConfiguration = HiroSavableStateConfiguration.DEFAULT
    private val environmentBuilder = HiroEnvironmentConfigurationBuilder()

    fun savableState(configuration: HiroSavableStateConfiguration) {
        savableStateConfiguration = configuration
    }

    fun savableState(action: HiroSavableStateConfiguration.Builder.() -> Unit) {
        savableStateConfiguration = savableStateConfiguration.toBuilder().apply(action).build()
    }

    fun environment(action: HiroEnvironmentConfigurationBuilder.() -> Unit) {
        environmentBuilder.apply(action)
    }

    internal fun build() = HiroComposeConfiguration.create(
        savableStateConfiguration = savableStateConfiguration,
        environment = environmentBuilder.build(),
    )
}

/** 构造 Hiro Compose 平台环境策略的 DSL 接收者 */
@HiroComposeConfigurationDsl
class HiroEnvironmentConfigurationBuilder internal constructor() {
    private var densityPolicy: HiroSystemValuePolicy<Float> = HiroSystemValuePolicy.FollowSystem()
    private var fontScalePolicy: HiroSystemValuePolicy<Float> = HiroSystemValuePolicy.FollowSystem()
    private var layoutDirectionPolicy: HiroSystemValuePolicy<LayoutDirection> = HiroSystemValuePolicy.FollowSystem()
    private var systemThemePolicy: HiroSystemValuePolicy<SystemTheme> = HiroSystemValuePolicy.FollowSystem()
    private var localeListPolicy: HiroSystemValuePolicy<LocaleList> = HiroSystemValuePolicy.FollowSystem()
    private var windowInsetsPolicy: HiroWindowInsetsPolicy = HiroWindowInsetsPolicy.FollowSystem(HiroWindowInsetsSnapshot.Zero)
    private var viewConfigurationPolicy: HiroViewConfigurationPolicy = HiroViewConfigurationPolicy.FollowSystem()

    fun density(action: HiroSystemValuePolicyBuilder<Float>.() -> Unit) {
        densityPolicy = HiroSystemValuePolicyBuilder(densityPolicy, ::validScale).apply(action).build()
    }

    fun fontScale(action: HiroSystemValuePolicyBuilder<Float>.() -> Unit) {
        fontScalePolicy = HiroSystemValuePolicyBuilder(fontScalePolicy, ::validScale).apply(action).build()
    }

    fun layoutDirection(action: HiroSystemValuePolicyBuilder<LayoutDirection>.() -> Unit) {
        layoutDirectionPolicy = HiroSystemValuePolicyBuilder(layoutDirectionPolicy) { it }.apply(action).build()
    }

    fun systemTheme(action: HiroSystemValuePolicyBuilder<SystemTheme>.() -> Unit) {
        systemThemePolicy = HiroSystemValuePolicyBuilder(systemThemePolicy) { it }.apply(action).build()
    }

    fun localeList(action: HiroSystemValuePolicyBuilder<LocaleList>.() -> Unit) {
        localeListPolicy = HiroSystemValuePolicyBuilder(localeListPolicy) { LocaleList(it.localeList.toList()) }.apply(action).build()
    }

    fun windowInsets(action: HiroWindowInsetsPolicyBuilder.() -> Unit) {
        windowInsetsPolicy = HiroWindowInsetsPolicyBuilder(windowInsetsPolicy).apply(action).build()
    }

    fun viewConfiguration(action: HiroViewConfigurationPolicyBuilder.() -> Unit) {
        viewConfigurationPolicy = HiroViewConfigurationPolicyBuilder(viewConfigurationPolicy).apply(action).build()
    }

    internal fun build() = HiroEnvironmentConfiguration(
        densityPolicy = densityPolicy,
        fontScalePolicy = fontScalePolicy,
        layoutDirectionPolicy = layoutDirectionPolicy,
        systemThemePolicy = systemThemePolicy,
        localeListPolicy = localeListPolicy,
        windowInsetsPolicy = windowInsetsPolicy,
        viewConfigurationPolicy = viewConfigurationPolicy,
    )
}

/** 为一个同步系统值选择跟随、固定或变换策略 */
@HiroComposeConfigurationDsl
class HiroSystemValuePolicyBuilder<T> internal constructor(initial: HiroSystemValuePolicy<T>, private val normalize: (T) -> T) {
    private var policy = initial

    fun followSystem() {
        policy = HiroSystemValuePolicy.FollowSystem(normalize)
    }

    fun fixed(value: T) {
        policy = HiroSystemValuePolicy.Fixed(normalize(value))
    }

    fun transformSystem(transformer: (T) -> T) {
        policy = HiroSystemValuePolicy.TransformSystem(transformer, normalize)
    }

    internal fun build() = policy
}

/** 为窗口 Insets 选择系统跟随、固定或系统变换策略 */
@HiroComposeConfigurationDsl
class HiroWindowInsetsPolicyBuilder internal constructor(initial: HiroWindowInsetsPolicy) {
    private var policy = initial

    fun followSystem(initial: HiroWindowInsetsSnapshot = HiroWindowInsetsSnapshot.Zero) {
        policy = HiroWindowInsetsPolicy.FollowSystem(initial.normalized())
    }

    fun fixed(snapshot: HiroWindowInsetsSnapshot) {
        policy = HiroWindowInsetsPolicy.Fixed(snapshot.normalized())
    }

    fun transformSystem(initial: HiroWindowInsetsSnapshot = HiroWindowInsetsSnapshot.Zero, transformer: (HiroWindowInsetsSnapshot) -> HiroWindowInsetsSnapshot) {
        policy = HiroWindowInsetsPolicy.TransformSystem(initial.normalized(), transformer)
    }

    internal fun build() = policy
}

internal data class HiroEnvironmentConfiguration(
    val densityPolicy: HiroSystemValuePolicy<Float>,
    val fontScalePolicy: HiroSystemValuePolicy<Float>,
    val layoutDirectionPolicy: HiroSystemValuePolicy<LayoutDirection>,
    val systemThemePolicy: HiroSystemValuePolicy<SystemTheme>,
    val localeListPolicy: HiroSystemValuePolicy<LocaleList>,
    val windowInsetsPolicy: HiroWindowInsetsPolicy,
    val viewConfigurationPolicy: HiroViewConfigurationPolicy,
)

internal sealed interface HiroSystemValuePolicy<T> {
    fun resolve(system: T): T

    class FollowSystem<T>(private val normalize: (T) -> T = { it }) : HiroSystemValuePolicy<T> {
        override fun resolve(system: T) = normalize(system)
    }

    class Fixed<T>(private val value: T) : HiroSystemValuePolicy<T> {
        override fun resolve(system: T) = value
    }

    class TransformSystem<T>(private val transformer: (T) -> T, private val normalize: (T) -> T) : HiroSystemValuePolicy<T> {
        override fun resolve(system: T) = normalize(transformer(system))
    }
}

internal sealed interface HiroWindowInsetsPolicy {
    val initial: HiroWindowInsetsSnapshot
    val readsSystem: Boolean
    fun resolve(system: HiroWindowInsetsSnapshot): HiroWindowInsetsSnapshot

    class FollowSystem(override val initial: HiroWindowInsetsSnapshot) : HiroWindowInsetsPolicy {
        override val readsSystem = true
        override fun resolve(system: HiroWindowInsetsSnapshot) = system.normalized()
    }

    class Fixed(private val snapshot: HiroWindowInsetsSnapshot) : HiroWindowInsetsPolicy {
        override val initial = snapshot
        override val readsSystem = false
        override fun resolve(system: HiroWindowInsetsSnapshot) = snapshot
    }

    class TransformSystem(override val initial: HiroWindowInsetsSnapshot, private val transformer: (HiroWindowInsetsSnapshot) -> HiroWindowInsetsSnapshot) : HiroWindowInsetsPolicy {
        override val readsSystem = true
        override fun resolve(system: HiroWindowInsetsSnapshot) = transformer(system).normalized()
    }
}

/** 以 DSL 创建一个新的不可变 Hiro Compose 配置 */
fun hiroComposeConfiguration(action: HiroComposeConfigurationBuilder.() -> Unit) = HiroComposeConfigurationBuilder().apply(action).build()

private fun validScale(value: Float): Float {
    require(value.isFinite() && value > 0f) { "Density 与 FontScale 必须是正有限值" }
    return value
}
