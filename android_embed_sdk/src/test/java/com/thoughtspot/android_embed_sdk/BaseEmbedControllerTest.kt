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
 * Unit tests for BaseEmbedController
 */
@RunWith(RobolectricTestRunner::class)
class BaseEmbedControllerTest {

    private lateinit var context: Context
    private lateinit var embedConfig: EmbedConfig
    private lateinit var viewConfig: SpecificViewConfig.Liveboard
    private lateinit var controller: BaseEmbedController
    private lateinit var mockWebView: WebView
    private var authTokenCallbackInvoked = false
    private var initCompletionCallbackInvoked = false

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        
        embedConfig = EmbedConfig(
            thoughtSpotHost = "https://test.thoughtspot.cloud",
            authType = AuthType.TrustedAuthToken
        )

        val liveboardConfig = LiveboardViewConfig(
            liveboardId = "test-liveboard-123"
        )
        viewConfig = SpecificViewConfig.Liveboard(liveboardConfig)

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
    fun testControllerCreation() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )
        assertNotNull(controller)
    }

    @Test
    fun testControllerCreationWithCallbacks() {
        var tokenCallbackInvoked = false
        var initCallbackInvoked = false

        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard",
            getAuthTokenCallback = { 
                tokenCallbackInvoked = true
                "test-token" 
            },
            initializationCompletion = { 
                initCallbackInvoked = true 
            }
        )

        assertNotNull(controller)
        // Callbacks should not be invoked yet
        assertFalse(tokenCallbackInvoked)
        assertFalse(initCallbackInvoked)
    }

    @Test
    fun testAttachToWebView() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)

        // Verify WebView settings were configured
        verify { mockWebView.settings }
        verify { mockWebView.loadUrl(any()) }
    }

    @Test
    fun testEventListenerRegistration() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        var eventReceived = false
        controller.on("testEvent") { data ->
            eventReceived = true
        }

        // Attach to WebView to setup JSBridge
        controller.attachTo(mockWebView)

        // Simulate event from JS
        val jsBridge = controller.JSBridge()
        val eventMessage = JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "testEvent")
            put("data", "test-data")
        }
        jsBridge.postMessage(eventMessage.toString())

        assertTrue(eventReceived)
    }

    @Test
    fun testEventListenerRemoval() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        var eventReceived = false
        controller.on("testEvent") { data ->
            eventReceived = true
        }

        // Remove the listener
        controller.off("testEvent")

        // Attach to WebView to setup JSBridge
        controller.attachTo(mockWebView)

        // Simulate event from JS
        val jsBridge = controller.JSBridge()
        val eventMessage = JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "testEvent")
            put("data", "test-data")
        }
        jsBridge.postMessage(eventMessage.toString())

        assertFalse(eventReceived)
    }

    @Test
    fun testJSBridgePostMessageWithInitVercelShell() {
        var initCallbackInvoked = false
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard",
            initializationCompletion = { initCallbackInvoked = true }
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        val message = JSONObject().apply {
            put("type", "INIT_VERCEL_SHELL")
        }
        jsBridge.postMessage(message.toString())

        // Verify initialization completion was called
        assertTrue(initCallbackInvoked)
        
        // Verify postMessage was called to send INIT and EMBED
        verify(atLeast = 3) { mockWebView.evaluateJavascript(any(), any()) }
    }

    @Test
    fun testJSBridgePostMessageWithRequestAuthToken() {
        var authTokenRequested = false
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard",
            getAuthTokenCallback = { 
                authTokenRequested = true
                "test-auth-token-123" 
            }
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        val message = JSONObject().apply {
            put("type", "REQUEST_AUTH_TOKEN")
        }
        jsBridge.postMessage(message.toString())

        assertTrue(authTokenRequested)
        // Verify that AUTH_TOKEN_RESPONSE was sent
        verify { mockWebView.evaluateJavascript(match { it.contains("AUTH_TOKEN_RESPONSE") }, any()) }
    }

    @Test
    fun testJSBridgePostMessageWithEmbedEvent() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        var receivedEventName: String? = null
        var receivedData: String? = null

        controller.on("customEvent") { data ->
            receivedData = data
            receivedEventName = "customEvent"
        }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        val message = JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "customEvent")
            put("data", "event-payload")
        }
        jsBridge.postMessage(message.toString())

        assertEquals("customEvent", receivedEventName)
        assertEquals("event-payload", receivedData)
    }

    @Test
    fun testJSBridgePostMessageWithInvalidJSON() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        
        // Should not crash with invalid JSON
        jsBridge.postMessage("invalid json {{{")
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }

    @Test
    fun testJSBridgePostMessageWithUnknownType() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        val message = JSONObject().apply {
            put("type", "UNKNOWN_TYPE")
        }
        
        // Should not crash with unknown type
        jsBridge.postMessage(message.toString())
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }

    @Test
    fun testTriggerHostEvent() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)

        val eventData = mapOf("key" to "value")
        controller.trigger(HostEvent.Search, eventData)

        // Verify evaluateJavascript was called with HOST_EVENT
        verify { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("search") }, 
                any()
            ) 
        }
    }

    @Test
    fun testTriggerHostEventWithoutData() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)

        controller.trigger(HostEvent.Navigate)

        // Verify evaluateJavascript was called with HOST_EVENT and Navigate
        verify(atLeast = 1) { 
            mockWebView.evaluateJavascript(
                match { it.contains("HOST_EVENT") && it.contains("Navigate") }, 
                any()
            ) 
        }
    }

    @Test
    fun testPostToShell() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)

        val message = mapOf(
            "type" to "TEST_MESSAGE",
            "data" to "test-data"
        )
        controller.postToShell(message)

        // Verify evaluateJavascript was called
        verify { 
            mockWebView.evaluateJavascript(
                match { it.contains("TEST_MESSAGE") }, 
                any()
            ) 
        }
    }

    @Test
    fun testLoadUrl() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)
        controller.loadUrl("https://example.com")

        verify(atLeast = 1) { mockWebView.loadUrl(any()) }
    }

    @Test
    fun testCleanup() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        controller.attachTo(mockWebView)
        controller.cleanup()

        verify { mockWebView.loadUrl("about:blank") }
        verify { mockWebView.removeAllViews() }
        verify { mockWebView.destroy() }
    }

    @Test
    fun testMultipleEventListeners() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard"
        )

        var event1Received = false
        var event2Received = false
        var event3Received = false

        controller.on("event1") { event1Received = true }
        controller.on("event2") { event2Received = true }
        controller.on("event3") { event3Received = true }

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        
        // Trigger event1
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "event1")
        }.toString())
        
        // Trigger event3
        jsBridge.postMessage(JSONObject().apply {
            put("type", "EMBED_EVENT")
            put("eventName", "event3")
        }.toString())

        assertTrue(event1Received)
        assertFalse(event2Received)
        assertTrue(event3Received)
    }

    @Test
    fun testAuthTokenCallbackException() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard",
            getAuthTokenCallback = { 
                throw RuntimeException("Token fetch failed")
            }
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        val message = JSONObject().apply {
            put("type", "REQUEST_AUTH_TOKEN")
        }
        
        // Should not crash even if callback throws exception
        jsBridge.postMessage(message.toString())
        
        // Should send empty token response
        verify { 
            mockWebView.evaluateJavascript(
                match { it.contains("AUTH_TOKEN_RESPONSE") }, 
                any()
            ) 
        }
    }

    @Test
    fun testSpecificViewConfigLiveboard() {
        val liveboardViewConfig = LiveboardViewConfig(
            liveboardId = "lb-123",
            vizId = "viz-456",
            fullHeight = true
        )
        
        val specificConfig = SpecificViewConfig.Liveboard(liveboardViewConfig)
        
        assertTrue(specificConfig is SpecificViewConfig.Liveboard)
        assertEquals("lb-123", specificConfig.cfg.liveboardId)
        assertEquals("viz-456", specificConfig.cfg.vizId)
        assertEquals(true, specificConfig.cfg.fullHeight)
    }

    @Test
    fun testAuthTokenCallbackNotProvided() {
        controller = BaseEmbedController(
            context = context,
            embedConfig = embedConfig,
            viewConfig = viewConfig,
            embedType = "Liveboard",
            getAuthTokenCallback = null
        )

        controller.attachTo(mockWebView)

        val jsBridge = controller.JSBridge()
        val message = JSONObject().apply {
            put("type", "REQUEST_AUTH_TOKEN")
        }
        
        // Should not crash when callback is not provided
        jsBridge.postMessage(message.toString())
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
}

