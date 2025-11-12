package se.isakalmgren.leaveprepared

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    configRepository: ConfigRepository = koinInject(),
    locationHelper: LocationHelper = koinInject(),
    modifier: Modifier = Modifier
) {
    val configState = configRepository.config.collectAsState()
    val currentConfig = configState.value
    
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
                        showError = "Could not get current location. Please try again or enter coordinates manually."
                    }
                } catch (e: Exception) {
                    showError = "Error getting location: ${e.message}"
                } finally {
                    isFetchingLocation = false
                }
            }
        } else {
            showError = "Location permission denied. Please grant location permission or enter coordinates manually."
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
                        showError = "Could not get current location. Please try again or enter coordinates manually."
                    }
                } catch (e: Exception) {
                    showError = "Error getting location: ${e.message}"
                } finally {
                    isFetchingLocation = false
                }
            }
        }
    }
    
    // Track if there are unsaved changes
    val hasChanges = remember(settingsState, currentConfig) {
        val currentState = SettingsState.fromConfig(currentConfig)
        settingsState != currentState
    }
    
    // Update state when config changes externally
    LaunchedEffect(currentConfig) {
        settingsState = SettingsState.fromConfig(currentConfig)
    }
    
    fun saveConfig() {
        try {
            val newConfig = settingsState.toConfig(currentConfig)
            configRepository.saveConfig(newConfig)
            showSaveSuccess = true
            showError = null
            coroutineScope.launch {
                kotlinx.coroutines.delay(2000)
                showSaveSuccess = false
            }
        } catch (e: Exception) {
            showError = "Failed to save settings: ${e.message}"
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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { handleBackNavigation() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                            text = "You have unsaved changes",
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
                            Text("Reset")
                        }
                        Button(
                            onClick = { saveConfig() },
                            modifier = Modifier.weight(2f),
                            enabled = hasChanges
                        ) {
                            Text("Save Changes")
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
                    message = "Settings saved successfully!",
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
            
            // Location Section
            SettingsSection(
                title = "Location",
                subtitle = "Set the location for weather forecasts"
            ) {
                val longitudeError = remember(settingsState.longitude) {
                    val lon = settingsState.longitude.toDoubleOrNull()
                    when {
                        settingsState.longitude.isBlank() -> "Required"
                        lon == null -> "Invalid number"
                        lon < -180 || lon > 180 -> "Must be between -180 and 180"
                        else -> null
                    }
                }
                
                val latitudeError = remember(settingsState.latitude) {
                    val lat = settingsState.latitude.toDoubleOrNull()
                    when {
                        settingsState.latitude.isBlank() -> "Required"
                        lat == null -> "Invalid number"
                        lat < -90 || lat > 90 -> "Must be between -90 and 90"
                        else -> null
                    }
                }
                
                OutlinedTextField(
                    value = settingsState.longitude,
                    onValueChange = { settingsState = settingsState.copy(longitude = it) },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = longitudeError != null,
                    supportingText = {
                        if (longitudeError != null) {
                            Text(longitudeError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Longitude (-180 to 180)")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = settingsState.latitude,
                    onValueChange = { settingsState = settingsState.copy(latitude = it) },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = latitudeError != null,
                    supportingText = {
                        if (latitudeError != null) {
                            Text(latitudeError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Latitude (-90 to 90)")
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
                        Text("Getting Location...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Get Current Location")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Commute Times Section
            SettingsSection(
                title = "Commute Times",
                subtitle = "24-hour format (0-23)"
            ) {
                CommuteTimeInput(
                    label = "Morning Commute",
                    startHour = settingsState.morningStart,
                    endHour = settingsState.morningEnd,
                    onStartChange = { settingsState = settingsState.copy(morningStart = it) },
                    onEndChange = { settingsState = settingsState.copy(morningEnd = it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                CommuteTimeInput(
                    label = "Evening Commute",
                    startHour = settingsState.eveningStart,
                    endHour = settingsState.eveningEnd,
                    onStartChange = { settingsState = settingsState.copy(eveningStart = it) },
                    onEndChange = { settingsState = settingsState.copy(eveningEnd = it) }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Temperature Thresholds Section
            SettingsSection(
                title = "Temperature Thresholds",
                subtitle = "Temperature in °C for clothing recommendations"
            ) {
                TemperatureThresholdInput(
                    label = "Very Light",
                    value = settingsState.tempVeryLight,
                    onValueChange = { settingsState = settingsState.copy(tempVeryLight = it) },
                    description = "Shorts and t-shirt weather"
                )
                
                TemperatureThresholdInput(
                    label = "Light",
                    value = settingsState.tempLight,
                    onValueChange = { settingsState = settingsState.copy(tempLight = it) },
                    description = "T-shirt with light jacket"
                )
                
                TemperatureThresholdInput(
                    label = "Moderate",
                    value = settingsState.tempModerate,
                    onValueChange = { settingsState = settingsState.copy(tempModerate = it) },
                    description = "Long sleeves and light jacket"
                )
                
                TemperatureThresholdInput(
                    label = "Cool",
                    value = settingsState.tempWarm,
                    onValueChange = { settingsState = settingsState.copy(tempWarm = it) },
                    description = "Sweater and jacket recommended"
                )
                
                TemperatureThresholdInput(
                    label = "Cold",
                    value = settingsState.tempVeryWarm,
                    onValueChange = { settingsState = settingsState.copy(tempVeryWarm = it) },
                    description = "Heavy jacket and layers needed"
                )
                
                TemperatureThresholdInput(
                    label = "Very Cold",
                    value = settingsState.tempCold,
                    onValueChange = { settingsState = settingsState.copy(tempCold = it) },
                    description = "Winter coat and warm layers"
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Precipitation Thresholds Section
            SettingsSection(
                title = "Precipitation Thresholds",
                subtitle = "When to recommend rain clothes"
            ) {
                val probError = remember(settingsState.precipProbThreshold) {
                    val prob = settingsState.precipProbThreshold.toDoubleOrNull()
                    when {
                        settingsState.precipProbThreshold.isBlank() -> "Required"
                        prob == null -> "Invalid number"
                        prob < 0 || prob > 100 -> "Must be 0-100"
                        else -> null
                    }
                }
                
                val amountError = remember(settingsState.precipAmountThreshold) {
                    val amount = settingsState.precipAmountThreshold.toDoubleOrNull()
                    when {
                        settingsState.precipAmountThreshold.isBlank() -> "Required"
                        amount == null -> "Invalid number"
                        amount < 0 -> "Must be positive"
                        else -> null
                    }
                }
                
                OutlinedTextField(
                    value = settingsState.precipProbThreshold,
                    onValueChange = { settingsState = settingsState.copy(precipProbThreshold = it) },
                    label = { Text("Probability Threshold (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = probError != null,
                    supportingText = {
                        if (probError != null) {
                            Text(probError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Minimum probability to recommend rain clothes")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = settingsState.precipAmountThreshold,
                    onValueChange = { settingsState = settingsState.copy(precipAmountThreshold = it) },
                    label = { Text("Amount Threshold (mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(amountError, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Minimum expected precipitation amount")
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Clothing Messages Section
            SettingsSection(
                title = "Clothing Messages",
                subtitle = "Customize messages for each clothing level"
            ) {
                ClothingMessageInput(
                    level = 1,
                    value = settingsState.clothingMsg1,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg1 = it) },
                    threshold = currentConfig.temperatureVeryLight
                )
                
                ClothingMessageInput(
                    level = 2,
                    value = settingsState.clothingMsg2,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg2 = it) },
                    threshold = currentConfig.temperatureLight
                )
                
                ClothingMessageInput(
                    level = 3,
                    value = settingsState.clothingMsg3,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg3 = it) },
                    threshold = currentConfig.temperatureModerate
                )
                
                ClothingMessageInput(
                    level = 4,
                    value = settingsState.clothingMsg4,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg4 = it) },
                    threshold = currentConfig.temperatureWarm
                )
                
                ClothingMessageInput(
                    level = 5,
                    value = settingsState.clothingMsg5,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg5 = it) },
                    threshold = currentConfig.temperatureVeryWarm
                )
                
                ClothingMessageInput(
                    level = 6,
                    value = settingsState.clothingMsg6,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg6 = it) },
                    threshold = currentConfig.temperatureCold
                )
                
                ClothingMessageInput(
                    level = 7,
                    value = settingsState.clothingMsg7,
                    onValueChange = { settingsState = settingsState.copy(clothingMsg7 = it) },
                    threshold = currentConfig.temperatureCold,
                    isLast = true
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
        }
        
        // Reset confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset to Defaults") },
                text = { Text("Are you sure you want to reset all settings to their default values? This cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            configRepository.resetToDefaults()
                            showResetDialog = false
                            showError = null
                        }
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes. Are you sure you want to leave? Your changes will be lost.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Cancel")
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                    startHour.isBlank() -> "Required"
                    hour == null -> "Invalid number"
                    hour < 0 || hour > 23 -> "Must be 0-23"
                    else -> null
                }
            }
            
            val endError = remember(endHour) {
                val hour = endHour.toIntOrNull()
                when {
                    endHour.isBlank() -> "Required"
                    hour == null -> "Invalid number"
                    hour < 0 || hour > 23 -> "Must be 0-23"
                    else -> null
                }
            }
            
            OutlinedTextField(
                value = startHour,
                onValueChange = onStartChange,
                label = { Text("Start Hour") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = startError != null,
                supportingText = startError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
            )
            OutlinedTextField(
                value = endHour,
                onValueChange = onEndChange,
                label = { Text("End Hour") },
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
    val error = remember(value) {
        val temp = value.toDoubleOrNull()
        when {
            value.isBlank() -> "Required"
            temp == null -> "Invalid number"
            else -> null
        }
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label (> °C)") },
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
    val error = remember(value) {
        if (value.isBlank()) "Message cannot be empty" else null
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                if (isLast) {
                    "Level $level (< ${threshold}°C)"
                } else {
                    "Level $level (> ${threshold}°C)"
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
                Text("Dismiss")
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
    val tempVeryLight: String,
    val tempLight: String,
    val tempModerate: String,
    val tempWarm: String,
    val tempVeryWarm: String,
    val tempCold: String,
    val precipProbThreshold: String,
    val precipAmountThreshold: String,
    val clothingMsg1: String,
    val clothingMsg2: String,
    val clothingMsg3: String,
    val clothingMsg4: String,
    val clothingMsg5: String,
    val clothingMsg6: String,
    val clothingMsg7: String
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
                tempVeryLight = config.temperatureVeryLight.toString(),
                tempLight = config.temperatureLight.toString(),
                tempModerate = config.temperatureModerate.toString(),
                tempWarm = config.temperatureWarm.toString(),
                tempVeryWarm = config.temperatureVeryWarm.toString(),
                tempCold = config.temperatureCold.toString(),
                precipProbThreshold = config.precipitationProbabilityThreshold.toString(),
                precipAmountThreshold = config.precipitationAmountThreshold.toString(),
                clothingMsg1 = config.clothingMessageLevel1,
                clothingMsg2 = config.clothingMessageLevel2,
                clothingMsg3 = config.clothingMessageLevel3,
                clothingMsg4 = config.clothingMessageLevel4,
                clothingMsg5 = config.clothingMessageLevel5,
                clothingMsg6 = config.clothingMessageLevel6,
                clothingMsg7 = config.clothingMessageLevel7
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
            temperatureVeryLight = tempVeryLight.toDoubleOrNull() ?: fallbackConfig.temperatureVeryLight,
            temperatureLight = tempLight.toDoubleOrNull() ?: fallbackConfig.temperatureLight,
            temperatureModerate = tempModerate.toDoubleOrNull() ?: fallbackConfig.temperatureModerate,
            temperatureWarm = tempWarm.toDoubleOrNull() ?: fallbackConfig.temperatureWarm,
            temperatureVeryWarm = tempVeryWarm.toDoubleOrNull() ?: fallbackConfig.temperatureVeryWarm,
            temperatureCold = tempCold.toDoubleOrNull() ?: fallbackConfig.temperatureCold,
            precipitationProbabilityThreshold = precipProbThreshold.toDoubleOrNull() ?: fallbackConfig.precipitationProbabilityThreshold,
            precipitationAmountThreshold = precipAmountThreshold.toDoubleOrNull() ?: fallbackConfig.precipitationAmountThreshold,
            clothingMessageLevel1 = clothingMsg1,
            clothingMessageLevel2 = clothingMsg2,
            clothingMessageLevel3 = clothingMsg3,
            clothingMessageLevel4 = clothingMsg4,
            clothingMessageLevel5 = clothingMsg5,
            clothingMessageLevel6 = clothingMsg6,
            clothingMessageLevel7 = clothingMsg7
        )
    }
}
