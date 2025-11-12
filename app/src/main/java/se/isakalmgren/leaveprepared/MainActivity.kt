package se.isakalmgren.leaveprepared

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import se.isakalmgren.leaveprepared.ui.theme.LeavePreparedTheme
import android.content.Context
import android.content.res.Configuration

class MainActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationScheduler.scheduleDailyNotification(this)
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // Apply locale before creating the context
        val languageRepository = LanguageRepository(newBase)
        val locale = languageRepository.getCurrentLocale()
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            newBase.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            newBase.resources.updateConfiguration(config, newBase.resources.displayMetrics)
            newBase
        }
        
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationScheduler.scheduleDailyNotification(this)
        }
        
        setContent {
            LeavePreparedTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Weather.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(700))},
                        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(700))},
                        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(700))},
                        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(700))},
                    ) {
                        composable(Screen.Weather.route) {
                            WeatherScreen(
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}