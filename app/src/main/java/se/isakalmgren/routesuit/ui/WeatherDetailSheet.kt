package se.isakalmgren.routesuit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import se.isakalmgren.routesuit.AppConfig
import se.isakalmgren.routesuit.R
import se.isakalmgren.routesuit.TimeSeries
import se.isakalmgren.routesuit.WeatherRecommendation
import se.isakalmgren.routesuit.parseTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailSheet(
    recommendation: WeatherRecommendation,
    timeSeries: List<TimeSeries>,
    config: AppConfig,
    title: String,
    commuteStartHour: Int,
    commuteEndHour: Int,
    onDismiss: () -> Unit
) {
    val date = recommendation.date ?: return

    val windowBuffer = 3
    val dayEntries: List<Pair<ZonedDateTime, TimeSeries>> = timeSeries
        .mapNotNull { entry ->
            val time = parseTime(entry.time, config) ?: return@mapNotNull null
            val hour = time.hour
            if (time.toLocalDate() == date && hour in (commuteStartHour - windowBuffer)..(commuteEndHour - 1 + windowBuffer))
                time to entry
            else null
        }
        .sortedBy { (time, _) -> time }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "$title · ${recommendation.dayLabel}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                )
                Text(
                    text = stringResource(
                        R.string.commute_window_hint,
                        commuteStartHour,
                        commuteEndHour
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.col_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp)
                )
                Text(
                    text = stringResource(R.string.col_temperature),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(72.dp)
                )
                Text(
                    text = stringResource(R.string.col_rain_chance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.col_rain_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            LazyColumn {
                items(dayEntries) { (zonedTime, entry) ->
                    val hour = zonedTime.hour
                    val isCommute = hour in commuteStartHour until commuteEndHour
                    val rowBackground = if (isCommute)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBackground)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = zonedTime.format(timeFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCommute) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.width(64.dp)
                        )
                        val temp = entry.data.airTemperature
                        Text(
                            text = if (temp != null) String.format(
                                LocalLocale.current.platformLocale,
                                "%.1f°",
                                temp
                            ) else "–",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCommute) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.width(72.dp)
                        )
                        val prob = entry.data.probabilityOfPrecipitation
                        Text(
                            text = if (prob != null) "${prob.toInt()}%" else "–",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        val amount = entry.data.precipitationAmountMean
                        Text(
                            text = if (amount != null && amount > 0.0)
                                String.format(Locale.getDefault(), "%.1f mm", amount)
                            else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
