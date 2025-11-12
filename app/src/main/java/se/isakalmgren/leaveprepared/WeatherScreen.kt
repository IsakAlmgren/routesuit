package se.isakalmgren.leaveprepared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import se.isakalmgren.leaveprepared.ui.theme.LeavePreparedTheme

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val recommendations: CommuteRecommendations) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

@Composable
fun WeatherScreen(
    apiService: SmhiApiService = koinInject(),
    configRepository: ConfigRepository = koinInject(),
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configState = configRepository.config.collectAsState()
    val appConfig = configState.value
    
    var uiState by remember { mutableStateOf<WeatherUiState>(WeatherUiState.Loading) }
    val coroutineScope = rememberCoroutineScope()
    
    fun fetchWeather() {
        coroutineScope.launch {
            uiState = WeatherUiState.Loading
            try {
                val response = apiService.getWeatherForecast()
                val recommendations = analyzeWeatherForCommutes(response.timeSeries, appConfig)
                uiState = WeatherUiState.Success(recommendations)
            } catch (e: Exception) {
                uiState = WeatherUiState.Error("Failed to fetch weather: ${e.message}")
            }
        }
    }
    
    LaunchedEffect(appConfig) {
        fetchWeather()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WeatherScreenHeader(onSettingsClick = onSettingsClick)
        
        when (val state = uiState) {
            is WeatherUiState.Loading -> {
                CircularProgressIndicator()
                Text("Loading weather forecast...")
            }
            
            is WeatherUiState.Success -> {
                if (state.recommendations.morningCommute != null) {
                    WeatherRecommendationCard(
                        recommendation = state.recommendations.morningCommute,
                        title = "🌅 To Work",
                        appConfig = appConfig
                    )
                }
                
                if (state.recommendations.eveningCommute != null) {
                    WeatherRecommendationCard(
                        recommendation = state.recommendations.eveningCommute,
                        title = "🌆 From Work",
                        appConfig = appConfig

                    )
                }
                
                if (state.recommendations.morningCommute == null && state.recommendations.eveningCommute == null) {
                    NoCommuteDataCard()
                }
                
                Button(
                    onClick = { fetchWeather() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Refresh")
                }
            }
            
            is WeatherUiState.Error -> {
                ErrorCard(
                    message = state.message,
                    onRetry = { fetchWeather() }
                )
            }
        }
    }
}

@Composable
fun WeatherRecommendationCard(
    recommendation: WeatherRecommendation,
    title: String = "",
    appConfig: AppConfig
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            if (title.isNotEmpty()) {
                RecommendationTitle(title = title, dayLabel = recommendation.dayLabel)
                Divider()
            }
            
            // Temperature display
            Text(
                text = "${String.format("%.1f", recommendation.temperature)}°C",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Clothing recommendation
            ClothingRecommendationCard(
                clothingMessage = getClothingMessage(recommendation.clothingLevel, appConfig)
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
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        if (dayLabel.isNotEmpty()) {
            Text(
                text = dayLabel,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ClothingRecommendationCard(clothingMessage: String) {
    InfoCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Text(
            text = "Clothing Recommendation",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = clothingMessage,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun RainRecommendationCard(recommendation: WeatherRecommendation) {
    if (recommendation.needsRainClothes) {
        InfoCard(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Text(
                text = if (recommendation.rainForLater) {
                    "🌧️ Bring Rain Clothes for Later!"
                } else {
                    "🌧️ Rain Clothes Needed!"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (recommendation.rainForLater) {
                Text(
                    text = "Rain expected on your way home",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Precipitation probability: ${recommendation.precipitationProbability.toInt()}%",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (recommendation.precipitationAmount > 0) {
                    Text(
                        text = "Expected: ${String.format("%.1f", recommendation.precipitationAmount)} mm",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    } else {
        InfoCard(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Text(
                text = "☀️ No Rain Expected",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "You can skip the rain gear today",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun InfoCard(
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun WeatherScreenHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Leave Prepared",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
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
                text = "Error",
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
                Text("Retry")
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
        )
    ) {
        Text(
            text = "No commute data available for today",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

// Preview composables
@Preview(showBackground = true, name = "Morning Commute - Rain Expected")
@Composable
fun WeatherRecommendationCardPreview_MorningRain() {
    LeavePreparedTheme {
        WeatherRecommendationCard(
            recommendation = WeatherRecommendation(
                needsRainClothes = true,
                clothingLevel = ClothingLevel.LEVEL_4,
                temperature = 8.5,
                precipitationProbability = 75.0,
                precipitationAmount = 2.3,
                message = "Warm clothing - sweater and jacket recommended\n🌧️ Bring rain clothes! Precipitation probability: 75% Expected precipitation: 2.3 mm",
                timeWindow = "Morning Commute (7-9 AM)",
                rainForLater = false,
                dayLabel = "Tomorrow"
            ),
            title = "🌅 To Work",
            appConfig = AppConfig()
        )
    }
}

@Preview(showBackground = true, name = "Morning Commute - Rain for Later")
@Composable
fun WeatherRecommendationCardPreview_MorningRainForLater() {
    LeavePreparedTheme {
        WeatherRecommendationCard(
            recommendation = WeatherRecommendation(
                needsRainClothes = true,
                clothingLevel = ClothingLevel.LEVEL_3,
                temperature = 12.0,
                precipitationProbability = 0.0,
                precipitationAmount = 0.0,
                message = "Moderate clothing - long sleeves and a light jacket\n🌧️ Bring rain clothes for later! Rain expected on your way home (80% chance, 1.5 mm)",
                timeWindow = "Morning Commute (7-9 AM)",
                rainForLater = true,
                dayLabel = "Tomorrow"
            ),
            title = "🌅 To Work",
            appConfig = AppConfig()
        )
    }
}

@Preview(showBackground = true, name = "Morning Commute - No Rain")
@Composable
fun WeatherRecommendationCardPreview_MorningNoRain() {
    LeavePreparedTheme {
        WeatherRecommendationCard(
            recommendation = WeatherRecommendation(
                needsRainClothes = false,
                clothingLevel = ClothingLevel.LEVEL_2,
                temperature = 18.0,
                precipitationProbability = 10.0,
                precipitationAmount = 0.0,
                message = "Light clothing - t-shirt with a light jacket\n☀️ No rain expected",
                timeWindow = "Morning Commute (7-9 AM)",
                rainForLater = false,
                dayLabel = "Today"
            ),
            title = "🌅 To Work",
            appConfig = AppConfig()
        )
    }
}

@Preview(showBackground = true, name = "Evening Commute - Rain")
@Composable
fun WeatherRecommendationCardPreview_EveningRain() {
    LeavePreparedTheme {
        WeatherRecommendationCard(
            recommendation = WeatherRecommendation(
                needsRainClothes = true,
                clothingLevel = ClothingLevel.LEVEL_6,
                temperature = -2.0,
                precipitationProbability = 90.0,
                precipitationAmount = 5.0,
                message = "Very cold - winter coat and warm layers essential\n🌧️ Bring rain clothes! Precipitation probability: 90% Expected precipitation: 5.0 mm",
                timeWindow = "Evening Commute (4-7 PM)",
                rainForLater = false,
                dayLabel = "Today"
            ),
            title = "🌆 From Work",
            appConfig = AppConfig()
        )
    }
}

@Preview(showBackground = true, name = "Evening Commute - No Rain")
@Composable
fun WeatherRecommendationCardPreview_EveningNoRain() {
    LeavePreparedTheme {
        WeatherRecommendationCard(
            recommendation = WeatherRecommendation(
                needsRainClothes = false,
                clothingLevel = ClothingLevel.LEVEL_1,
                temperature = 25.0,
                precipitationProbability = 5.0,
                precipitationAmount = 0.0,
                message = "Light clothing - shorts and t-shirt weather\n☀️ No rain expected",
                timeWindow = "Evening Commute (4-7 PM)",
                rainForLater = false,
                dayLabel = "Today"
            ),
            title = "🌆 From Work",
            appConfig = AppConfig()
        )
    }
}

@Preview(showBackground = true, name = "Success State - Both Commutes")
@Composable
fun WeatherScreenPreview_Success() {
    LeavePreparedTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WeatherScreenHeader(onSettingsClick = {})
                
                WeatherRecommendationCard(
                    recommendation = WeatherRecommendation(
                        needsRainClothes = true,
                        clothingLevel = ClothingLevel.LEVEL_4,
                        temperature = 8.5,
                        precipitationProbability = 75.0,
                        precipitationAmount = 2.3,
                        message = "Warm clothing - sweater and jacket recommended\n🌧️ Bring rain clothes! Precipitation probability: 75% Expected precipitation: 2.3 mm",
                        timeWindow = "Morning Commute (7-9 AM)",
                        rainForLater = false,
                        dayLabel = "Tomorrow"
                    ),
                    title = "🌅 To Work",
                    appConfig = AppConfig()
                )
                
                WeatherRecommendationCard(
                    recommendation = WeatherRecommendation(
                        needsRainClothes = false,
                        clothingLevel = ClothingLevel.LEVEL_3,
                        temperature = 10.0,
                        precipitationProbability = 20.0,
                        precipitationAmount = 0.0,
                        message = "Moderate clothing - long sleeves and a light jacket\n☀️ No rain expected",
                        timeWindow = "Evening Commute (4-7 PM)",
                        rainForLater = false,
                        dayLabel = "Today"
                    ),
                    title = "🌆 From Work",
                    appConfig = AppConfig()
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
    LeavePreparedTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WeatherScreenHeader(onSettingsClick = {})
                
                CircularProgressIndicator()
                Text("Loading weather forecast...")
            }
        }
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun WeatherScreenPreview_Error() {
    LeavePreparedTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WeatherScreenHeader(onSettingsClick = {})
                
                ErrorCard(
                    message = "Failed to fetch weather: Network error",
                    onRetry = { }
                )
            }
        }
    }
}

