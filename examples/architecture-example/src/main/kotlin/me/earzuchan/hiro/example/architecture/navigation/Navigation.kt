package me.earzuchan.hiro.example.architecture.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data class Detail(val serial: Int) : NavKey