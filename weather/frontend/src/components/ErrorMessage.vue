<template>
  <div class="error-message">
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    required: true
  }
})
</script>

<style scoped>
.error-message {
  background: rgba(231, 76, 60, 0.9);
  color: white;
  padding: 20px 30px;
  border-radius: 10px;
  text-align: center;
  max-width: 400px;
  margin: 30px auto;
}

.error-message p {
  font-size: 1.1rem;
}
</style>