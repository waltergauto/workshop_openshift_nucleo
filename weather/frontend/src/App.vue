<template>
  <div class="app">
    <header class="header">
      <h1>Weather Dashboard</h1>
    </header>
    <main class="main">
      <SearchBar @search="handleSearch" />
      <LoadingSpinner v-if="loading" />
      <ErrorMessage v-if="error" :message="error" />
      <div v-if="weather && !loading" class="results">
        <WeatherCard :weather="weather" />
        <ForecastList v-if="forecasts.length" :forecasts="forecasts" />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import SearchBar from './components/SearchBar.vue'
import WeatherCard from './components/WeatherCard.vue'
import ForecastList from './components/ForecastList.vue'
import LoadingSpinner from './components/LoadingSpinner.vue'
import ErrorMessage from './components/ErrorMessage.vue'

const weather = ref(null)
const forecasts = ref([])
const loading = ref(false)
const error = ref(null)

const API_BASE = '/api'

const handleSearch = async (city) => {
  loading.value = true
  error.value = null
  weather.value = null
  forecasts.value = []

  try {
    const [weatherRes, forecastRes] = await Promise.all([
      axios.get(`${API_BASE}/weather`, { params: { city } }),
      axios.get(`${API_BASE}/forecast`, { params: { city } })
    ])

    weather.value = weatherRes.data
    forecasts.value = forecastRes.data.forecasts || []
  } catch (err) {
    error.value = err.response?.data?.error || 'Failed to fetch weather data'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.app {
  min-height: 100vh;
  padding: 20px;
}

.header {
  text-align: center;
  padding: 20px 0;
}

.header h1 {
  color: white;
  font-size: 2.5rem;
  font-weight: 700;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.main {
  max-width: 900px;
  margin: 0 auto;
}

.results {
  display: flex;
  flex-direction: column;
  gap: 30px;
  margin-top: 30px;
}
</style>