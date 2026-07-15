package me.earzuchan.hiro.compose.windowinsets

import me.earzuchan.hiro.compose.HiroComposeConfigurationDsl

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
