package se.isakalmgren.leaveprepared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    data object Loading : WeatherUiState()
    data class Success(val recommendations: CommuteRecommendations) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel : ViewModel() {
    private val apiService = SmhiApiService.create()
    
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()
    
    init {
        fetchWeather()
    }
    
    fun fetchWeather() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = apiService.getWeatherForecast()
                val recommendations = analyzeWeatherForCommutes(response.timeSeries)
                
                _uiState.value = WeatherUiState.Success(recommendations)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error("Failed to fetch weather: ${e.message}")
            }
        }
    }
}

