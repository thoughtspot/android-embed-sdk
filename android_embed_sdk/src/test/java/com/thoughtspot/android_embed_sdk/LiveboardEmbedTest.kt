package com.thoughtspot.android.embedsdk

import android.content.Context
import android.webkit.WebView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*

/**
 * Unit tests for LiveboardEmbed
 */
@RunWith(RobolectricTestRunner::class)
class LiveboardEmbedTest {

    private lateinit var context: Context
    private lateinit var liveboardEmbed: LiveboardEmbed

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        liveboardEmbed = LiveboardEmbed(context)
    }

    @Test
    fun testLiveboardEmbedCreation() {
        assertNotNull(liveboardEmbed)
    }

    @Test
    fun testLiveboardEmbedCreationWithAttributes() {
        val embedView = LiveboardEmbed(context, null)
        assertNotNull(embedView)
    }

    @Test
    fun testLiveboardEmbedInitialization() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-id"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.TrustedAuthToken
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedInitializationWithAuthTokenCallback() {
        var authTokenCallbackInvoked = false
        
        val viewConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-id"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.TrustedAuthToken
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig,
            getAuthToken = { 
                authTokenCallbackInvoked = true
                "test-token" 
            }
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
        // Callback should not be invoked during initialization
        assertFalse(authTokenCallbackInvoked)
    }

    @Test
    fun testLiveboardEmbedInitializationWithOnInitCallback() {
        var onInitCallbackInvoked = false
        
        val viewConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-id"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.TrustedAuthToken
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig,
            onInit = { 
                onInitCallbackInvoked = true
            }
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
        // Init callback should not be invoked during initialization (only when shell is ready)
        assertFalse(onInitCallbackInvoked)
    }

    @Test
    fun testLiveboardEmbedInitializationWithAllCallbacks() {
        var authTokenCallbackInvoked = false
        var onInitCallbackInvoked = false
        
        val viewConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-id",
            vizId = "test-viz-id"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.TrustedAuthToken
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig,
            getAuthToken = { 
                authTokenCallbackInvoked = true
                "test-token-123" 
            },
            onInit = { 
                onInitCallbackInvoked = true
            }
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testGetControllerBeforeInitialization() {
        val controller = liveboardEmbed.getController()
        assertNull(controller)
    }

    @Test
    fun testGetControllerAfterInitialization() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-id"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.None
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
        assertTrue(controller is LiveboardEmbedController)
    }

    @Test
    fun testLiveboardEmbedWithComplexViewConfig() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "complex-liveboard-id",
            vizId = "complex-viz-id",
            fullHeight = true,
            hideLiveboardHeader = true,
            showLiveboardTitle = false
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://complex.thoughtspot.cloud",
            authType = AuthType.SSO
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedWithBasicAuth() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "basic-auth-liveboard"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://basic.thoughtspot.cloud",
            authType = AuthType.Basic,
            username = "testuser"
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedWithNoAuth() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "no-auth-liveboard"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://noauth.thoughtspot.cloud",
            authType = AuthType.None
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedMultipleInitializations() {
        val viewConfig1 = LiveboardViewConfig(
            liveboardId = "first-liveboard"
        )
        
        val embedConfig1 = EmbedConfig(
            thoughtSpotHost = "https://first.thoughtspot.cloud",
            authType = AuthType.None
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig1,
            embedConfig = embedConfig1
        )

        val controller1 = liveboardEmbed.getController()
        assertNotNull(controller1)

        // Second initialization should replace the first
        val viewConfig2 = LiveboardViewConfig(
            liveboardId = "second-liveboard"
        )
        
        val embedConfig2 = EmbedConfig(
            thoughtSpotHost = "https://second.thoughtspot.cloud",
            authType = AuthType.SSO
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig2,
            embedConfig = embedConfig2
        )

        val controller2 = liveboardEmbed.getController()
        assertNotNull(controller2)
        // Controllers should be different instances
        assertNotSame(controller1, controller2)
    }

    @Test
    fun testLiveboardEmbedIsFrameLayout() {
        assertTrue(liveboardEmbed is android.widget.FrameLayout)
    }

    @Test
    fun testLiveboardEmbedChildCount() {
        // Should have one child (the WebView)
        assertEquals(1, liveboardEmbed.childCount)
    }

    @Test
    fun testLiveboardEmbedWebViewChild() {
        // First child should be a WebView
        val child = liveboardEmbed.getChildAt(0)
        assertTrue(child is WebView)
    }

    @Test
    fun testLiveboardEmbedWithMinimalConfig() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "minimal-id"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://minimal.cloud",
            authType = AuthType.None
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        assertNotNull(liveboardEmbed.getController())
    }

    @Test
    fun testLiveboardEmbedContextPreservation() {
        assertEquals(context, liveboardEmbed.context)
    }

    @Test
    fun testLiveboardEmbedWithSAMLAuth() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "saml-liveboard"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://saml.thoughtspot.cloud",
            authType = AuthType.SAML
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedWithOIDCAuth() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "oidc-liveboard"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://oidc.thoughtspot.cloud",
            authType = AuthType.OIDC
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedWithTrustedAuthTokenCookieless() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "cookieless-liveboard"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://cookieless.thoughtspot.cloud",
            authType = AuthType.TrustedAuthTokenCookieless
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig,
            getAuthToken = { "cookieless-token" }
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedInitializationPreservesCallbacks() {
        var tokenCallCount = 0
        var initCallCount = 0
        
        val viewConfig = LiveboardViewConfig(
            liveboardId = "callback-test"
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://callback.test",
            authType = AuthType.TrustedAuthToken
        )

        val tokenCallback = { 
            tokenCallCount++
            "token-$tokenCallCount"
        }

        val initCallback: () -> Unit = {
            initCallCount++
        }

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig,
            getAuthToken = tokenCallback,
            onInit = initCallback
        )

        assertNotNull(liveboardEmbed.getController())
        // Callbacks are stored but not invoked yet
        assertEquals(0, tokenCallCount)
        assertEquals(0, initCallCount)
    }

    @Test
    fun testLiveboardEmbedViewConfigWithAllOptions() {
        val viewConfig = LiveboardViewConfig(
            liveboardId = "full-config-liveboard",
            vizId = "viz-123",
            fullHeight = true,
            hideLiveboardHeader = false,
            showLiveboardTitle = true
        )
        
        val embedConfig = EmbedConfig(
            thoughtSpotHost = "https://full.config",
            authType = AuthType.None
        )

        liveboardEmbed.initialize(
            viewConfig = viewConfig,
            embedConfig = embedConfig
        )

        val controller = liveboardEmbed.getController()
        assertNotNull(controller)
        assertTrue(controller is LiveboardEmbedController)
    }
}

