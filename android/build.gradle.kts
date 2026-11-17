// HANOUT LITE — مشروع أندرويد الأصلي (Kotlin + Jetpack Compose)
// ------------------------------------------------------------------
// كيتنفّذ البناء على GitHub Actions (راجع .github/workflows/android-kotlin-apk.yml)
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}
