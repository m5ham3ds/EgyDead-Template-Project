plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// 🔴 هذا هو السطر الوحيد الذي تقوم بتغييره عند إنشاء إضافة جديدة
val siteName = "egydead"

android {
    namespace = "com.aistudio.cinestream.xyzabc.extension.egydead"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aistudio.cinestream.xyzabc.extension.egydead"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        // ✅ هذه هي الطريقة الصحيحة لتغيير اسم ملف APK النهائي
        archivesBaseName = "${siteName.substring(0, 1).uppercase() + siteName.substring(1)}-Extension"
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

    // ❌ تم إزالة كتلة applicationVariants.all بالكامل لأنها تسبب التعارض
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    // لا نضع أي مكتبات واجهات (UI) لأن الإضافة تعمل في الخلفية فقط
}
