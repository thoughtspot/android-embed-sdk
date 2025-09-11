package com.thoughtspot.android.embedsdk

import EmbedConfig
import EmbedEvent
import HostEvent
import LiveboardViewConfig
import android.content.Context
import org.json.JSONObject

/**
 * Handles the two-step shell handshake and all Host/Embed events.
 */
class LiveboardEmbedController(
    context: Context,
    /**
     * Your strongly-typed Liveboard settings
     */
    private val cfg: LiveboardViewConfig,
    /**
     * Your strongly-typed EmbedConfig (ThoughtSpot host, authType, etc.)
     */
    private val embedConfig: EmbedConfig,
    /**
     * Optional synchronous callback to fetch a fresh auth token
     */
    private val getAuthTokenCallback: (() -> String)? = null,
    /**
     * Called once the shell is ready and INIT has been sent
     */
    private val initializationCallback: (() -> Unit)? = null
) : BaseEmbedController(
    context             = context,
    embedConfig         = embedConfig,
    viewConfig          = SpecificViewConfig.Liveboard(cfg),
    embedType           = "Liveboard",
    getAuthTokenCallback    = getAuthTokenCallback,
    initializationCompletion = initializationCallback
) {

    /**
     * Register for embed events, e.g. LiveboardRendered, Error, etc.
     */
    fun on(event: EmbedEvent, callback: (String?) -> Unit) {
        super.on(event.value, callback)
    }

    /**
     * Remove a listener if you want
     */
    fun off(event: EmbedEvent) {
        super.off(event.value)
    }



    /**
     * Fire a HostEvent into the shell (search, drillDown, etc.)
     */
    override fun trigger(event: HostEvent, data: Any? ) {
        super.trigger(event, data)
    }
}
