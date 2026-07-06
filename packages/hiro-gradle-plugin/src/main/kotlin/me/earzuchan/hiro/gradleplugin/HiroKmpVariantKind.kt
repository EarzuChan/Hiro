package me.earzuchan.hiro.gradleplugin

internal enum class HiroKmpVariantKind(val wireName: String) {
    SKIKO("skiko"),
    JVM("jvm"),
    DESKTOP("desktop");

    companion object {
        val priority = listOf(SKIKO, JVM, DESKTOP)

        fun fromVariantName(variantName: String): HiroKmpVariantKind {
            val normalized = variantName.removeSuffix("-published")

            return entries.firstOrNull { candidate -> normalized == "${candidate.wireName}ApiElements" || normalized == "${candidate.wireName}RuntimeElements" } ?: error("不是 Hiro 可识别的 KMP 候选变体：$variantName")
        }
    }
}
