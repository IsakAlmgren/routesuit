package se.isakalmgren.leaveprepared

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("createdTime") val createdTime: String,
    @SerializedName("referenceTime") val referenceTime: String,
    @SerializedName("geometry") val geometry: Geometry,
    @SerializedName("timeSeries") val timeSeries: List<TimeSeries>
)

data class Geometry(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: List<Double>
)

data class TimeSeries(
    @SerializedName("time") val time: String,
    @SerializedName("intervalParametersStartTime") val intervalParametersStartTime: String,
    @SerializedName("data") val data: WeatherData
)

data class WeatherData(
    @SerializedName("air_temperature") val airTemperature: Double?,
    @SerializedName("wind_from_direction") val windFromDirection: Double?,
    @SerializedName("wind_speed") val windSpeed: Double?,
    @SerializedName("wind_speed_of_gust") val windSpeedOfGust: Double?,
    @SerializedName("relative_humidity") val relativeHumidity: Double?,
    @SerializedName("air_pressure_at_mean_sea_level") val airPressureAtMeanSeaLevel: Double?,
    @SerializedName("visibility_in_air") val visibilityInAir: Double?,
    @SerializedName("thunderstorm_probability") val thunderstormProbability: Double?,
    @SerializedName("probability_of_frozen_precipitation") val probabilityOfFrozenPrecipitation: Double?,
    @SerializedName("cloud_area_fraction") val cloudAreaFraction: Double?,
    @SerializedName("low_type_cloud_area_fraction") val lowTypeCloudAreaFraction: Double?,
    @SerializedName("medium_type_cloud_area_fraction") val mediumTypeCloudAreaFraction: Double?,
    @SerializedName("high_type_cloud_area_fraction") val highTypeCloudAreaFraction: Double?,
    @SerializedName("cloud_base_altitude") val cloudBaseAltitude: Double?,
    @SerializedName("cloud_top_altitude") val cloudTopAltitude: Double?,
    @SerializedName("precipitation_amount_mean") val precipitationAmountMean: Double?,
    @SerializedName("precipitation_amount_min") val precipitationAmountMin: Double?,
    @SerializedName("precipitation_amount_max") val precipitationAmountMax: Double?,
    @SerializedName("precipitation_amount_median") val precipitationAmountMedian: Double?,
    @SerializedName("probability_of_precipitation") val probabilityOfPrecipitation: Double?,
    @SerializedName("precipitation_frozen_part") val precipitationFrozenPart: Double?,
    @SerializedName("predominant_precipitation_type_at_surface") val predominantPrecipitationTypeAtSurface: Double?,
    @SerializedName("symbol_code") val symbolCode: Double?
)

