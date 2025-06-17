package com.thoughtspot.android.embedsdk

import EmbedConfig
import HostEvent
import LiveboardViewConfig
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import org.json.JSONObject
import org.json.JSONArray

sealed class SpecificViewConfig {
    data class Liveboard(val cfg: LiveboardViewConfig): SpecificViewConfig()
    // other configs
}

open class BaseEmbedController(
    private val context: Context,
    private val embedConfig: EmbedConfig,
    private val viewConfig: SpecificViewConfig,
    private val embedType: String,
    private val getAuthTokenCallback: (() -> String)? = null,
    private val initializationCompletion: (() -> Unit)? = null
) {
    private lateinit var webView: WebView
    private val eventListeners = mutableMapOf<String, (String?) -> Unit>()
    private val gson = Gson()
    private val shellUrl = "https://mobile-embed-shell.vercel.app"

    fun attachTo(webView: WebView) {
        this.webView = webView
        setupWebView()
        webView.post {
            webView.loadUrl(shellUrl)
        }
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // nothing here – we wait for the shell to post INIT_VERCEL_SHELL
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(JSBridge(), "ReactNativeWebView")

        // inject a small shim so that window.postMessage(...) in the page
        // routes into our ReactNativeWebview.postMessage:
        webView.evaluateJavascript("""
              (function() {
                const original = window.postMessage;
                window.postMessage = function(msg) {
                  ReactNativeWebView.postMessage(JSON.stringify(msg));
                  original.call(window, msg, "*");
                };
              })()
        """.trimIndent(), null)
    }

    /**
     * Register to receive embed‐events emitted from the shell:
     * e.g. "EMBED_EVENT" with { eventName, data }
     */
    open fun on(eventName: String, callback: (String?) -> Unit) {
        eventListeners[eventName] = callback
    }

    /**
     * Removes embedEvent
     * e.g. "EMBED_EVENT" with { eventName, data }
     */
    open fun off(eventName: String) {
        eventListeners.remove(eventName)
    }

    /**
     * This class is the JS → Android bridge.
     * All window.postMessage calls in the shell will arrive here.
     */
    inner class JSBridge {
        @JavascriptInterface
        fun postMessage(message: String) {
            try {
                val json = JSONObject(message)
                when (val type = json.getString("type")) {
                    "INIT_VERCEL_SHELL" -> handleInitVercelShell()
                    "REQUEST_AUTH_TOKEN" -> handleRequestAuthToken()
                    "EMBED_EVENT" -> {
                        val name = json.getString("eventName")
                        val data = json.optString("data", null)
                        eventListeners[name]?.invoke(data)
                    }
                    else -> Log.w("BaseEmbedController", "Unknown message type: $type")
                }
            } catch (e: Exception) {
//                Log.w("BaseEmbedController", "Failed to parse postMessage: $message", e)
            }
        }
    }

    open fun trigger(event: HostEvent, data: Any? = null) {
        val json = gson.toJson(mapOf(
            "type" to "HOST_EVENT",
            "eventName" to event.value,
            "payload" to data
        ))
        webView.post {
            webView.evaluateJavascript("window.postMessage($json, '*');", null)
        }
    }

    private val TAG = "BaseEmbedController"

    private fun handleInitVercelShell() {
        val authTypeValue = embedConfig.authType.value

        val initPayload = JSONObject(gson.toJson(embedConfig)).apply {
            put("getTokenFromSDK", true)
            put("authType", authTypeValue)
        }
        postToShell(mapOf(
            "type"    to "INIT",
            "payload" to initPayload
        ))

        val viewConfigJson   = when(viewConfig) {
            is SpecificViewConfig.Liveboard -> JSONObject(gson.toJson(viewConfig.cfg))
            // other type of embed
        }
        postToShell(mapOf(
            "type"      to "EMBED",
            "embedType" to embedType,
            "viewConfig" to viewConfigJson
        ))

        initializationCompletion?.invoke()
    }


    private fun handleRequestAuthToken() {
        getAuthTokenCallback?.let { getToken ->
            val token = try {
                getToken()
            } catch (e: Exception) {
                Log.e("BaseEmbedController", "Auth token fetch failed", e)
                null
            }
            postToShell(mapOf(
                "type" to "AUTH_TOKEN_RESPONSE",
                "token" to (token ?: "")
            ))
        }
    }

    /** Helper to JSON‐serialize Map→JSONObject and call window.postMessage */
    open fun postToShell(message: Map<String, Any>) {
        val json = JSONObject(message).toString()
        webView.post {
            webView.evaluateJavascript("window.postMessage($json, '*');") { _ -> }
        }
    }

    fun loadUrl(url: String) {
        webView.post {
            webView.loadUrl(url)
        }
    }

    /** Clean up the WebView when you’re done */
    fun cleanup() {
        webView.loadUrl("about:blank")
        webView.removeAllViews()
        webView.destroy()
    }
}
