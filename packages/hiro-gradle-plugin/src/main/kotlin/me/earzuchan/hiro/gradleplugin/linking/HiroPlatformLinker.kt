package me.earzuchan.hiro.gradleplugin.linking

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper

/**
 * 按 [HiroPlatformAliases] 把类文件中的桌面平台类型重新链接到安卓
 *
 * 两步走：先把对桌面真类专属成员的访问改写到 Hiro 桥接类，再把其余所有类型引用（描述符、签名、类型指令等）整体改名为安卓类型。
 * 改写保持操作数栈形状不变（`new` 与 `dup` 原样保留、仅换类型），因此栈映射帧无需重算
 *
 * Kotlin 元数据不做改动：编译器按类名解析到安卓上的同名别名，再自行算出安卓描述符，与改写后的字节码一致
 */
internal object HiroPlatformLinker {
    private const val ASM_API = Opcodes.ASM9

    private val aliases = HiroPlatformAliases.byDesktopType

    @Suppress("DEPRECATION") // 带 api 参数的构造器较新，AGP 自带的 ASM 可能没有
    private val remapper = SimpleRemapper(aliases.mapValues { it.value.androidType })

    private val objectMethods = setOf("equals(Ljava/lang/Object;)Z", "hashCode()I", "toString()Ljava/lang/String;", "getClass()Ljava/lang/Class;", "notify()V", "notifyAll()V", "wait()V", "wait(J)V", "wait(JI)V")

    // 类文件中引用某类型，必经其描述符或常量池中的类名（UTF8 条目：标签 1、两字节长度、内容）
    private val needles: List<ByteArray> = HiroPlatformAliases.all.flatMap { alias ->
        val name = alias.desktopType.toByteArray(Charsets.UTF_8)
        listOf(alias.desktopDescriptor.toByteArray(Charsets.UTF_8), byteArrayOf(1, (name.size shr 8).toByte(), name.size.toByte()) + name)
    }

    fun needsLinking(classBytes: ByteArray) = needles.any { classBytes.contains(it) }

    /** @param where 出错时用于定位的类文件描述 */
    fun link(classBytes: ByteArray, where: String): ByteArray {
        val writer = ClassWriter(0)
        ClassReader(classBytes).accept(MemberBridgingClassVisitor(ClassRemapper(writer, remapper), where), 0)
        return writer.toByteArray()
    }

    private class MemberBridgingClassVisitor(next: ClassVisitor, private val where: String) : ClassVisitor(ASM_API, next) {
        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val next = super.visitMethod(access, name, descriptor, signature, exceptions) ?: return null
            return MemberBridgingMethodVisitor(next, "$where#$name$descriptor")
        }
    }

    private class MemberBridgingMethodVisitor(next: MethodVisitor, private val where: String) : MethodVisitor(ASM_API, next) {
        override fun visitTypeInsn(opcode: Int, type: String) {
            val alias = aliases[type]

            // new 桌面真类 → new 桥接类，随后的 dup 与构造器调用参数保持原样
            if (opcode == Opcodes.NEW && alias != null) super.visitTypeInsn(opcode, alias.bridgeType) else super.visitTypeInsn(opcode, type)
        }

        override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
            val alias = aliases[owner] ?: return super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

            if (opcode == Opcodes.INVOKESPECIAL && name == "<init>") {
                // 构造桥接类后立刻转为安卓类型，栈顶由桥接实例换成安卓实例，栈深不变
                super.visitMethodInsn(Opcodes.INVOKESPECIAL, alias.bridgeType, name, descriptor, false)
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, alias.bridgeType, alias.bridgeFinisher, "()${alias.desktopDescriptor}", false)
                return
            }

            val bridged = alias.memberBridges["$name$descriptor"]
            if (opcode == Opcodes.INVOKEVIRTUAL && bridged != null) {
                // 实例调用 → 以接收者为首参的静态调用，栈形状一致；描述符里的桌面类型随后由改名统一换掉
                super.visitMethodInsn(Opcodes.INVOKESTATIC, alias.bridgeType, bridged, "(${alias.desktopDescriptor}${descriptor.substring(1)}", false)
                return
            }

            // 继承自 Object 的方法在安卓类型上同样存在，交给改名即可
            if (opcode == Opcodes.INVOKEVIRTUAL && "$name$descriptor" in objectMethods) return super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)

            unsupported("$owner.$name$descriptor")
        }

        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            if (owner in aliases) unsupported("$owner.$name:$descriptor")

            super.visitFieldInsn(opcode, owner, name, descriptor)
        }

        override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any?) {
            (bootstrapMethodArguments.toList() + bootstrapMethodHandle).filterIsInstance<Handle>().firstOrNull { it.owner in aliases }?.let { unsupported("${it.owner}.${it.name}${it.desc}（方法句柄）") }

            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
        }

        override fun visitLdcInsn(value: Any?) {
            if (value is Handle && value.owner in aliases) unsupported("${value.owner}.${value.name}${value.desc}（方法句柄）")

            super.visitLdcInsn(value)
        }

        private fun unsupported(member: String): Nothing = throw IllegalStateException("Hiro Gradle 插件：$where 访问了桌面平台类型的专属成员 $member，尚无安卓桥接，请向 Hiro 反馈")
    }

    private fun ByteArray.contains(needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > size) return false

        outer@ for (start in 0..size - needle.size) {
            for (offset in needle.indices) if (this[start + offset] != needle[offset]) continue@outer
            return true
        }

        return false
    }
}
