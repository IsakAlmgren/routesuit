package se.isakalmgren.routesuit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import se.isakalmgren.routesuit.ui.theme.RouteSuitTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val recommendations: CommuteRecommendations) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    apiService: SmhiApiService = koinInject(),
    configRepository: ConfigRepository = koinInject(),
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configState = configRepository.config.collectAsState()
    val appConfig = configState.value
    val context = LocalContext.current
    
    var uiState by remember { mutableStateOf<WeatherUiState>(WeatherUiState.Loading) }
    val coroutineScope = rememberCoroutineScope()
    
    fun fetchWeather() {
        coroutineScope.launch {
            uiState = WeatherUiState.Loading
            try {
                val lonStr = String.format(java.util.Locale.US, "%.4f", appConfig.longitude)
                val latStr = String.format(java.util.Locale.US, "%.3f", appConfig.latitude)
                val url = "https://opendata-download-metfcst.smhi.se/api/category/snow1g/version/1/geotype/point/lon/$lonStr/lat/$latStr/data.json"
                android.util.Log.d("WeatherScreen", "Attempting to fetch weather from URL: $url")
                android.util.Log.d("WeatherScreen", "Longitude: ${appConfig.longitude} -> $lonStr, Latitude: ${appConfig.latitude} -> $latStr")
                
                val response = apiService.getWeatherForecast(
                    longitude = lonStr,
                    latitude = latStr
                )
                val recommendations = analyzeWeatherForCommutes(response.timeSeries, appConfig, context)
                uiState = WeatherUiState.Success(recommendations)
            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.failed_to_fetch_weather, e.message ?: "")
                uiState = WeatherUiState.Error(errorMsg)
            }
        }
    }
    
    LaunchedEffect(appConfig) {
        fetchWeather()
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            WeatherTopAppBar(onSettingsClick = onSettingsClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is WeatherUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = stringResource(R.string.loading_weather_forecast),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                is WeatherUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        if (state.recommendations.morningCommute != null) {
                            WeatherRecommendationCard(
                                recommendation = state.recommendations.morningCommute,
                                title = stringResource(R.string.to_work)
                            )
                        }
                        
                        if (state.recommendations.eveningCommute != null) {
                            WeatherRecommendationCard(
                                recommendation = state.recommendations.eveningCommute,
                                title = stringResource(R.string.from_work)
                            )
                        }
                        
                        if (state.recommendations.morningCommute == null && state.recommendations.eveningCommute == null) {
                            NoCommuteDataCard()
                        }
                        
                        OutlinedButton(
                            onClick = { fetchWeather() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp)
                        ) {
                            Text(stringResource(R.string.refresh))
                        }
                    }
                }
                
                is WeatherUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        ErrorCard(
                            message = state.message,
                            onRetry = { fetchWeather() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherRecommendationCard(
    recommendation: WeatherRecommendation,
    title: String = ""
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title
            if (title.isNotEmpty()) {
                RecommendationTitle(title = title, dayLabel = recommendation.dayLabel)
            }
            
            // Temperature display
            Text(
                text = stringResource(R.string.temperature_format, recommendation.temperature),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayMedium
            )
            
            // Rain clothes recommendation
            RainRecommendationCard(recommendation = recommendation)
        }
    }
}

@Composable
private fun RecommendationTitle(title: String, dayLabel: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge
        )
        if (dayLabel.isNotEmpty()) {
            Text(
                text = dayLabel,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun RainRecommendationCard(recommendation: WeatherRecommendation) {
    if (recommendation.needsRainClothes) {
        InfoCard(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ) {
            Text(
                text = if (recommendation.rainForLater) {
                    stringResource(R.string.bring_rain_clothes_later)
                } else {
                    stringResource(R.string.bring_rain_clothes)
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (recommendation.rainForLater) {
                Text(
                    text = stringResource(R.string.rain_expected_later),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = stringResource(R.string.precipitation_probability, recommendation.precipitationProbability.toInt()),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (recommendation.precipitationAmount > 0) {
                    Text(
                        text = stringResource(R.string.expected_precipitation, recommendation.precipitationAmount),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    } else {
        InfoCard(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Text(
                text = stringResource(R.string.no_rain_expected),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = stringResource(R.string.skip_rain_gear),
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun InfoCard(
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherTopAppBar(onSettingsClick: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.content_description_app_icon),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .padding(start = 16.dp)
            )
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.content_description_settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.error),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun NoCommuteDataCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = stringResource(R.string.no_commute_data),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Preview composables
@Preview(showBackground = true, name = "Morning Commute - Rain Expected")
@Composable
fun WeatherRecommendationCardPreview_MorningRain() {
    RouteSuitTheme(darkTheme = true) {
        val context = LocalContext.current
        val appConfig = AppConfig()
        val recommendationBase = WeatherRecommendation(
            needsRainClothes = true,
            temperature = 8.5,
            precipitationProbability = 75.0,
            precipitationAmount = 2.3,
            message = "",
            timeWindow = "Morning Commute (7-9 AM)",
            rainForLater = false,
            dayLabel = "Tomorrow"
        )
        val recommendation = recommendationBase.copy(message = generateRecommendationMessage(recommendationBase, appConfig, context))
        WeatherRecommendationCard(
            recommendation = recommendation,
            title = "To Work"
        )
    }
}

@Preview(showBackground = true, name = "Morning Commute - Rain for Later")
@Composable
fun WeatherRecommendationCardPreview_MorningRainForLater() {
    RouteSuitTheme {
        val context = LocalContext.current
        val appConfig = AppConfig()
        val recommendationBase = WeatherRecommendation(
            needsRainClothes = true,
            temperature = 12.0,
            precipitationProbability = 80.0, // Evening commute's precipitation probability
            precipitationAmount = 1.5, // Evening commute's precipitation amount
            message = "",
            timeWindow = "Morning Commute (7-9 AM)",
            rainForLater = true,
            dayLabel = "Tomorrow"
        )
        val recommendation = recommendationBase.copy(message = generateRecommendationMessage(recommendationBase, appConfig, context))
        WeatherRecommendationCard(
            recommendation = recommendation,
            title = "To Work"
        )
    }
}

@Preview(showBackground = true, name = "Morning Commute - No Rain")
@Composable
fun WeatherRecommendationCardPreview_MorningNoRain() {
    RouteSuitTheme {
        val context = LocalContext.current
        val appConfig = AppConfig()
        val recommendationBase = WeatherRecommendation(
            needsRainClothes = false,
            temperature = 18.0,
            precipitationProbability = 10.0,
            precipitationAmount = 0.0,
            message = "",
            timeWindow = "Morning Commute (7-9 AM)",
            rainForLater = false,
            dayLabel = "Today"
        )
        val recommendation = recommendationBase.copy(message = generateRecommendationMessage(recommendationBase, appConfig, context))
        WeatherRecommendationCard(
            recommendation = recommendation,
            title = "To Work"
        )
    }
}

@Preview(showBackground = true, name = "Evening Commute - Rain")
@Composable
fun WeatherRecommendationCardPreview_EveningRain() {
    RouteSuitTheme {
        val context = LocalContext.current
        val appConfig = AppConfig()
        val recommendationBase = WeatherRecommendation(
            needsRainClothes = true,
            temperature = -2.0,
            precipitationProbability = 90.0,
            precipitationAmount = 5.0,
            message = "",
            timeWindow = "Evening Commute (4-7 PM)",
            rainForLater = false,
            dayLabel = "Today"
        )
        val recommendation = recommendationBase.copy(message = generateRecommendationMessage(recommendationBase, appConfig, context))
        WeatherRecommendationCard(
            recommendation = recommendation,
            title = "From Work"
        )
    }
}

@Preview(showBackground = true, name = "Evening Commute - No Rain")
@Composable
fun WeatherRecommendationCardPreview_EveningNoRain() {
    RouteSuitTheme {
        val context = LocalContext.current
        val appConfig = AppConfig()
        val recommendationBase = WeatherRecommendation(
            needsRainClothes = false,
            temperature = 25.0,
            precipitationProbability = 5.0,
            precipitationAmount = 0.0,
            message = "",
            timeWindow = "Evening Commute (4-7 PM)",
            rainForLater = false,
            dayLabel = "Today"
        )
        val recommendation = recommendationBase.copy(message = generateRecommendationMessage(recommendationBase, appConfig, context))
        WeatherRecommendationCard(
            recommendation = recommendation,
            title = "From Work"
        )
    }
}

@Preview(showBackground = true, name = "Success State - Both Commutes")
@Composable
fun WeatherScreenPreview_Success() {
    RouteSuitTheme {
        val context = LocalContext.current
        val appConfig = AppConfig()
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val morningRecommendationBase = WeatherRecommendation(
                    needsRainClothes = true,
                    temperature = 8.5,
                    precipitationProbability = 75.0,
                    precipitationAmount = 2.3,
                    message = "",
                    timeWindow = "Morning Commute (7-9 AM)",
                    rainForLater = false,
                    dayLabel = "Tomorrow"
                )
                val morningRecommendation = morningRecommendationBase.copy(message = generateRecommendationMessage(morningRecommendationBase, appConfig, context))
                
                val eveningRecommendationBase = WeatherRecommendation(
                    needsRainClothes = false,
                    temperature = 10.0,
                    precipitationProbability = 20.0,
                    precipitationAmount = 0.0,
                    message = "",
                    timeWindow = "Evening Commute (4-7 PM)",
                    rainForLater = false,
                    dayLabel = "Today"
                )
                val eveningRecommendation = eveningRecommendationBase.copy(message = generateRecommendationMessage(eveningRecommendationBase, appConfig, context))
                
                WeatherRecommendationCard(
                    recommendation = morningRecommendation,
                    title = "To Work"
                )
                
                WeatherRecommendationCard(
                    recommendation = eveningRecommendation,
                    title = "From Work"
                )
                
                Button(
                    onClick = { },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun WeatherScreenPreview_Loading() {
    RouteSuitTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("Loading weather forecast...")
            }
        }
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun WeatherScreenPreview_Error() {
    RouteSuitTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ErrorCard(
                    message = "Failed to fetch weather: Network error",
                    onRetry = { }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Top App Bar")
@Composable
fun WeatherTopAppBarPreview() {
    RouteSuitTheme {
        Scaffold(
            topBar = {
                WeatherTopAppBar(onSettingsClick = {})
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = "Content area",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }
    }
}

