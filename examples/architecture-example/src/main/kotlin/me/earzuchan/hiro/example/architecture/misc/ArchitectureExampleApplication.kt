package me.earzuchan.hiro.example.architecture.misc

import android.app.Application
import me.earzuchan.hiro.example.architecture.viewmodel.ArchitectureViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class ArchitectureExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ArchitectureExampleApplication)
            modules(architectureModule)
        }
    }
}

private val architectureModule = module { viewModelOf(::ArchitectureViewModel) }
