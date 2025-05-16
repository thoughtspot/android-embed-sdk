package com.thoughtspot.android_embed_sdk

import EmbedConfig
import LiveboardViewConfig
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import android.widget.FrameLayout

/**
 * A simple FrameLayout wrapper that hosts the WebView and
 * wires up a LiveboardEmbedController for you.
 */
class LiveboardEmbed @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val webView: WebView = WebView(context).also {
        it.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
    }
    private var controller: LiveboardEmbedController? = null

    init {
        // add the WebView into this layout
        addView(webView)
    }

    /**
     * Call from your Activity/Fragment after inflation:
     *
     *   liveboardEmbedView.initialize(
     *       viewConfig       = LiveboardViewConfig(...),
     *       embedConfig      = EmbedConfig(...),
     *       getAuthToken     = { /* your token */ },
     *       onInit           = { /* shell ready */ }
     *   )
     */
    fun initialize(
        viewConfig: LiveboardViewConfig,
        embedConfig: EmbedConfig,
        getAuthToken: (() -> String)? = null,
        onInit: (() -> Unit)?        = null
    ) {
        controller = LiveboardEmbedController(
            context,
            viewConfig,
            embedConfig,
            getAuthToken,
            onInit
        )
        controller!!.attachTo(webView)
//        controller!!.loadUrl("https://mobile-embed-shell.vercel.app")
    }

    /**
     * Expose controller so host app can register listeners or trigger events.
     */
    fun getController(): LiveboardEmbedController? = controller
}
