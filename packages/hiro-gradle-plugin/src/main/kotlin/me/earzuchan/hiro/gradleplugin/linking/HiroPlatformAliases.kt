package me.earzuchan.hiro.gradleplugin.linking

/**
 * 平台别名：公共层 expect 的类型在桌面上是真类，在安卓上却是别名（typealias）
 *
 * 第三方 KMP 包被 HGP 换成桌面变体后，其字节码按桌面真类链接；而 Hiro 在安卓上使用安卓官方的实现，二者签名对不上。
 * 把桌面类型改写为安卓别名的目标类型，相当于让该字节码按安卓 actual 重新链接
 *
 * @property desktopType 桌面上的真类（内部名）
 * @property androidType 安卓上别名所指的类型（内部名）
 * @property bridgeType 承接桌面专属成员的 Hiro 桥接类（内部名），须具备与桌面真类一一对应的构造器
 * @property bridgeFinisher 桥接类上把自身转为 [androidType] 的无参实例方法名
 * @property memberBridges 桌面真类的实例成员（名称+描述符）到桥接类同名静态方法的映射，静态方法以原接收者为首个参数
 */
internal data class HiroPlatformAlias(
    val desktopType: String,
    val androidType: String,
    val bridgeType: String,
    val bridgeFinisher: String,
    val memberBridges: Map<String, String>,
) {
    val desktopDescriptor = "L$desktopType;"

    val androidDescriptor = "L$androidType;"
}

internal object HiroPlatformAliases {
    val all = listOf(
        // SavedState：桌面为包着 Map 的真类，安卓为 Bundle。其构造器与 map 经 inline 的 savedState {} 构建器漏进调用方
        HiroPlatformAlias(
            desktopType = "androidx/savedstate/SavedState",
            androidType = "android/os/Bundle",
            bridgeType = "me/earzuchan/hiro/compose/internal/compat/HiroSavedStateBridge",
            bridgeFinisher = "toSavedState",
            memberBridges = mapOf("getMap()Ljava/util/Map;" to "readMap"),
        ),
    )

    val byDesktopType = all.associateBy { it.desktopType }
}
