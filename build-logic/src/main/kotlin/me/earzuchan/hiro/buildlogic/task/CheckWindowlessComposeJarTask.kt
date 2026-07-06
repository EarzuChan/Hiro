package me.earzuchan.hiro.buildlogic.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipFile

abstract class CheckWindowlessComposeJarTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputJar: RegularFileProperty

    @get:Input
    abstract val requiredEntries: ListProperty<String>

    @get:Input
    abstract val forbiddenFragments: ListProperty<String>

    @TaskAction
    fun run() {
        ZipFile(inputJar.get().asFile).use { zip ->
            val entryNames = zip.entries().asSequence().map { it.name }.toSet()
            val missingEntries = requiredEntries.get().filterNot { it in entryNames }
            check(missingEntries.isEmpty()) {
                "窗口无关 Compose 产物缺少必要类：${missingEntries.joinToString()}"
            }

            val leak = entryNames.firstOrNull { entryName ->
                forbiddenFragments.get().any { fragment -> entryName.contains(fragment) }
            }
            check(leak == null) {
                "窗口无关 Compose 产物包含禁止路径：$leak"
            }
        }
    }
}
