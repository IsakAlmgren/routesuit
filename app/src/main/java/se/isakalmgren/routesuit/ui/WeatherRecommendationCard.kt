package se.isakalmgren.routesuit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import se.isakalmgren.routesuit.AppConfig
import se.isakalmgren.routesuit.R
import se.isakalmgren.routesuit.WeatherRecommendation
import se.isakalmgren.routesuit.generateRecommendationMessage
import se.isakalmgren.routesuit.ui.theme.RouteSuitTheme

@Composable
fun WeatherRecommendationCard(
    recommendation: WeatherRecommendation,
    title: String = ""
) {
    Card(
        modifier = Modifier.Companion.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.Companion.padding(20.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
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
                fontWeight = FontWeight.Companion.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Companion.Center,
                modifier = Modifier.Companion.fillMaxWidth(),
                style = MaterialTheme.typography.displayMedium
            )

            // Rain clothes recommendation
            RainRecommendationCard(recommendation = recommendation)
        }
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
        val recommendation = recommendationBase.copy(
            message = generateRecommendationMessage(
                recommendationBase,
                appConfig,
                context
            )
        )
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
        val recommendation = recommendationBase.copy(
            message = generateRecommendationMessage(
                recommendationBase,
                appConfig,
                context
            )
        )
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
        val recommendation = recommendationBase.copy(
            message = generateRecommendationMessage(
                recommendationBase,
                appConfig,
                context
            )
        )
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
        val recommendation = recommendationBase.copy(
            message = generateRecommendationMessage(
                recommendationBase,
                appConfig,
                context
            )
        )
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
        val recommendation = recommendationBase.copy(
            message = generateRecommendationMessage(
                recommendationBase,
                appConfig,
                context
            )
        )
        WeatherRecommendationCard(
            recommendation = recommendation,
            title = "From Work"
        )
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
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
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