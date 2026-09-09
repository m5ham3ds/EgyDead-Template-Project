plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 🔴 هذا هو السطر الوحيد الذي تقوم بتغييره عند إنشاء إضافة جديدة
val siteName = "egydead"

// ✅ تعيين اسم الملف النهائي على مستوى المشروع (هنا خارج android)
// هذه هي الطريقة الصحيحة في Kotlin DSL
project.archivesBaseName = "${siteName.substring(0, 1).uppercase() + siteName.substring(1)}-Extension"

android {
    namespace = "com.aistudio.cinestream.xyzabc.extension.egydead"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.cinestream.xyzabc.extension.egydead"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // ❌ لا نضع archivesBaseName هنا
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

    // ❌ تم إزالة كتلة applicationVariants.all بالكامل
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}
