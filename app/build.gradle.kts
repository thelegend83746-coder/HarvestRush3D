import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val gdxVersion = "1.12.1"
val natives by configurations.creating

android {
    namespace = "com.yt.harvestrush"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yt.harvestrush"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
            assets.srcDirs("src/main/assets")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
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
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // libGDX
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")

    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        file("src/main/jniLibs/armeabi-v7a").mkdirs()
        file("src/main/jniLibs/arm64-v8a").mkdirs()
        file("src/main/jniLibs/x86").mkdirs()
        file("src/main/jniLibs/x86_64").mkdirs()

        try {
            natives.files.forEach { jar ->
                var outputDir = file("src/main/jniLibs/armeabi-v7a")
                if (jar.name.contains("arm64-v8a")) outputDir = file("src/main/jniLibs/arm64-v8a")
                else if (jar.name.contains("armeabi-v7a")) outputDir = file("src/main/jniLibs/armeabi-v7a")
                else if (jar.name.contains("x86_64")) outputDir = file("src/main/jniLibs/x86_64")
                else if (jar.name.contains("x86")) outputDir = file("src/main/jniLibs/x86")

                zipTree(jar).matching {
                    include("*.so")
                }.forEach { soFile ->
                    val target = File(outputDir, soFile.name)
                    if (!target.exists() || target.length() != soFile.length()) {
                        soFile.copyTo(target, overwrite = true)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Native copy skipped: " + e.message)
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyAndroidNatives")
}
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn("copyAndroidNatives")
}
