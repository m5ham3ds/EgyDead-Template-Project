plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
}

// ✅ ✅ ✅ الطريقة السحرية لتغيير اسم الملف النهائي بدون أي تعارض
tasks.whenTaskAdded {
    if (name == "assembleRelease") {
        doLast {
            val sourceFile = file("build/outputs/apk/release/app-release.apk")
            val targetFile = file("build/outputs/apk/release/${siteName.substring(0, 1).uppercase() + siteName.substring(1)}-Extension-Release.apk")
            if (sourceFile.exists()) {
                sourceFile.renameTo(targetFile)
                println("✅ تم تغيير اسم الملف إلى: ${targetFile.name}")
            } else {
                println("❌ الملف المصدر غير موجود: ${sourceFile.absolutePath}")
            }
        }
    }
}
