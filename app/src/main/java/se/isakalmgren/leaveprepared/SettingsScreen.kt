package se.isakalmgren.leaveprepared

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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    configRepository: ConfigRepository = koinInject(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configState = configRepository.config.collectAsState()
    val currentConfig = configState.value
    
    var morningStart by remember { mutableStateOf(currentConfig.morningCommuteStartHour.toString()) }
    var morningEnd by remember { mutableStateOf(currentConfig.morningCommuteEndHour.toString()) }
    var eveningStart by remember { mutableStateOf(currentConfig.eveningCommuteStartHour.toString()) }
    var eveningEnd by remember { mutableStateOf(currentConfig.eveningCommuteEndHour.toString()) }
    
    var tempVeryLight by remember { mutableStateOf(currentConfig.temperatureVeryLight.toString()) }
    var tempLight by remember { mutableStateOf(currentConfig.temperatureLight.toString()) }
    var tempModerate by remember { mutableStateOf(currentConfig.temperatureModerate.toString()) }
    var tempWarm by remember { mutableStateOf(currentConfig.temperatureWarm.toString()) }
    var tempVeryWarm by remember { mutableStateOf(currentConfig.temperatureVeryWarm.toString()) }
    var tempCold by remember { mutableStateOf(currentConfig.temperatureCold.toString()) }
    
    var precipProbThreshold by remember { mutableStateOf(currentConfig.precipitationProbabilityThreshold.toString()) }
    var precipAmountThreshold by remember { mutableStateOf(currentConfig.precipitationAmountThreshold.toString()) }
    
    var clothingMsg1 by remember { mutableStateOf(currentConfig.clothingMessageLevel1) }
    var clothingMsg2 by remember { mutableStateOf(currentConfig.clothingMessageLevel2) }
    var clothingMsg3 by remember { mutableStateOf(currentConfig.clothingMessageLevel3) }
    var clothingMsg4 by remember { mutableStateOf(currentConfig.clothingMessageLevel4) }
    var clothingMsg5 by remember { mutableStateOf(currentConfig.clothingMessageLevel5) }
    var clothingMsg6 by remember { mutableStateOf(currentConfig.clothingMessageLevel6) }
    var clothingMsg7 by remember { mutableStateOf(currentConfig.clothingMessageLevel7) }
    
    var timezone by remember { mutableStateOf(currentConfig.timezone.id) }
    
    val coroutineScope = rememberCoroutineScope()
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    // Update local state when config changes
    LaunchedEffect(currentConfig) {
        morningStart = currentConfig.morningCommuteStartHour.toString()
        morningEnd = currentConfig.morningCommuteEndHour.toString()
        eveningStart = currentConfig.eveningCommuteStartHour.toString()
        eveningEnd = currentConfig.eveningCommuteEndHour.toString()
        tempVeryLight = currentConfig.temperatureVeryLight.toString()
        tempLight = currentConfig.temperatureLight.toString()
        tempModerate = currentConfig.temperatureModerate.toString()
        tempWarm = currentConfig.temperatureWarm.toString()
        tempVeryWarm = currentConfig.temperatureVeryWarm.toString()
        tempCold = currentConfig.temperatureCold.toString()
        precipProbThreshold = currentConfig.precipitationProbabilityThreshold.toString()
        precipAmountThreshold = currentConfig.precipitationAmountThreshold.toString()
        clothingMsg1 = currentConfig.clothingMessageLevel1
        clothingMsg2 = currentConfig.clothingMessageLevel2
        clothingMsg3 = currentConfig.clothingMessageLevel3
        clothingMsg4 = currentConfig.clothingMessageLevel4
        clothingMsg5 = currentConfig.clothingMessageLevel5
        clothingMsg6 = currentConfig.clothingMessageLevel6
        clothingMsg7 = currentConfig.clothingMessageLevel7
        timezone = currentConfig.timezone.id
    }
    
    fun saveConfig() {
        try {
            val newConfig = AppConfig(
                morningCommuteStartHour = morningStart.toIntOrNull() ?: currentConfig.morningCommuteStartHour,
                morningCommuteEndHour = morningEnd.toIntOrNull() ?: currentConfig.morningCommuteEndHour,
                eveningCommuteStartHour = eveningStart.toIntOrNull() ?: currentConfig.eveningCommuteStartHour,
                eveningCommuteEndHour = eveningEnd.toIntOrNull() ?: currentConfig.eveningCommuteEndHour,
                temperatureVeryLight = tempVeryLight.toDoubleOrNull() ?: currentConfig.temperatureVeryLight,
                temperatureLight = tempLight.toDoubleOrNull() ?: currentConfig.temperatureLight,
                temperatureModerate = tempModerate.toDoubleOrNull() ?: currentConfig.temperatureModerate,
                temperatureWarm = tempWarm.toDoubleOrNull() ?: currentConfig.temperatureWarm,
                temperatureVeryWarm = tempVeryWarm.toDoubleOrNull() ?: currentConfig.temperatureVeryWarm,
                temperatureCold = tempCold.toDoubleOrNull() ?: currentConfig.temperatureCold,
                precipitationProbabilityThreshold = precipProbThreshold.toDoubleOrNull() ?: currentConfig.precipitationProbabilityThreshold,
                precipitationAmountThreshold = precipAmountThreshold.toDoubleOrNull() ?: currentConfig.precipitationAmountThreshold,
                clothingMessageLevel1 = clothingMsg1,
                clothingMessageLevel2 = clothingMsg2,
                clothingMessageLevel3 = clothingMsg3,
                clothingMessageLevel4 = clothingMsg4,
                clothingMessageLevel5 = clothingMsg5,
                clothingMessageLevel6 = clothingMsg6,
                clothingMessageLevel7 = clothingMsg7,
                timezone = try {
                    java.time.ZoneId.of(timezone)
                } catch (e: Exception) {
                    currentConfig.timezone
                }
            )
            configRepository.saveConfig(newConfig)
            showSaveSuccess = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(2000)
                showSaveSuccess = false
            }
        } catch (e: Exception) {
            // Handle error - could show a snackbar
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (showSaveSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Settings saved successfully!",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Commute Times Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Commute Times (24-hour format)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text("Morning Commute")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = morningStart,
                        onValueChange = { morningStart = it },
                        label = { Text("Start Hour") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = morningEnd,
                        onValueChange = { morningEnd = it },
                        label = { Text("End Hour") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                
                Text("Evening Commute")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = eveningStart,
                        onValueChange = { eveningStart = it },
                        label = { Text("Start Hour") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = eveningEnd,
                        onValueChange = { eveningEnd = it },
                        label = { Text("End Hour") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Temperature Thresholds Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Temperature Thresholds (°C)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = tempVeryLight,
                    onValueChange = { tempVeryLight = it },
                    label = { Text("Very Light (> °C)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("Shorts and t-shirt weather") }
                )
                OutlinedTextField(
                    value = tempLight,
                    onValueChange = { tempLight = it },
                    label = { Text("Light (> °C)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("T-shirt with light jacket") }
                )
                OutlinedTextField(
                    value = tempModerate,
                    onValueChange = { tempModerate = it },
                    label = { Text("Moderate (> °C)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("Long sleeves and light jacket") }
                )
                OutlinedTextField(
                    value = tempWarm,
                    onValueChange = { tempWarm = it },
                    label = { Text("Cool (> °C)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("Sweater and jacket") }
                )
                OutlinedTextField(
                    value = tempVeryWarm,
                    onValueChange = { tempVeryWarm = it },
                    label = { Text("Cold (> °C)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("Heavy jacket and layers") }
                )
                OutlinedTextField(
                    value = tempCold,
                    onValueChange = { tempCold = it },
                    label = { Text("Very Cold (> °C)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = { Text("Winter coat and warm layers") }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Precipitation Thresholds Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Precipitation Thresholds",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = precipProbThreshold,
                    onValueChange = { precipProbThreshold = it },
                    label = { Text("Probability Threshold (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = precipAmountThreshold,
                    onValueChange = { precipAmountThreshold = it },
                    label = { Text("Amount Threshold (mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Clothing Messages Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Clothing Level Messages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Customize the messages shown for each clothing level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = clothingMsg1,
                    onValueChange = { clothingMsg1 = it },
                    label = { Text("Level 1 (> ${currentConfig.temperatureVeryLight}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = clothingMsg2,
                    onValueChange = { clothingMsg2 = it },
                    label = { Text("Level 2 (> ${currentConfig.temperatureLight}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = clothingMsg3,
                    onValueChange = { clothingMsg3 = it },
                    label = { Text("Level 3 (> ${currentConfig.temperatureModerate}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = clothingMsg4,
                    onValueChange = { clothingMsg4 = it },
                    label = { Text("Level 4 (> ${currentConfig.temperatureWarm}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = clothingMsg5,
                    onValueChange = { clothingMsg5 = it },
                    label = { Text("Level 5 (> ${currentConfig.temperatureVeryWarm}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = clothingMsg6,
                    onValueChange = { clothingMsg6 = it },
                    label = { Text("Level 6 (> ${currentConfig.temperatureCold}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                OutlinedTextField(
                    value = clothingMsg7,
                    onValueChange = { clothingMsg7 = it },
                    label = { Text("Level 7 (< ${currentConfig.temperatureCold}°C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timezone Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Timezone",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = { Text("Timezone ID (e.g., Europe/Stockholm)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text(
                    text = "Common timezones: Europe/Stockholm, America/New_York, Asia/Tokyo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    configRepository.resetToDefaults()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset to Defaults")
            }
            Button(
                onClick = { saveConfig() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

