# Weather Dashboard - Specification

## 1. Project Overview

**Project Name**: Weather Dashboard
**Type**: Full-stack web application (SPA + REST API)
**Core Functionality**: Display current weather and 5-day forecast for any city
**Target Users**: General users seeking weather information

## 2. Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Vue.js SPA    │ ──▶ │  FastAPI Python │ ──▶ │  OpenWeatherMap │
│   (Frontend)    │     │    (Backend)    │     │     (External)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## 3. Backend (FastAPI)

### Endpoints
- `GET /health` - Health check
- `GET /weather?city={city}` - Get current weather for a city
- `GET /forecast?city={city}` - Get 5-day forecast

### Data Model
```python
WeatherResponse:
  city: str
  country: str
  temperature: float (°C)
  humidity: int (%)
  description: str
  icon: str

ForecastResponse:
  city: str
  forecasts: List[DailyForecast]
```

### Environment
- Port: 8000
- External API: Open-Meteo (free, no API key required)

## 4. Frontend (Vue.js)

### Pages/Views
1. **Dashboard** - Main view with search and weather display

### Components
1. `SearchBar` - City search input
2. `WeatherCard` - Current weather display
3. `ForecastList` - 5-day forecast cards
4. `LoadingSpinner` - Loading state
5. `ErrorMessage` - Error display

### State Management
- Reactive weather data
- Loading/error states
- Search history (localStorage)

### Styling
- Clean, modern design
- Responsive layout
- Weather-themed colors (gradients based on conditions)

## 5. Project Structure

```
weather/
├── backend/
│   ├── main.py
│   ├── requirements.txt
│   └── .env
├── frontend/
│   ├── src/
│   │   ├── App.vue
│   │   ├── main.js
│   │   ├── components/
│   │   └── views/
│   ├── public/
│   ├── package.json
│   └── vite.config.js
├── frontend/nginx.conf
├── backend/dockerfile
├── frontend/dockerfile
├── docker-compose.yml
└── SPEC.md
```

## 6. Acceptance Criteria

- [x] Backend serves weather data from OpenWeatherMap
- [x] Frontend displays current weather for searched city
- [x] Frontend displays 5-day forecast
- [x] Loading states are shown during API calls
- [x] Error handling for invalid cities
- [x] Responsive design works on mobile
- [x] Docker deployment configured
- [x] Health check endpoint available