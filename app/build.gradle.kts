import java.net.URL
import java.net.URI
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.OutputFile
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.stormbrowser.kytrqz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

abstract class DownloadAppIconTask : DefaultTask() {
    @get:Input
    abstract val urlString: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        outputs.upToDateWhen { outputFile.get().asFile.exists() }
    }

    @TaskAction
    fun download() {
        val baseToken = "AVvXsEj_FA4kHg1D6afKxwmVnlPQcg8epgljK"
        val remainingToken = "9cQv7uja07Yaq1RghGLJClag2tRVgahak1TOTV53pu7RXgl_ngFiOP_nR4ey0P-qzJgE39JQKon6FpwCLlH9p7DWJzFbsGeykJIY4GZ87k_UWpn7zKDrG5eOX4F10MCSh_Yf3Q3bv8aIbzR1AH1dB0xqfPEA"
        val variations = listOf("-", "--", "---", "_", "__", "hyphenhyphen", "", "-_")
        for (v in variations) {
            val candidateUrl = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/${baseToken}${v}${remainingToken}/s256/favicon-2.png"
            try {
                val url = URI(candidateUrl).toURL()
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val code = connection.responseCode
                println("Candidate variation '$v' returned status code: $code")
                if (code == 200) {
                    val destinationFile = outputFile.get().asFile
                    if (!destinationFile.parentFile.exists()) {
                        destinationFile.parentFile.mkdirs()
                    }
                    destinationFile.writeBytes(url.readBytes())
                    println("Successfully downloaded app icon using variation '$v' to ${destinationFile.absolutePath}!")
                    return
                }
            } catch (e: Exception) {
                println("Candidate variation '$v' threw error: $e")
            }
        }
        println("Error: None of the candidate variations for app icon succeeded.")
    }
}

tasks.register<DownloadAppIconTask>("downloadAppIcon") {
    urlString.set("https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEj_FA4kHg1D6afKxwmVnlPQcg8epgljKh--9cQv7uja07Yaq1RghGLJClag2tRVgahak1TOTV53pu7RXgl_ngFiOP_nR4ey0P-qzJgE39JQKon6FpwCLlH9p7DWJzFbsGeykJIY4GZ87k_UWpn7zKDrG5eOX4F10MCSh_Yf3Q3bv8aIbzR1AH1dB0xqfPEA/s256/favicon-2.png")
    outputFile.set(layout.projectDirectory.file("src/main/res/drawable/app_icon_custom.png"))
}

tasks.named("preBuild") {
    dependsOn("downloadAppIcon")
}
