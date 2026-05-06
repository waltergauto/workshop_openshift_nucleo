<template>
  <div class="search-bar">
    <div class="search-container">
      <input
        v-model="city"
        type="text"
        placeholder="Enter city name..."
        @keyup.enter="submit"
        list="history"
      />
      <datalist id="history">
        <option v-for="h in history" :key="h" :value="h" />
      </datalist>
      <button @click="submit" :disabled="!city.trim()">Search</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const emit = defineEmits(['search'])
const city = ref('')
const history = ref([])

const STORAGE_KEY = 'weather_search_history'

onMounted(() => {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored) {
    history.value = JSON.parse(stored)
  }
})

const addToHistory = (query) => {
  history.value = [query, ...history.value.filter(h => h !== query)].slice(0, 5)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value))
}

const submit = () => {
  if (city.value.trim()) {
    addToHistory(city.value.trim())
    emit('search', city.value.trim())
  }
}
</script>

<style scoped>
.search-bar {
  display: flex;
  justify-content: center;
}

.search-container {
  display: flex;
  gap: 10px;
  width: 100%;
  max-width: 500px;
}

input {
  padding: 15px 20px;
  font-size: 1.1rem;
  border: none;
  border-radius: 50px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
  outline: none;
  transition: box-shadow 0.3s;
}

input:focus {
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.3);
}

button {
  padding: 15px 30px;
  font-size: 1rem;
  font-weight: 600;
  border: none;
  border-radius: 50px;
  background: #ff6b6b;
  color: white;
  cursor: pointer;
  transition: transform 0.2s, background 0.2s;
}

button:hover:not(:disabled) {
  transform: scale(1.05);
  background: #ff5252;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>