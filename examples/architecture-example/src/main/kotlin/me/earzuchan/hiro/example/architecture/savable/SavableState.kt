package me.earzuchan.hiro.example.architecture.savable

import android.os.Bundle
import kotlinx.serialization.Serializable
import me.earzuchan.hiro.compose.savable.hiroSavableStateCodec
import me.earzuchan.hiro.compose.savable.hiroSavableStateConfiguration

@Serializable
data object AutomaticSavableObjectKey

@Serializable
data class AutomaticSavableKey(val scope: String)

data class ThirdPartySavableKey(val scope: String)

private const val SCOPE = "scope"

internal val architectureSavableStateConfiguration = hiroSavableStateConfiguration {
    addCodec(
        hiroSavableStateCodec<ThirdPartySavableKey>(
            typeId = "me.earzuchan.hiro.example.architecture.third-party-key",
            serialize = { key -> Bundle().apply { putString(SCOPE, key.scope) } },
            deserialize = { state -> ThirdPartySavableKey(checkNotNull(state.getString(SCOPE))) },
        )
    )
}
