import org.gradle.api.publish.maven.tasks.GenerateMavenPom

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
    signing
}

group = "io.github.thoughtspot"
version = "0.0.1-beta"

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from("src/main/java")
    from("src/main/kotlin")
}

// 2) Ensure POM generation waits for sourcesJar
tasks.withType<GenerateMavenPom>().configureEach {
    dependsOn(sourcesJar)
}

android {
    namespace = "com.thoughtspot.android.embedsdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
      testOptions {
        unitTests {
          isIncludeAndroidResources = true
        }
      }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    api("com.google.code.gson:gson:2.8.8")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.22")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId    = project.group.toString()
            artifactId = "android-embed-sdk"
            version    = project.version.toString()

            afterEvaluate { from(components["release"]) }


            pom {
                name.set("Android Embed SDK")
                description.set("ThoughtSpot Android Embed SDK")
                url.set("https://github.com/thoughtspot/android-embed-sdk")
                licenses {
                    license {
                        name.set("ThoughtSpot Development Tools EULA")
                        url.set("https://github.com/thoughtspot/android-embed-sdk/blob/main/LICENSE.md")
                    }
                }
                developers {
                    developer {
                        id.set("thoughtspot")
                        name.set("ThoughtSpot, Inc.")
                        email.set("support@thoughtspot.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/thoughtspot/android-embed-sdk.git")
                    developerConnection.set("scm:git:ssh://github.com/thoughtspot/android-embed-sdk.git")
                    url.set("https://github.com/thoughtspot/android-embed-sdk")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "OSSRH"
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = project.findProperty("ossrhUsername") as String?
                password = project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

tasks.named("generateMetadataFileForReleasePublication") {
    enabled = false
}

signing {
    val keyId = findProperty("signing.keyId") as String?
    val secretKeyPath = findProperty("signing.secretKeyFile") as String?
    val secretKey = secretKeyPath?.let { file(it).readText() }
    val password = findProperty("signing.password") as String?

    if (!keyId.isNullOrBlank() && !secretKey.isNullOrBlank() && !password.isNullOrBlank()) {
        useInMemoryPgpKeys(keyId, secretKey, password)
        sign(publishing.publications["release"])
    } else {
        logger.warn("Signing properties (keyId, secretKeyFile, password) not found or invalid. Skipping signing.")
    }
}

