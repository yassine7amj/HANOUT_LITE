# التطبيق ما كيستعملش تصغير الكود (isMinifyEnabled = false)،
# ولكن كنخلّيو هاد الملف باش البناء ما يفشلش ونتحكمو فالمستقبل.

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class ma.hanout.lite.data.** {
    *** Companion;
}
-keepclasseswithmembers class ma.hanout.lite.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
