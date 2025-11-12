package se.isakalmgren.leaveprepared

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

enum class AppLanguage(val code: String) {
    AUTO("auto"),
    ENGLISH("en"),
    SWEDISH("sv");
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: AUTO
        }
    }
}

class LanguageRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "app_preferences",
        Context.MODE_PRIVATE
    )
    
    private val languageKey = "app_language"
    
    fun getSelectedLanguage(): AppLanguage {
        val code = prefs.getString(languageKey, AppLanguage.AUTO.code) ?: AppLanguage.AUTO.code
        return AppLanguage.fromCode(code)
    }
    
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(languageKey, language.code).apply()
    }
    
    fun getCurrentLocale(): Locale {
        val selectedLanguage = getSelectedLanguage()
        return when (selectedLanguage) {
            AppLanguage.AUTO -> {
                // Use system locale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale
                }
            }
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.SWEDISH -> Locale("sv", "SE")
        }
    }
    
    fun getCurrentLanguageCode(): String {
        return getCurrentLocale().language
    }
    
    fun applyLocale(context: Context) {
        val locale = getCurrentLocale()
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}

