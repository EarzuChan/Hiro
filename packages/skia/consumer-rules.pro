# Hiro Skia 与本机库之间按固定名称连接的原生方法
-keepclasseswithmembernames,includedescriptorclasses class me.earzuchan.hiro.skia.HiroSkiaRuntime {
    native <methods>;
}
-keepclasseswithmembernames,includedescriptorclasses class org.jetbrains.skia.** {
    native <methods>;
}

# Minimal 本机库加载时主动初始化的 Skia 桥接类型
-keep,allowoptimization class org.jetbrains.skia.AnimationFrameInfo {
    <init>(...);
}
-keep,allowoptimization class org.jetbrains.skia.Color4f {
    <init>(float, float, float, float);
}
-keep,allowoptimization class org.jetbrains.skia.FontFamilyName {
    <init>(java.lang.String, java.lang.String);
}
-keep,allowoptimization class org.jetbrains.skia.FontFeature {
    <init>(java.lang.String, int);
    int _tag;
    int value;
    int start;
    int end;
}
-keep,allowoptimization class org.jetbrains.skia.FontMetrics {
    <init>(...);
}
-keep,allowoptimization class org.jetbrains.skia.FontVariation {
    <init>(int, float);
    int _tag;
    float value;
}
-keep,allowoptimization class org.jetbrains.skia.FontVariationAxis {
    <init>(int, float, float, float, boolean);
}
-keep,allowoptimization class org.jetbrains.skia.ImageInfo {
    <init>(int, int, int, int, long);
}
-keep,allowoptimization class org.jetbrains.skia.IPoint {
    <init>(int, int);
}
-keep,allowoptimization class org.jetbrains.skia.IRect {
    org.jetbrains.skia.IRect makeLTRB(int, int, int, int);
    int left;
    int top;
    int right;
    int bottom;
}
-keep,allowoptimization class org.jetbrains.skia.Path {
    <init>(long);
}
-keep,allowoptimization class org.jetbrains.skia.PathSegment {
    <init>(...);
}
-keep,allowoptimization class org.jetbrains.skia.Point {
    <init>(float, float);
    float x;
    float y;
}
-keep,allowoptimization class org.jetbrains.skia.Rect {
    org.jetbrains.skia.Rect makeLTRB(float, float, float, float);
    float left;
    float top;
    float right;
    float bottom;
}
-keep,allowoptimization class org.jetbrains.skia.RRect {
    org.jetbrains.skia.RRect makeLTRB(...);
    org.jetbrains.skia.RRect makeNinePatchLTRB(...);
    org.jetbrains.skia.RRect makeComplexLTRB(...);
    float left;
    float top;
    float right;
    float bottom;
    float[] radii;
}
-keep,allowoptimization class org.jetbrains.skia.RSXform {
    <init>(float, float, float, float);
}
-keep,allowoptimization class org.jetbrains.skia.impl.Native {
    long _ptr;
}

# Minimal 本机库加载时主动初始化的段落与 SVG 桥接类型
-keep,allowoptimization class org.jetbrains.skia.paragraph.LineMetrics {
    <init>(...);
}
-keep,allowoptimization class org.jetbrains.skia.paragraph.TextBox {
    <init>(float, float, float, float, int);
}
-keep,allowoptimization class org.jetbrains.skia.paragraph.DecorationStyle {
    <init>(boolean, boolean, boolean, boolean, int, int, float);
}
-keep,allowoptimization class org.jetbrains.skia.paragraph.Shadow {
    <init>(int, float, float, double);
}
-keep,allowoptimization class org.jetbrains.skia.svg.SVGLength {
    <init>(float, int);
}
-keep,allowoptimization class org.jetbrains.skia.svg.SVGPreserveAspectRatio {
    <init>(int, int);
}

# Minimal 本机库通过 JNI 调用 Kotlin 函数对象
-keep,allowoptimization interface kotlin.jvm.functions.Function0 {
    java.lang.Object invoke();
}
