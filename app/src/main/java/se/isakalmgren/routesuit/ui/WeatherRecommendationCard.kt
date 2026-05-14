package se.isakalmgren.routesuit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
    recommendation: WeatherRecommendation, title: String = ""
) {
    val cardColor =
        if (recommendation.rainForLater || recommendation.needsRainClothes) MaterialTheme.colorScheme.errorContainer else CardDefaults.cardColors().containerColor
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (recommendation.anyRainToday) "🌧" else "☀️",
                Modifier.padding(end = 8.dp),
                fontSize = 24.sp
            )



            Column() {
                Text(
                    text = title, fontWeight = FontWeight.W500, fontSize = 20.sp
                )
                Text(
                    text = if (recommendation.rainForLater) {
                        stringResource(R.string.bring_rain_clothes_later)
                    } else if (recommendation.needsRainClothes) {
                        stringResource(R.string.bring_rain_clothes)
                    } else stringResource(R.string.no_rain_expected),
                    fontWeight = FontWeight.W400,
                    fontSize = 20.sp,
                    color = if (recommendation.anyRainToday) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                )
                if (recommendation.rainForLater) {
                    Text(
                        text = stringResource(R.string.rain_expected_later),
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Temperature display
            Text(
                text = stringResource(R.string.temperature_format, recommendation.temperature),
                fontSize = 30.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

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
                recommendationBase, appConfig, context
            )
        )
        WeatherRecommendationCard(
            recommendation = recommendation, title = "To Work"
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
                recommendationBase, appConfig, context
            )
        )
        WeatherRecommendationCard(
            recommendation = recommendation, title = "To Work"
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
                recommendationBase, appConfig, context
            )
        )
        WeatherRecommendationCard(
            recommendation = recommendation, title = "To Work"
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
                recommendationBase, appConfig, context
            )
        )
        WeatherRecommendationCard(
            recommendation = recommendation, title = "From Work"
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
                recommendationBase, appConfig, context
            )
        )
        WeatherRecommendationCard(
            recommendation = recommendation, title = "From Work"
        )
    }
}

