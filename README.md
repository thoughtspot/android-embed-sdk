# 🧹 ThoughtSpot Android Embed SDK

Embed ThoughtSpot content directly into your native Android applications with ease using the **ThoughtSpot Android Embed SDK**.

---

## 📋 Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **minSdk** | 21 | 24+ |
| **compileSdk** | 33 | 34 |
| **targetSdk** | 33 | 34 |
| **Android Gradle Plugin** | 8.0.0 | 8.2.0+ |
| **Gradle** | 8.0 | 8.2+ |
| **Kotlin** | 1.9.0 | 1.9.24 |

---

## 📦 Installation

### Gradle (Maven Central)

**1. Ensure Maven Central is in your `settings.gradle.kts`:**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

**2. Add the dependency in your app's `build.gradle.kts`:**

```kotlin
implementation("com.thoughtspot:android-embed-sdk:1.1.0")
```

**3. Add Internet permission in your `AndroidManifest.xml`:**

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 🚀 Usage

### 1. XML Layout

```xml
<com.thoughtspot.android.embedsdk.LiveboardEmbed
    android:id="@+id/liveboard_embed_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintBottom_toTopOf="@id/another_view"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

### 2. Kotlin Integration

```kotlin
val embedView = findViewById<LiveboardEmbed>(R.id.liveboard_embed_view)

val viewConfig = LiveboardViewConfig(
    liveboardId = "your-liveboard-id",
    enable2ColumnLayout = true,
    customizations = CustomisationsInterface(
        style = CustomStyles(
            customCSS = customCssInterface(
                variables = mapOf(
                    "--ts-var-primary-color" to "#0055ff",
                    "--ts-var-liveboard-dual-column-breakpoint" to "1100px"
                    // Add more variables as needed
                )
            )
        )
    )
)

val embedConfig = EmbedConfig(
    thoughtSpotHost = "https://your.thoughtspot.instance",
    authType = AuthType.TrustedAuthTokenCookieless
)

val getAuthToken: () -> String = {
    runBlocking {
        // Replace with real token retrieval logic
        "your-auth-token"
    }
}

embedView.initialize(
    viewConfig = viewConfig,
    embedConfig = embedConfig,
    getAuthToken = getAuthToken
)
```

---

## 🧠 Event Handling

### Listen to SDK Events

```kotlin
embedView.getController()?.on(EmbedEvent.AuthInit) { payload ->
    println("✅ Auth initialized: $payload")
}
```

### Trigger Host Events

```kotlin
embedView.getController()?.trigger(HostEvent.Reload)
```

---

## 🔧 Customization

Easily style your embed via CSS variables passed in `customCSS`:

```kotlin
customCssInterface(
    variables = mapOf(
        "--ts-var-primary-color" to "#0055ff",
        "--ts-var-root-background" to "#ffffff"
        // Add any ThoughtSpot CSS variables
    )
)
```

---

## 💪 Testing

Includes support for:

* Unit tests (`JUnit`, `Mockito`)
* Android instrumented tests (`Espresso`, `AndroidX Test`)

---

## 📜 License

[ThoughtSpot Development Tools EULA](https://github.com/thoughtspot/android-embed-sdk/blob/main/LICENSE.md)

---

## 💠 Development & Publishing

Uses:

* Kotlin DSL for Gradle
* Maven publishing with signed artifacts
* Sources JAR included

See `build.gradle.kts` for full configuration.

---

## 👤 Maintainers

* ThoughtSpot, Inc. – [support@thoughtspot.com](mailto:support@thoughtspot.com)

---

## 🔗 Resources

* [ThoughtSpot Developers](https://developers.thoughtspot.com)
* [Liveboard Embedding Docs](https://developers.thoughtspot.com/docs/embed/liveboard)
