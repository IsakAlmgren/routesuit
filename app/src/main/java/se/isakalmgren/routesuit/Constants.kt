package se.isakalmgren.routesuit

object Constants {
    // API Configuration
    const val API_BASE_URL = "https://opendata-download-metfcst.smhi.se/api/"
    
    // Network Timeouts (in seconds)
    const val CONNECT_TIMEOUT_SECONDS = 10L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 10L
    
    // Notification Configuration
    const val NOTIFICATION_DEFAULT_HOUR = 7
    const val NOTIFICATION_DEFAULT_MINUTE = 30
    const val NOTIFICATION_MIN_DELAY_MINUTES = 15L
    
    // Data Freshness
    const val STALE_DATA_THRESHOLD_HOURS = 1L
}

