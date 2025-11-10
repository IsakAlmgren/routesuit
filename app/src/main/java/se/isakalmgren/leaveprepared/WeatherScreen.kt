package se.isakalmgren.leaveprepared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import se.isakalmgren.leaveprepared.ui.theme.LeavePreparedTheme

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Leave Prepared",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        when (val state = uiState) {
            is WeatherUiState.Loading -> {
                CircularProgressIndicator()
                Text("Loading weather forecast...")
            }
            
            is WeatherUiState.Success -> {
                if (state.recommendations.morningCommute != null) {
                    WeatherRecommendationCard(
                        recommendation = state.recommendations.morningCommute,
                        title = "🌅 To Work"
                    )
                }
                
                if (state.recommendations.eveningCommute != null) {
                    WeatherRecommendationCard(
                        recommendation = state.recommendations.eveningCommute,
                        title = "🌆 From Work"
                    )
                }
                
                if (state.recommendations.morningCommute == null && state.recommendations.eveningCommute == null) {
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
                
                Button(
                    onClick = { viewModel.fetchWeather() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Refresh")
                }
            }
            
            is WeatherUiState.Error -> {
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
                            text = state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = { viewModel.fetchWeather() },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
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
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            if (title.isNotEmpty()) {
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
                    if (recommendation.dayLabel.isNotEmpty()) {
                        Text(
                            text = recommendation.dayLabel,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Divider()
            }
            
            // Temperature display
            Text(
                text = "${String.format("%.1f", recommendation.temperature)}°C",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Divider()
            
            // Clothing recommendation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clothing Recommendation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = getClothingMessage(recommendation.clothingLevel),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Rain clothes recommendation
            if (recommendation.needsRainClothes) {
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
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
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
            
            // Full message
            Text(
                text = recommendation.message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 16.sp
            )
        }
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
                clothingLevel = ClothingLevel.WARM,
                temperature = 8.5,
                precipitationProbability = 75.0,
                precipitationAmount = 2.3,
                message = "Warm clothing - sweater and jacket recommended\n🌧️ Bring rain clothes! Precipitation probability: 75% Expected precipitation: 2.3 mm",
                timeWindow = "Morning Commute (7-9 AM)",
                rainForLater = false,
                dayLabel = "Tomorrow"
            ),
            title = "🌅 To Work"
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
                clothingLevel = ClothingLevel.MODERATE,
                temperature = 12.0,
                precipitationProbability = 0.0,
                precipitationAmount = 0.0,
                message = "Moderate clothing - long sleeves and a light jacket\n🌧️ Bring rain clothes for later! Rain expected on your way home (80% chance, 1.5 mm)",
                timeWindow = "Morning Commute (7-9 AM)",
                rainForLater = true,
                dayLabel = "Tomorrow"
            ),
            title = "🌅 To Work"
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
                clothingLevel = ClothingLevel.LIGHT,
                temperature = 18.0,
                precipitationProbability = 10.0,
                precipitationAmount = 0.0,
                message = "Light clothing - t-shirt with a light jacket\n☀️ No rain expected",
                timeWindow = "Morning Commute (7-9 AM)",
                rainForLater = false,
                dayLabel = "Today"
            ),
            title = "🌅 To Work"
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
                clothingLevel = ClothingLevel.COLD,
                temperature = -2.0,
                precipitationProbability = 90.0,
                precipitationAmount = 5.0,
                message = "Cold weather - winter coat and warm layers essential\n🌧️ Bring rain clothes! Precipitation probability: 90% Expected precipitation: 5.0 mm",
                timeWindow = "Evening Commute (4-7 PM)",
                rainForLater = false,
                dayLabel = "Today"
            ),
            title = "🌆 From Work"
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
                clothingLevel = ClothingLevel.VERY_LIGHT,
                temperature = 25.0,
                precipitationProbability = 5.0,
                precipitationAmount = 0.0,
                message = "Light clothing - shorts and t-shirt weather\n☀️ No rain expected",
                timeWindow = "Evening Commute (4-7 PM)",
                rainForLater = false,
                dayLabel = "Today"
            ),
            title = "🌆 From Work"
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
                Text(
                    text = "Leave Prepared",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                WeatherRecommendationCard(
                    recommendation = WeatherRecommendation(
                        needsRainClothes = true,
                        clothingLevel = ClothingLevel.WARM,
                        temperature = 8.5,
                        precipitationProbability = 75.0,
                        precipitationAmount = 2.3,
                        message = "Warm clothing - sweater and jacket recommended\n🌧️ Bring rain clothes! Precipitation probability: 75% Expected precipitation: 2.3 mm",
                        timeWindow = "Morning Commute (7-9 AM)",
                        rainForLater = false,
                        dayLabel = "Tomorrow"
                    ),
                    title = "🌅 To Work"
                )
                
                WeatherRecommendationCard(
                    recommendation = WeatherRecommendation(
                        needsRainClothes = false,
                        clothingLevel = ClothingLevel.MODERATE,
                        temperature = 10.0,
                        precipitationProbability = 20.0,
                        precipitationAmount = 0.0,
                        message = "Moderate clothing - long sleeves and a light jacket\n☀️ No rain expected",
                        timeWindow = "Evening Commute (4-7 PM)",
                        rainForLater = false,
                        dayLabel = "Today"
                    ),
                    title = "🌆 From Work"
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
                Text(
                    text = "Leave Prepared",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
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
                Text(
                    text = "Leave Prepared",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
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
                            text = "Failed to fetch weather: Network error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = { },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

