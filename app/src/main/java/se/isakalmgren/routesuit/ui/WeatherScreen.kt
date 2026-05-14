package se.isakalmgren.routesuit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.Context
import android.os.PowerManager
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import se.isakalmgren.routesuit.ui.theme.RouteSuitTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException
import retrofit2.HttpException
import se.isakalmgren.routesuit.AppConfig
import se.isakalmgren.routesuit.CommuteRecommendations
import se.isakalmgren.routesuit.ConfigRepository
import se.isakalmgren.routesuit.Constants
import se.isakalmgren.routesuit.R
import se.isakalmgren.routesuit.SmhiApiService
import se.isakalmgren.routesuit.TimeSeries
import se.isakalmgren.routesuit.WeatherRecommendation
import se.isakalmgren.routesuit.analyzeWeatherForCommutes
import se.isakalmgren.routesuit.generateRecommendationMessage
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(
        val recommendations: CommuteRecommendations,
        val timeSeries: List<TimeSeries>,
        val lastUpdated: Long = System.currentTimeMillis()
    ) : WeatherUiState()

    data class Error(val title: String, val message: String) : WeatherUiState()
}

private data class DetailSheetArgs(
    val recommendation: WeatherRecommendation,
    val timeSeries: List<TimeSeries>,
    val commuteStartHour: Int,
    val commuteEndHour: Int,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier,
    apiService: SmhiApiService = koinInject(),
    configRepository: ConfigRepository = koinInject(),
    onSettingsClick: () -> Unit = {},

    ) {
    val configState = configRepository.config.collectAsState()
    val appConfig = configState.value
    val context = LocalContext.current

    var uiState by remember { mutableStateOf<WeatherUiState>(WeatherUiState.Loading) }
    var detailSheetArgs by remember { mutableStateOf<DetailSheetArgs?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun fetchWeather() {
        coroutineScope.launch {
            uiState = WeatherUiState.Loading
            try {
                val lonStr = String.format(Locale.US, "%.4f", appConfig.longitude)
                val latStr = String.format(Locale.US, "%.3f", appConfig.latitude)
                Timber.d("Fetching weather for coordinates: lon=$lonStr, lat=$latStr")

                val response = apiService.getWeatherForecast(
                    longitude = lonStr,
                    latitude = latStr
                )
                val recommendations =
                    analyzeWeatherForCommutes(response.timeSeries, appConfig, context)
                uiState = WeatherUiState.Success(
                    recommendations,
                    response.timeSeries,
                    System.currentTimeMillis()
                )
                Timber.d("Weather data fetched successfully")
            } catch (e: SocketTimeoutException) {
                Timber.e(e, "Connection timeout while fetching weather")
                val title = context.getString(R.string.error_timeout)
                val message = context.getString(R.string.error_timeout_message)
                uiState = WeatherUiState.Error(title, message)
            } catch (e: UnknownHostException) {
                Timber.e(e, "Network error - unable to resolve host")
                val title = context.getString(R.string.error_network)
                val message = context.getString(R.string.error_network_message)
                uiState = WeatherUiState.Error(title, message)
            } catch (e: IOException) {
                Timber.e(e, "Network I/O error")
                val title = context.getString(R.string.error_network)
                val message = context.getString(R.string.error_network_message)
                uiState = WeatherUiState.Error(title, message)
            } catch (e: HttpException) {
                Timber.e(e, "HTTP error: ${e.code()}")
                val title = context.getString(R.string.error_server)
                val message = context.getString(R.string.error_server_message)
                uiState = WeatherUiState.Error(title, message)
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error while fetching weather")
                val title = context.getString(R.string.error_unknown)
                val message = context.getString(
                    R.string.error_unknown_message,
                    e.message ?: context.getString(R.string.error)
                )
                uiState = WeatherUiState.Error(title, message)
            }
        }
    }

    LaunchedEffect(appConfig) {
        fetchWeather()
    }

    detailSheetArgs?.let { args ->
        WeatherDetailSheet(
            recommendation = args.recommendation,
            timeSeries = args.timeSeries,
            config = appConfig,
            title = args.title,
            commuteStartHour = args.commuteStartHour,
            commuteEndHour = args.commuteEndHour,
            onDismiss = { detailSheetArgs = null }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            WeatherTopAppBar(onSettingsClick = onSettingsClick)
        }
    ) { innerPadding ->
        val isRefreshing = when (uiState) {
            is WeatherUiState.Loading -> true
            else -> false
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { fetchWeather() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                    BatteryOptimizationBanner(
                        onSettingsClick = onSettingsClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                AnimatedContent(
                    targetState = uiState,
                    contentKey = { it::class },
                    transitionSpec = {
                        fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                                fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
                    },
                    label = "weather_state"
                ) { state ->
                    when (state) {
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
                                val timestamp = formatTimestamp(state.lastUpdated, context)
                                val isStale =
                                    (System.currentTimeMillis() - state.lastUpdated) > Constants.STALE_DATA_THRESHOLD_HOURS * 60 * 60 * 1000

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = context.getString(R.string.last_updated, timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isStale) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = context.getString(R.string.data_stale_warning),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                val toWorkTitle = stringResource(R.string.to_work)
                                val fromWorkTitle = stringResource(R.string.from_work)

                                var morningVisible by remember { mutableStateOf(false) }
                                var eveningVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    morningVisible = true
                                    delay(100)
                                    eveningVisible = true
                                }

                                val cardEnter = scaleIn(
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    initialScale = 0.92f
                                ) + fadeIn(spring(stiffness = Spring.StiffnessMedium))

                                if (state.recommendations.morningCommute != null) {
                                    AnimatedVisibility(
                                        visible = morningVisible,
                                        enter = cardEnter
                                    ) {
                                        WeatherRecommendationCard(
                                            recommendation = state.recommendations.morningCommute,
                                            title = toWorkTitle,
                                            onClick = {
                                                detailSheetArgs = DetailSheetArgs(
                                                    recommendation = state.recommendations.morningCommute,
                                                    timeSeries = state.timeSeries,
                                                    commuteStartHour = appConfig.morningCommuteStartHour,
                                                    commuteEndHour = appConfig.morningCommuteEndHour,
                                                    title = toWorkTitle
                                                )
                                            }
                                        )
                                    }
                                }

                                if (state.recommendations.eveningCommute != null) {
                                    AnimatedVisibility(
                                        visible = eveningVisible,
                                        enter = cardEnter
                                    ) {
                                        WeatherRecommendationCard(
                                            recommendation = state.recommendations.eveningCommute,
                                            title = fromWorkTitle,
                                            onClick = {
                                                detailSheetArgs = DetailSheetArgs(
                                                    recommendation = state.recommendations.eveningCommute,
                                                    timeSeries = state.timeSeries,
                                                    commuteStartHour = appConfig.eveningCommuteStartHour,
                                                    commuteEndHour = appConfig.eveningCommuteEndHour,
                                                    title = fromWorkTitle
                                                )
                                            }
                                        )
                                    }
                                }

                                if (state.recommendations.morningCommute == null && state.recommendations.eveningCommute == null) {
                                    NoCommuteDataCard()
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
                                    title = state.title,
                                    message = state.message,
                                    onRetry = { fetchWeather() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun formatTimestamp(timestamp: Long, context: Context): String {
    val dateTime = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return if (dateTime.toLocalDate() == LocalDate.now()) {
        dateTime.format(timeFormatter)
    } else {
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.getDefault())
        dateTime.format(dateFormatter)
    }
}

@Composable
private fun ErrorCard(title: String, message: String, onRetry: () -> Unit) {
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
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium
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
                val morningRecommendation = morningRecommendationBase.copy(
                    message = generateRecommendationMessage(
                        morningRecommendationBase,
                        appConfig,
                        context
                    )
                )

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
                val eveningRecommendation = eveningRecommendationBase.copy(
                    message = generateRecommendationMessage(
                        eveningRecommendationBase,
                        appConfig,
                        context
                    )
                )

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
                    title = "Error",
                    message = "Failed to fetch weather: Network error",
                    onRetry = { }
                )
            }
        }
    }
}

