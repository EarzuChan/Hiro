package androidx.compose.runtime.saveable.serialization

import androidx.compose.runtime.saveable.Saver
import androidx.savedstate.SavedState
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

internal inline fun <reified Serializable : Any> serializableSaver(configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT): Saver<Serializable, SavedState> = serializableSaver(
    serializer = configuration.serializersModule.serializer(),
    configuration = configuration
)

internal fun <Serializable : Any> serializableSaver(serializer: KSerializer<Serializable>, configuration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT): Saver<Serializable, SavedState> = Saver(
    save = { original -> encodeToSavedState(serializer, original, configuration) },
    restore = { savedState -> decodeFromSavedState(serializer, savedState, configuration) }
)
