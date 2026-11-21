package com.winhub.hernandez.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import com.winhub.hernandez.R

/**
 * Localization entry point that works without a Context.
 *
 * The app's UI text used to be hardcoded English literals scattered across Compose screens,
 * activities and utility objects. This object lets every one of those call sites read the same
 * string resource — `I18n.s(R.string.ui_something)` — whether or not a Context is at hand.
 *
 * [init] runs from [com.winhub.hernandez.BannerlatorApp.onCreate], which is before any activity,
 * service or Compose composition, so [s] never sees a null context in practice. If it somehow
 * does (a background process that skips Application), the reflected ActivityThread handle keeps
 * text working instead of blanking the UI.
 */
object I18n {

    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var appContext: Context? = null

    /** Called once from Application.onCreate. Safe to call repeatedly. */
    @JvmStatic
    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun context(): Context? {
        appContext?.let { return it }
        val viaReflection = runCatching {
            val activityThread = Class.forName("android.app.ActivityThread")
            val app = activityThread.getMethod("currentApplication").invoke(null) as? Context
            val ctx = app?.applicationContext
            if (ctx != null) appContext = ctx
            ctx
        }.getOrNull()
        return viaReflection ?: appContext
    }

    /** Resolves a string resource. Use instead of `context.getString(...)` where no Context is available. */
    @JvmStatic
    fun s(@StringRes id: Int): String = context()?.getString(id) ?: ""

    /** Resolves a formatted string resource. */
    @JvmStatic
    fun s(@StringRes id: Int, vararg args: Any?): String = context()?.getString(id, *args) ?: ""
}
