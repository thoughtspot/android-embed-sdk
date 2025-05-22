# ThoughtSpot Android Embed SDK Usage Guide

## Overview

The `android-embed-sdk` allows developers to embed ThoughtSpot liveboards seamlessly into Android applications with support for custom styles, authentication, and event handling.

---

## 📦 Dependency Setup

You can use the SDK in two ways:

### 1. JitPack (for snapshots or GitHub releases)

In your **root `build.gradle`**:

```gradle
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```

In your **module `build.gradle`**:

```gradle
	dependencies {
	        implementation 'com.github.thoughtspot:android-embed-sdk:Tag'
	}
```

### 2. Maven Central (for stable releases)

In your **module `build.gradle`**:

```gradle
    dependencies {
        implementation("io.github.thoughtspot:android-embed-sdk:<version>")
    }
```

---

## 🔧 Basic Integration Example

```kotlin
package com.your-id.prj-name

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.thoughtspot.android_embed_sdk.LiveboardEmbed
import com.thoughtspot.android_embed_sdk.AuthType
import com.thoughtspot.android_embed_sdk.EmbedEvent
import com.thoughtspot.android_embed_sdk.HostEvent
import customCssInterface
import kotlinx.coroutines.*
import CustomStyles
import CustomisationsInterface
import EmbedConfig
import LiveboardViewConfig

class MainActivity : AppCompatActivity() {
    // You can instead fetch token from your backend.
    private suspend fun fetchAuthTokenAsync(): String {
        return "<YOUR_TRUSTED_AUTH_TOKEN>"
    }

    private val getAuthToken: () -> String = {
        runBlocking {
            fetchAuthTokenAsync()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val embedView = findViewById<LiveboardEmbed>(R.id.liveboard_embed_view)

        val viewConfig = LiveboardViewConfig(
            liveboardId = "your-livebaord-id",
            enable2ColumnLayout = true,
            customizations = CustomisationsInterface(
                style = CustomStyles(
                    customCSS = customCssInterface(
                        variables = mapOf(
                            "--ts-var-primary-color" to "#0055ff",
                            "--ts-var-max-width" to "1200px",
                            "--ts-var-enable-2-column-layout" to "true",
                            "--ts-var-root-background" to "#fef4dd",
                            "--ts-var-root-color" to "#4a4a4a",
                            "--ts-var-viz-title-color" to "#8e6b23",
                            "--ts-var-viz-title-font-family" to "'Georgia', 'Times New Roman', serif",
                            "--ts-var-viz-title-text-transform" to "capitalize",
                            "--ts-var-viz-description-color" to "#6b705c",
                            "--ts-var-viz-description-font-family" to "'Verdana', 'Helvetica', sans-serif",
                            "--ts-var-viz-border-radius" to "6px",
                            "--ts-var-viz-box-shadow" to "0 3px 6px rgba(0, 0, 0, 0.15)",
                            "--ts-var-viz-background" to "#fffbf0",
                            "--ts-var-viz-legend-hover-background" to "#ffe4b5",
                            "--ts-var-liveboard-dual-column-breakpoint" to "1100px",
                            "--ts-var-liveboard-single-column-breakpoint" to "320px"
                        )
                    )
                )
            )
        )

        val embedConfig = EmbedConfig(
            thoughtSpotHost = "ts-host",
            authType = AuthType.TrustedAuthTokenCookieless
        )

        embedView.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig,
            getAuthToken = getAuthToken
        )

        // Example: Listen to Reload event
        embedView.getController()?.on(HostEvent.Reload) { payload ->
            println("♻️ Liveboard reloaded with payload: $payload")
        }
    }
}
```

---

## 📌 Notes

* `LiveboardEmbed` view should be defined in your XML layout (`activity_main.xml`).
* Your backend should generate and return a **Trusted Auth Token**.
* Snapshot builds may change frequently; prefer stable versions for production.

---

## 🆘 Support

Please open an issue or discussion on the [GitHub repository](https://github.com/thoughtspot/android-embed-sdk) for support or feature requests.

---

© ThoughtSpot, Inc.
