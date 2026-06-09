package se.mildtanke.routesuit

sealed class Screen(val route: String) {
    data object Weather : Screen("weather")
    data object Settings : Screen("settings")
}

