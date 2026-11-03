package com.d100tific.filemusic.core.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Manages the app's chosen display language (English or Spanish), independent of the
 * system language. Backed by plain [android.content.SharedPreferences] instead of the
 * DataStore used for the rest of the settings because the language must be readable
 * synchronously from [android.app.Activity.attachBaseContext], before any coroutine can run.
 */
object LocaleManager {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"

    /** "en" and "es" are supported; "system" defers to the device locale. */
    const val LANG_SYSTEM = "system"
    const val LANG_EN = "en"
    const val LANG_ES = "es"

    /** Default language the very first time the app runs, per product requirements. */
    private const val DEFAULT_LANGUAGE = LANG_EN

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    /** Wraps [base] with a Configuration forced to the chosen language (or the system one). */
    fun wrapContext(base: Context): Context {
        val language = getLanguage(base)
        val locale = if (language == LANG_SYSTEM) {
            Configuration(base.resources.configuration).locales[0]
        } else {
            Locale.Builder().setLanguage(language).build()
        }
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }
}
