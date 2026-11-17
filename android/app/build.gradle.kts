// HANOUT LITE — تطبيق الزبون (Kotlin + Jetpack Compose)
// ------------------------------------------------------------------
// كل شي خاص بالحانوت (الاسم، slug، إعدادات Firebase) كيتعطى وقت البناء
// عبر -P أو متغيرات البيئة، وراجع الـ workflow:
//   .github/workflows/android-kotlin-apk.yml
//
// أمثلة محلية:
//   gradle assembleRelease -Pshop=hanout-nakhil -PappLabel="حانوت النخيل"
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/** كيقرا قيمة البناء: -P أولا، من بعد متغير البيئة، من بعد القيمة الافتراضية. */
fun buildValue(name: String, fallback: String = ""): String =
    (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: fallback

val appLabel = buildValue("appLabel", "HANOUT LITE")
val shopSlug = buildValue("shop", "")
val fbProjectId = buildValue("fbProjectId", "")
val fbApiKey = buildValue("fbApiKey", "")
val webBase = buildValue("webBase", "https://yassine7amj.github.io/HANOUT_LITE")
val appPackage = buildValue("appPackage", "ma.hanout.lite")

/* نوع النسخة: customer (تطبيق الزبون) ولا merchant (تطبيق التاجر).
   كنزيدو لاحقة .merchant باش الجوج يثبّتو فمرة وحدة فحال الجوج. */
val appRole = buildValue("role", "customer").lowercase().let {
    if (it == "merchant") "merchant" else "customer"
}
val applicationIdValue =
    if (appRole == "merchant" && !appPackage.endsWith(".merchant")) "$appPackage.merchant"
    else appPackage

/* مفتاح التوقيع: كيتعطى من الـ workflow كمتغيرات بيئة (اختياري).
   بلا مفتاح، التطبيق كيتبنى غير موقّع المفعول (صالح للتجريب). */
val keystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val keystorePassword = System.getenv("KEYSTORE_PASSWORD").orEmpty()
// ملاحظة: سميّة هاد المتغير خاصها تكون مختلفة على اسم الخاصية ديال
// SigningConfig (keyPassword) باش ما يوقعلناش self-assignment ونصيفطو null.
val signingKeyPassword = System.getenv("KEY_PASSWORD").orEmpty().ifBlank { keystorePassword }
val keyAliasName = System.getenv("KEY_ALIAS").orEmpty().ifBlank { "android" }
val hasKeystore = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

android {
    namespace = "ma.hanout.lite"
    compileSdk = 35

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = 26
        targetSdk = 35
        versionCode = buildValue("versionCode", "1").toIntOrNull() ?: 1
        versionName = buildValue("versionName", "1.0")

        manifestPlaceholders["appLabel"] = appLabel

        buildConfigField("String", "APP_ROLE", "\"$appRole\"")
        buildConfigField("String", "SHOP_SLUG", "\"$shopSlug\"")
        buildConfigField("String", "FB_PROJECT_ID", "\"$fbProjectId\"")
        buildConfigField("String", "FB_API_KEY", "\"$fbApiKey\"")
        buildConfigField("String", "WEB_BASE", "\"$webBase\"")
    }

    if (hasKeystore) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
