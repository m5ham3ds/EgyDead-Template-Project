plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 🔴 هذا هو السطر الوحيد الذي تقوم بتغييره عند إنشاء إضافة جديدة (يجب أن يكون بحروف إنجليزية صغيرة بدون مسافات)
val siteName = "egydead"

android {
    // بناء اسم الحزمة الخاص بالإضافة تلقائياً ليكون مستقلاً عن التطبيق الأساسي
    namespace = "com.aistudio.cinestream.xyzabc.extension.egydead"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.cinestream.xyzabc.extension.egydead"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // كود ذكي لتغيير اسم ملف APK المخرج ليكون باسم الموقع تلقائياً
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
             output.outputFileName = "${siteName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}-Extension-Release.apk"
            }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    // لا نضع أي مكتبات واجهات (UI) لأن الإضافة تعمل في الخلفية فقط لتقليل الحجم.
}
