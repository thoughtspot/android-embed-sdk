package com.thoughtspot.android.embedsdk

import android.content.Context
import android.webkit.WebView
import io.mockk.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.*

/**
 * Unit tests for LiveboardEmbedController
 */
@RunWith(RobolectricTestRunner::class)
class LiveboardEmbedControllerTest {

    private lateinit var context: Context
    private lateinit var embedConfig: EmbedConfig
    private lateinit var liveboardConfig: LiveboardViewConfig
    private lateinit var controller: LiveboardEmbedController
    private lateinit var mockWebView: WebView

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.TrustedAuthToken
        )

        liveboardConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-123",
            vizId = "test-viz-456"
        )

        mockWebView = mockk(relaxed = true)
        
        // Mock the WebView.post method to execute immediately for testing
        every { mockWebView.post(any()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
            true
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun testLiveboardEmbedControllerCreation() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )
        assertNotNull(controller)
    }

    @Test
    fun testLiveboardEmbedControllerWithCallbacks() {
        var authTokenCallbackInvoked = false
        var initCallbackInvoked = false

        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig,
            getAuthTokenCallback = { 
                authTokenCallbackInvoked = true
                "test-token" 
            },
            initializationCallback = { 
                initCallbackInvoked = true 
            }
        )

        assertNotNull(controller)
        // Callbacks should not be invoked yet
        assertFalse(authTokenCallbackInvoked)
        assertFalse(initCallbackInvoked)
    }

    @Test
    fun testOnEventWithEmbedEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var liveboardRenderedCalled = false
        var receivedData: String? = null

        controller.on(EmbedEvent.LiveboardRendered) { data ->
            liveboardRenderedCalled = true
            receivedData = data
        }

        controller.attachTo(mockWebView)

        // Simulate the event from JS - use the correct event name
        val jsBridge = controller.JSBridge()
        val eventMessage = JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "PinboardRendered")  // Use actual enum value
            put("data", "test-liveboard-data")
        }
        jsBridge.postMessage(eventMessage.toString())

        assertTrue(liveboardRenderedCalled)
        assertEquals("test-liveboard-data", receivedData)
    }

    @Test
    fun testOffEventWithEmbedEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var eventCalled = false
        controller.on(EmbedEvent.Load) { data ->
            eventCalled = true
        }

        // Remove the event listener
        controller.off(EmbedEvent.Load)

        controller.attachTo(mockWebView)

        // Simulate the event from JS
        val jsBridge = controller.JSBridge()
        val eventMessage = JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "load")
        }
        jsBridge.postMessage(eventMessage.toString())

        assertFalse(eventCalled)
    }

    @Test
    fun testTriggerWithHostEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)

        val searchData = mapOf("query" to "revenue")
        controller.trigger(HostEvent.Search, searchData)

        // Verify evaluateJavascript was called with HOST_EVENT
        verify { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("search") }, 
                any()
            ) 
        }
    }

    @Test
    fun testTriggerNavigateEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)

        val navigationData = mapOf("path" to "/liveboards")
        controller.trigger(HostEvent.Navigate, navigationData)

        verify(atLeast = 1) { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("Navigate") }, 
                any()
            ) 
        }
    }

    @Test
    fun testTriggerDrillDownEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)

        val drillDownData = mapOf("vizId" to "viz-123")
        controller.trigger(HostEvent.DrillDown, drillDownData)

        verify(atLeast = 1) { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("triggerDrillDown") }, 
                any()
            ) 
        }
    }

    @Test
    fun testMultipleEmbedEventListeners() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var initEventReceived = false
        var loadEventReceived = false
        var liveboardRenderedReceived = false

        controller.on(EmbedEvent.Init) { initEventReceived = true }
        controller.on(EmbedEvent.Load) { loadEventReceived = true }
        controller.on(EmbedEvent.LiveboardRendered) { liveboardRenderedReceived = true }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()

        // Trigger Init event
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "init")
        }.toString())

        // Trigger LiveboardRendered event - use actual enum value
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "PinboardRendered")
        }.toString())

        assertTrue(initEventReceived)
        assertFalse(loadEventReceived)
        assertTrue(liveboardRenderedReceived)
    }

    @Test
    fun testErrorEventHandling() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var errorReceived = false
        var errorData: String? = null

        controller.on(EmbedEvent.Error) { data ->
            errorReceived = true
            errorData = data
        }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "Error")  // Use actual enum value
            put("data", "Error loading liveboard")
        }.toString())

        assertTrue(errorReceived)
        assertEquals("Error loading liveboard", errorData)
    }

    @Test
    fun testDataEventHandling() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var dataReceived = false
        var dataPayload: String? = null

        controller.on(EmbedEvent.Data) { data ->
            dataReceived = true
            dataPayload = data
        }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "data")
            put("data", "{\"columns\": [], \"rows\": []}")
        }.toString())

        assertTrue(dataReceived)
        assertNotNull(dataPayload)
    }

    @Test
    fun testVizPointClickEventHandling() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var clickReceived = false
        var clickData: String? = null

        controller.on(EmbedEvent.VizPointClick) { data ->
            clickReceived = true
            clickData = data
        }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "vizPointClick")
            put("data", "{\"vizId\": \"viz-123\", \"point\": {}}")
        }.toString())

        assertTrue(clickReceived)
        assertNotNull(clickData)
    }

    @Test
    fun testTriggerFilterEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)

        val filterData = mapOf(
            "column" to "region",
            "value" to "west"
        )
        controller.trigger(HostEvent.Filter, filterData)

        verify { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("filter") }, 
                any()
            ) 
        }
    }

    @Test
    fun testTriggerReloadEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)

        controller.trigger(HostEvent.Reload)

        verify { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("reload") }, 
                any()
            ) 
        }
    }

    @Test
    fun testTriggerSetVisibleVizsEvent() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)

        val vizIds = listOf("viz-1", "viz-2", "viz-3")
        controller.trigger(HostEvent.SetVisibleVizs, vizIds)

        verify(atLeast = 1) { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("SetPinboardVisibleVizs") }, 
                any()
            ) 
        }
    }

    @Test
    fun testInheritedCleanupMethod() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)
        controller.cleanup()

        verify { mockWebView.loadUrl("about:blank") }
        verify { mockWebView.removeAllViews() }
        verify { mockWebView.destroy() }
    }

    @Test
    fun testInheritedLoadUrlMethod() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        controller.attachTo(mockWebView)
        controller.loadUrl("https://custom-url.com")

        verify(atLeast = 1) { mockWebView.loadUrl(any()) }
    }

    @Test
    fun testInitializationCallbackInvoked() {
        var initCallbackInvoked = false
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig,
            initializationCallback = { initCallbackInvoked = true }
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "INIT_VERCEL_SHELL")
        }.toString())

        assertTrue(initCallbackInvoked)
    }

    @Test
    fun testAuthTokenCallbackInvoked() {
        var authTokenRequested = false
        var tokenValue = ""
        
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig,
            getAuthTokenCallback = { 
                authTokenRequested = true
                tokenValue = "auth-token-abc123"
                tokenValue
            }
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "REQUEST_AUTH_TOKEN")
        }.toString())

        assertTrue(authTokenRequested)
        assertEquals("auth-token-abc123", tokenValue)
    }

    @Test
    fun testLiveboardConfigurationPassedCorrectly() {
        val customConfig = LiveboardViewConfig(
            liveboardId = "custom-lb-id",
            vizId = "custom-viz-id",
            fullHeight = true,
            hideLiveboardHeader = true,
            showLiveboardTitle = false
        )

        controller = LiveboardEmbedController(
            context = context,
            cfg = customConfig,
            embedConfig = embedConfig
        )

        assertNotNull(controller)
        // Controller should be created successfully with custom config
    }

    @Test
    fun testEmbedConfigWithDifferentAuthTypes() {
        val authTypes = listOf(
            AuthType.None,
            AuthType.TrustedAuthToken,
            AuthType.SSO,
            AuthType.SAML
        )

        authTypes.forEach { authType ->
            val config = EmbedConfig(
                thoughtSpotHost = "https://test.cloud",
                authType = authType
            )
            
            val testController = LiveboardEmbedController(
                context = context,
                cfg = liveboardConfig,
                embedConfig = config
            )
            
            assertNotNull(testController)
        }
    }

    @Test
    fun testAuthExpireEventHandling() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var authExpireReceived = false
        controller.on(EmbedEvent.AuthExpire) { 
            authExpireReceived = true
        }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "ThoughtspotAuthExpired")  // Use actual enum value
        }.toString())

        assertTrue(authExpireReceived)
    }

    @Test
    fun testAuthInitEventHandling() {
        controller = LiveboardEmbedController(
            context = context,
            cfg = liveboardConfig,
            embedConfig = embedConfig
        )

        var authInitReceived = false
        controller.on(EmbedEvent.AuthInit) { 
            authInitReceived = true
        }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "authInit")
        }.toString())

        assertTrue(authInitReceived)
    }
}

