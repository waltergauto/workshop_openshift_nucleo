<template>
  <div class="weather-card" :class="getWeatherClass">
    <div class="location">
      <h2>{{ weather.city }}, {{ weather.country }}</h2>
    </div>
    <div class="temp-section">
      <img :src="iconUrl" :alt="weather.description" class="weather-icon" />
      <span class="temperature">{{ weather.temperature }}°C</span>
    </div>
    <div class="details">
      <p class="description">{{ weather.description }}</p>
      <div class="info-grid">
        <div class="info-item">
          <span class="label">Feels Like</span>
          <span class="value">{{ weather.feels_like }}°C</span>
        </div>
        <div class="info-item">
          <span class="label">Humidity</span>
          <span class="value">{{ weather.humidity }}%</span>
        </div>
        <div class="info-item">
          <span class="label">Wind</span>
          <span class="value">{{ weather.wind_speed }} km/h</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  weather: {
    type: Object,
    required: true
  }
})

const iconUrl = computed(() => 
  `https://openweathermap.org/img/wn/${props.weather.icon}@2x.png`
)

const getWeatherClass = computed(() => {
  const desc = props.weather.description.toLowerCase()
  if (desc.includes('clear')) return 'sunny'
  if (desc.includes('cloud')) return 'cloudy'
  if (desc.includes('rain')) return 'rainy'
  if (desc.includes('snow')) return 'snowy'
  return 'default'
})
</script>

<style scoped>
.weather-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  transition: transform 0.3s;
}

.weather-card:hover {
  transform: translateY(-5px);
}

.location h2 {
  color: #333;
  font-size: 1.8rem;
  margin-bottom: 20px;
}

.temp-section {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.weather-icon {
  width: 80px;
  height: 80px;
}

.temperature {
  font-size: 4rem;
  font-weight: 700;
  color: #333;
}

.description {
  font-size: 1.2rem;
  color: #666;
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.info-item {
  text-align: center;
  padding: 15px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 10px;
}

.info-item .label {
  display: block;
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 5px;
}

.info-item .value {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
}

@media (max-width: 600px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>