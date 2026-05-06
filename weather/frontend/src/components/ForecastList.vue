<template>
  <div class="forecast-list">
    <h3>5-Day Forecast</h3>
    <div class="forecast-grid">
      <div v-for="day in forecasts" :key="day.date" class="forecast-card">
        <p class="day">{{ day.day }}</p>
        <p class="date">{{ formatDate(day.date) }}</p>
        <img :src="`https://openweathermap.org/img/wn/${day.icon}@2x.png`" :alt="day.description" />
        <p class="desc">{{ day.description }}</p>
        <p class="temp">
          <span class="high">{{ day.temp_max }}°</span>
          <span class="low">{{ day.temp_min }}°</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  forecasts: {
    type: Array,
    required: true
  }
})

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.forecast-list h3 {
  color: white;
  font-size: 1.5rem;
  margin-bottom: 20px;
  text-align: center;
}

.forecast-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 15px;
}

.forecast-card {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  padding: 20px 15px;
  text-align: center;
  transition: transform 0.2s;
}

.forecast-card:hover {
  transform: scale(1.05);
}

.day {
  font-weight: 700;
  font-size: 1.1rem;
  color: #333;
}

.date {
  font-size: 0.85rem;
  color: #888;
  margin-bottom: 10px;
}

.forecast-card img {
  width: 50px;
  height: 50px;
}

.desc {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 10px;
  min-height: 40px;
}

.temp {
  font-weight: 600;
}

.high {
  color: #e74c3c;
  margin-right: 8px;
}

.low {
  color: #3498db;
}

@media (max-width: 768px) {
  .forecast-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 480px) {
  .forecast-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>