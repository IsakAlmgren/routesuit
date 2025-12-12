package se.isakalmgren.routesuit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import android.app.Activity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    configRepository: ConfigRepository = koinInject(),
    locationHelper: LocationHelper = koinInject(),
    languageRepository: LanguageRepository = koinInject(),
    modifier: Modifier = Modifier
) {
    val configState = configRepository.config.collectAsState()
    val currentConfig = configState.value
    val context = LocalContext.current
    
    // State management using a data class for better organization
    var settingsState by remember(currentConfig) {
        mutableStateOf(SettingsState.fromConfig(currentConfig))
    }
    
    val coroutineScope = rememberCoroutineScope()
    var showSaveSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    
    // Permission launcher for location
    val locationPermissionLauncherInternal = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            coroutineScope.launch {
                isFetchingLocation = true
                try {
                    val location = locationHelper.getCurrentLocation()
                    if (location != null) {
                        settingsState = settingsState.copy(
                            longitude = String.format(java.util.Locale.US, "%.4f", location.first),
                            latitude = String.format(java.util.Locale.US, "%.4f", location.second)
                        )
                    } else {
                        showError = context.getString(R.string.could_not_get_location)
                    }
                } catch (e: Exception) {
                    showError = context.getString(R.string.error_getting_location, e.message ?: "")
                } finally {
                    isFetchingLocation = false
                }
            }
        } else {
            showError = context.getString(R.string.location_permission_denied)
        }
    }
    
    fun getCurrentLocation() {
        if (!locationHelper.hasLocationPermission()) {
            locationPermissionLauncherInternal.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            coroutineScope.launch {
                isFetchingLocation = true
                try {
                    val location = locationHelper.getCurrentLocation()
                    if (location != null) {
                        settingsState = settingsState.copy(
                            longitude = String.format(java.util.Locale.US, "%.4f", location.first),
                            latitude = String.format(java.util.Locale.US, "%.4f", location.second)
                        )
                    } else {
                        showError = context.getString(R.string.could_not_get_location)
                    }
                } catch (e: Exception) {
                    showError = context.getString(R.string.error_getting_location, e.message ?: "")
                } finally {
                    isFetchingLocation = false
                }
            }
        }
    }
    
    // Track initial language to detect changes
    val initialLanguage = remember { languageRepository.getSelectedLanguage() }
    var currentLanguage by remember { mutableStateOf(initialLanguage) }
    
    // Track if there are unsaved changes (config or language)
    val hasChanges = remember(settingsState, currentConfig, currentLanguage, initialLanguage) {
        val currentState = SettingsState.fromConfig(currentConfig)
        val configChanged = settingsState != currentState
        val languageChanged = currentLanguage != initialLanguage
        configChanged || languageChanged
    }
    
    // Update state when config changes externally
    LaunchedEffect(currentConfig) {
        settingsState = SettingsState.fromConfig(currentConfig)
    }
    
    fun saveConfig() {
        try {
            val newConfig = settingsState.toConfig(currentConfig)
            configRepository.saveConfig(newConfig)
            
            // Save language preference if it changed
            val languageChanged = currentLanguage != initialLanguage
            if (languageChanged) {
                languageRepository.setLanguage(currentLanguage)
            }
            
            // If language changed, recreate activity to apply locale
            if (languageChanged) {
                val activity = context as? Activity
                activity?.recreate()
            } else {
                showSaveSuccess = true
                showError = null
                coroutineScope.launch {
                    kotlinx.coroutines.delay(2000)
                    showSaveSuccess = false
                }
            }
        } catch (e: Exception) {
            showError = context.getString(R.string.failed_to_save_settings, e.message ?: "")
        }
    }
    
    fun handleBackNavigation() {
        if (hasChanges) {
            showExitDialog = true
        } else {
            navController.popBackStack()
        }
    }
    
    // Handle system back button - Navigation Compose handles this automatically,
    // but we intercept it to check for unsaved changes
    BackHandler(enabled = true) {
        handleBackNavigation()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { handleBackNavigation() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasChanges) {
                        Text(
                            text = stringResource(R.string.you_have_unsaved_changes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.reset))
                        }
                        Button(
                            onClick = { saveConfig() },
                            modifier = Modifier.weight(2f),
                            enabled = hasChanges
                        ) {
                            Text(stringResource(R.string.save_changes))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            
            // Success/Error messages
            if (showSaveSuccess) {
                SuccessCard(
                    message = stringResource(R.string.settings_saved_successfully),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (showError != null) {
                ErrorCard(
                    message = showError!!,
                    modifier = Modifier.fillMaxWidth(),
                    onDismiss = { showError = null }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Language Section
            SettingsSection(
                title = stringResource(R.string.language),
                subtitle = stringResource(R.string.language_subtitle)
            ) {
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (currentLanguage) {
                            AppLanguage.AUTO -> stringResource(R.string.language_auto)
                            AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                            AppLanguage.SWEDISH -> stringResource(R.string.language_swedish)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AppLanguage.values().forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (language) {
                                            AppLanguage.AUTO -> stringResource(R.string.language_auto)
                                            AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                            AppLanguage.SWEDISH -> stringResource(R.string.language_swedish)
                                        }
                                    )
                                },
                                onClick = {
                                    currentLanguage = language
                                    // Language will be saved and applied when user clicks Save
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Location Section
            SettingsSection(
                title = stringResource(R.string.location),
                subtitle = stringResource(R.string.location_subtitle)
            ) {
                val longitudeError = remember(settingsState.longitude) {
                    val lon = settingsState.longitude.toDoubleOrNull()
                    when {
                        settingsState.longitude.isBlank() -> context.getString(R.string.required)
                        lon == null -> context.getString(R.string.invalid_number)
                        lon < -180 || lon > 180 -> context.getString(R.string.must_be_between, "-180", "180")
                        else -> null
                    }
                }
                
                val latitudeError = remember(settingsState.latitude) {
                    val lat = settingsState.latitude.toDoubleOrNull()
                    when {
                        settingsState.latitude.isBlank() -> context.getString(R.string.required)
                        lat == null -> context.getString(R.string.invalid_number)
                        lat < -90 || lat > 90 -> context.getString(R.string.must_be_between, "-90", "90")
                        else -> null
                    }
                }
                
                OutlinedTextField(
                    value = settingsState.longitude,
                    onValueChange = { settingsState = settingsState.copy(longitude = it) },
                    label = { Text(stringResource(R.string.longitude)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = longitudeError != null,
                    supportingText = {
                        if (longitudeError != null) {
                            Text(longitudeError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.longitude_hint))
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = settingsState.latitude,
                    onValueChange = { settingsState = settingsState.copy(latitude = it) },
                    label = { Text(stringResource(R.string.latitude)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = latitudeError != null,
                    supportingText = {
                        if (latitudeError != null) {
                            Text(latitudeError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.latitude_hint))
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { getCurrentLocation() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFetchingLocation
                ) {
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.getting_location))
                    } else {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.get_current_location))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Commute Times Section
            SettingsSection(
                title = stringResource(R.string.commute_times),
                subtitle = stringResource(R.string.commute_times_subtitle)
            ) {
                CommuteTimeInput(
                    label = stringResource(R.string.morning_commute),
                    startHour = settingsState.morningStart,
                    endHour = settingsState.morningEnd,
                    onStartChange = { settingsState = settingsState.copy(morningStart = it) },
                    onEndChange = { settingsState = settingsState.copy(morningEnd = it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                CommuteTimeInput(
                    label = stringResource(R.string.evening_commute),
                    startHour = settingsState.eveningStart,
                    endHour = settingsState.eveningEnd,
                    onStartChange = { settingsState = settingsState.copy(eveningStart = it) },
                    onEndChange = { settingsState = settingsState.copy(eveningEnd = it) }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Temperature Thresholds Section
            SettingsSection(
                title = stringResource(R.string.temperature_thresholds),
                subtitle = stringResource(R.string.temperature_thresholds_subtitle)
            ) {
                TemperatureThresholdInput(
                    label = stringResource(R.string.hot),
                    value = settingsState.tempHot,
                    onValueChange = { settingsState = settingsState.copy(tempHot = it) },
                    description = stringResource(R.string.hot_description)
                )
                
                TemperatureThresholdInput(
                    label = stringResource(R.string.warm),
                    value = settingsState.tempWarm,
                    onValueChange = { settingsState = settingsState.copy(tempWarm = it) },
                    description = stringResource(R.string.warm_description)
                )
                
                TemperatureThresholdInput(
                    label = stringResource(R.string.mild),
                    value = settingsState.tempMild,
                    onValueChange = { settingsState = settingsState.copy(tempMild = it) },
                    description = stringResource(R.string.mild_description)
                )
                
                TemperatureThresholdInput(
                    label = stringResource(R.string.cool),
                    value = settingsState.tempCool,
                    onValueChange = { settingsState = settingsState.copy(tempCool = it) },
                    description = stringResource(R.string.cool_description)
                )
                
                TemperatureThresholdInput(
                    label = stringResource(R.string.cold),
                    value = settingsState.tempCold,
                    onValueChange = { settingsState = settingsState.copy(tempCold = it) },
                    description = stringResource(R.string.cold_description)
                )
                
                TemperatureThresholdInput(
                    label = stringResource(R.string.very_cold),
                    value = settingsState.tempVeryCold,
                    onValueChange = { settingsState = settingsState.copy(tempVeryCold = it) },
                    description = stringResource(R.string.very_cold_description)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Precipitation Thresholds Section
            SettingsSection(
                title = stringResource(R.string.precipitation_thresholds),
                subtitle = stringResource(R.string.precipitation_thresholds_subtitle)
            ) {
                val probError = remember(settingsState.precipProbThreshold) {
                    val prob = settingsState.precipProbThreshold.toDoubleOrNull()
                    when {
                        settingsState.precipProbThreshold.isBlank() -> context.getString(R.string.required)
                        prob == null -> context.getString(R.string.invalid_number)
                        prob < 0 || prob > 100 -> context.getString(R.string.must_be_0_to_100)
                        else -> null
                    }
                }
                
                val amountError = remember(settingsState.precipAmountThreshold) {
                    val amount = settingsState.precipAmountThreshold.toDoubleOrNull()
                    when {
                        settingsState.precipAmountThreshold.isBlank() -> context.getString(R.string.required)
                        amount == null -> context.getString(R.string.invalid_number)
                        amount < 0 -> context.getString(R.string.must_be_positive)
                        else -> null
                    }
                }
                
                OutlinedTextField(
                    value = settingsState.precipProbThreshold,
                    onValueChange = { settingsState = settingsState.copy(precipProbThreshold = it) },
                    label = { Text(stringResource(R.string.probability_threshold_percent)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = probError != null,
                    supportingText = {
                        if (probError != null) {
                            Text(probError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.probability_threshold_hint))
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = settingsState.precipAmountThreshold,
                    onValueChange = { settingsState = settingsState.copy(precipAmountThreshold = it) },
                    label = { Text(stringResource(R.string.amount_threshold_mm)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(amountError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(stringResource(R.string.amount_threshold_hint))
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Notification Days Section
            SettingsSection(
                title = stringResource(R.string.notification_days),
                subtitle = stringResource(R.string.notification_days_subtitle)
            ) {
                val daysOfWeek = listOf(
                    Calendar.MONDAY to stringResource(R.string.monday),
                    Calendar.TUESDAY to stringResource(R.string.tuesday),
                    Calendar.WEDNESDAY to stringResource(R.string.wednesday),
                    Calendar.THURSDAY to stringResource(R.string.thursday),
                    Calendar.FRIDAY to stringResource(R.string.friday),
                    Calendar.SATURDAY to stringResource(R.string.saturday),
                    Calendar.SUNDAY to stringResource(R.string.sunday)
                )
                
                daysOfWeek.forEach { (dayOfWeek, dayName) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val currentDays = settingsState.notificationDays.toMutableSet()
                                if (currentDays.contains(dayOfWeek)) {
                                    currentDays.remove(dayOfWeek)
                                } else {
                                    currentDays.add(dayOfWeek)
                                }
                                settingsState = settingsState.copy(notificationDays = currentDays)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = settingsState.notificationDays.contains(dayOfWeek),
                            onCheckedChange = { checked ->
                                val currentDays = settingsState.notificationDays.toMutableSet()
                                if (checked) {
                                    currentDays.add(dayOfWeek)
                                } else {
                                    currentDays.remove(dayOfWeek)
                                }
                                settingsState = settingsState.copy(notificationDays = currentDays)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Clothing Messages Section
            SettingsSection(
                title = stringResource(R.string.clothing_messages),
                subtitle = stringResource(R.string.clothing_messages_subtitle)
            ) {
                ClothingMessageInput(
                    level = 1,
                    value = settingsState.clothingMsg1,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg1 = it) },
                    threshold = currentConfig.temperatureHot
                )
                
                ClothingMessageInput(
                    level = 2,
                    value = settingsState.clothingMsg2,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg2 = it) },
                    threshold = currentConfig.temperatureWarm
                )
                
                ClothingMessageInput(
                    level = 3,
                    value = settingsState.clothingMsg3,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg3 = it) },
                    threshold = currentConfig.temperatureMild
                )
                
                ClothingMessageInput(
                    level = 4,
                    value = settingsState.clothingMsg4,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg4 = it) },
                    threshold = currentConfig.temperatureCool
                )
                
                ClothingMessageInput(
                    level = 5,
                    value = settingsState.clothingMsg5,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg5 = it) },
                    threshold = currentConfig.temperatureCold
                )
                
                ClothingMessageInput(
                    level = 6,
                    value = settingsState.clothingMsg6,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg6 = it) },
                    threshold = currentConfig.temperatureVeryCold
                )
                
                ClothingMessageInput(
                    level = 7,
                    value = settingsState.clothingMsg7,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg7 = it) },
                    threshold = currentConfig.temperatureVeryCold,
                    isLast = true
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
        }
        
        // Reset confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(stringResource(R.string.reset_to_defaults)) },
                text = { Text(stringResource(R.string.reset_confirmation)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            configRepository.resetToDefaults()
                            showResetDialog = false
                            showError = null
                        }
                    ) {
                        Text(stringResource(R.string.reset))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        
        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text(stringResource(R.string.unsaved_changes)) },
                text = { Text(stringResource(R.string.unsaved_changes_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text(stringResource(R.string.discard))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

// Reusable Components

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            content()
        }
    }
}

@Composable
private fun CommuteTimeInput(
    label: String,
    startHour: String,
    endHour: String,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val startError = remember(startHour) {
                val hour = startHour.toIntOrNull()
                when {
                    startHour.isBlank() -> context.getString(R.string.required)
                    hour == null -> context.getString(R.string.invalid_number)
                    hour < 0 || hour > 23 -> context.getString(R.string.must_be_0_to_23)
                    else -> null
                }
            }
            
            val endError = remember(endHour) {
                val hour = endHour.toIntOrNull()
                when {
                    endHour.isBlank() -> context.getString(R.string.required)
                    hour == null -> context.getString(R.string.invalid_number)
                    hour < 0 || hour > 23 -> context.getString(R.string.must_be_0_to_23)
                    else -> null
                }
            }
            
            OutlinedTextField(
                value = startHour,
                onValueChange = onStartChange,
                label = { Text(stringResource(R.string.start_hour)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = startError != null,
                supportingText = startError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            OutlinedTextField(
                value = endHour,
                onValueChange = onEndChange,
                label = { Text(stringResource(R.string.end_hour)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = endError != null,
                supportingText = endError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
        }
    }
}

@Composable
private fun TemperatureThresholdInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    description: String
) {
    val context = LocalContext.current
    val error = remember(value) {
        val temp = value.toDoubleOrNull()
        when {
            value.isBlank() -> context.getString(R.string.required)
            temp == null -> context.getString(R.string.invalid_number)
            else -> null
        }
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.temperature_threshold_label, label)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            } else {
                Text(description)
            }
        }
    )
}

@Composable
private fun ClothingMessageInput(
    level: Int,
    value: String,
    onValueChange: (String) -> Unit,
    threshold: Double,
    isLast: Boolean = false
) {
    val context = LocalContext.current
    val error = remember(value) {
        if (value.isBlank()) context.getString(R.string.message_cannot_be_empty) else null
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                if (isLast) {
                    stringResource(R.string.clothing_level_label_last, level, threshold)
                } else {
                    stringResource(R.string.clothing_level_label, level, threshold)
                }
            )
        },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 3,
        minLines = 1,
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
    )
    
    if (!isLast) {
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SuccessCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    }
}

// State management data class
private data class SettingsState(
    val longitude: String,
    val latitude: String,
    val morningStart: String,
    val morningEnd: String,
    val eveningStart: String,
    val eveningEnd: String,
    val tempHot: String,
    val tempWarm: String,
    val tempMild: String,
    val tempCool: String,
    val tempCold: String,
    val tempVeryCold: String,
    val precipProbThreshold: String,
    val precipAmountThreshold: String,
    val clothingMsg1: String,
    val clothingMsg2: String,
    val clothingMsg3: String,
    val clothingMsg4: String,
    val clothingMsg5: String,
    val clothingMsg6: String,
    val clothingMsg7: String,
    val notificationDays: Set<Int>
) {
    companion object {
        fun fromConfig(config: AppConfig): SettingsState {
            return SettingsState(
                longitude = String.format(java.util.Locale.US, "%.4f", config.longitude),
                latitude = String.format(java.util.Locale.US, "%.3f", config.latitude),
                morningStart = config.morningCommuteStartHour.toString(),
                morningEnd = config.morningCommuteEndHour.toString(),
                eveningStart = config.eveningCommuteStartHour.toString(),
                eveningEnd = config.eveningCommuteEndHour.toString(),
                tempHot = config.temperatureHot.toString(),
                tempWarm = config.temperatureWarm.toString(),
                tempMild = config.temperatureMild.toString(),
                tempCool = config.temperatureCool.toString(),
                tempCold = config.temperatureCold.toString(),
                tempVeryCold = config.temperatureVeryCold.toString(),
                precipProbThreshold = config.precipitationProbabilityThreshold.toString(),
                precipAmountThreshold = config.precipitationAmountThreshold.toString(),
                clothingMsg1 = config.clothingMessageLevel1,
                clothingMsg2 = config.clothingMessageLevel2,
                clothingMsg3 = config.clothingMessageLevel3,
                clothingMsg4 = config.clothingMessageLevel4,
                clothingMsg5 = config.clothingMessageLevel5,
                clothingMsg6 = config.clothingMessageLevel6,
                clothingMsg7 = config.clothingMessageLevel7,
                notificationDays = config.notificationDays
            )
        }
    }
    
    fun toConfig(fallbackConfig: AppConfig): AppConfig {
        return AppConfig(
            longitude = longitude.toDoubleOrNull() ?: fallbackConfig.longitude,
            latitude = latitude.toDoubleOrNull() ?: fallbackConfig.latitude,
            morningCommuteStartHour = morningStart.toIntOrNull() ?: fallbackConfig.morningCommuteStartHour,
            morningCommuteEndHour = morningEnd.toIntOrNull() ?: fallbackConfig.morningCommuteEndHour,
            eveningCommuteStartHour = eveningStart.toIntOrNull() ?: fallbackConfig.eveningCommuteStartHour,
            eveningCommuteEndHour = eveningEnd.toIntOrNull() ?: fallbackConfig.eveningCommuteEndHour,
            temperatureHot = tempHot.toDoubleOrNull() ?: fallbackConfig.temperatureHot,
            temperatureWarm = tempWarm.toDoubleOrNull() ?: fallbackConfig.temperatureWarm,
            temperatureMild = tempMild.toDoubleOrNull() ?: fallbackConfig.temperatureMild,
            temperatureCool = tempCool.toDoubleOrNull() ?: fallbackConfig.temperatureCool,
            temperatureCold = tempCold.toDoubleOrNull() ?: fallbackConfig.temperatureCold,
            temperatureVeryCold = tempVeryCold.toDoubleOrNull() ?: fallbackConfig.temperatureVeryCold,
            precipitationProbabilityThreshold = precipProbThreshold.toDoubleOrNull() ?: fallbackConfig.precipitationProbabilityThreshold,
            precipitationAmountThreshold = precipAmountThreshold.toDoubleOrNull() ?: fallbackConfig.precipitationAmountThreshold,
            clothingMessageLevel1 = clothingMsg1,
            clothingMessageLevel2 = clothingMsg2,
            clothingMessageLevel3 = clothingMsg3,
            clothingMessageLevel4 = clothingMsg4,
            clothingMessageLevel5 = clothingMsg5,
            clothingMessageLevel6 = clothingMsg6,
            clothingMessageLevel7 = clothingMsg7,
            notificationDays = notificationDays
        )
    }
}
