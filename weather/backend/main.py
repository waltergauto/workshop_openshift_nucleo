from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
import httpx

app = FastAPI(title="Weather Dashboard API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

OPEN_METEO_BASE = "https://api.open-meteo.com/v1"
OPEN_GEO_BASE = "https://geocoding-api.open-meteo.com/v1"

WEATHER_CODES = {
    0: {"desc": "Clear sky", "icon": "01d"},
    1: {"desc": "Mainly clear", "icon": "01d"},
    2: {"desc": "Partly cloudy", "icon": "02d"},
    3: {"desc": "Overcast", "icon": "03d"},
    45: {"desc": "Fog", "icon": "50d"},
    48: {"desc": "Depositing rime fog", "icon": "50d"},
    51: {"desc": "Light drizzle", "icon": "09d"},
    53: {"desc": "Moderate drizzle", "icon": "09d"},
    55: {"desc": "Dense drizzle", "icon": "09d"},
    61: {"desc": "Slight rain", "icon": "10d"},
    63: {"desc": "Moderate rain", "icon": "10d"},
    65: {"desc": "Heavy rain", "icon": "10d"},
    71: {"desc": "Slight snow", "icon": "13d"},
    73: {"desc": "Moderate snow", "icon": "13d"},
    75: {"desc": "Heavy snow", "icon": "13d"},
    80: {"desc": "Slight rain showers", "icon": "09d"},
    81: {"desc": "Moderate rain showers", "icon": "09d"},
    82: {"desc": "Violent rain showers", "icon": "09d"},
    95: {"desc": "Thunderstorm", "icon": "11d"},
    96: {"desc": "Thunderstorm with slight hail", "icon": "11d"},
    99: {"desc": "Thunderstorm with heavy hail", "icon": "11d"},
}


class WeatherData(BaseModel):
    city: str
    country: str
    temperature: float
    humidity: int
    description: str
    icon: str
    feels_like: float
    wind_speed: float


class DailyForecast(BaseModel):
    date: str
    day: str
    temperature: float
    temp_min: float
    temp_max: float
    humidity: int
    description: str
    icon: str


class ForecastResponse(BaseModel):
    city: str
    forecasts: List[DailyForecast]


@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "weather-api"}


async def geocode_city(city: str) -> dict:
    client = httpx.AsyncClient(timeout=30.0)
    try:
        response = await client.get(
            f"{OPEN_GEO_BASE}/search",
            params={"name": city, "count": 1, "language": "en", "format": "json"}
        )
        data = response.json()
        if not data.get("results"):
            raise ValueError("City not found")
        return data["results"][0]
    finally:
        await client.aclose()


async def get_weather_data(lat: float, lon: float) -> dict:
    client = httpx.AsyncClient(timeout=30.0)
    try:
        response = await client.get(
            f"{OPEN_METEO_BASE}/forecast",
            params={
                "latitude": lat,
                "longitude": lon,
                "current": "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                "daily": "weather_code,temperature_2m_max,temperature_2m_min,relative_humidity_2m_max",
                "timezone": "auto",
                "forecast_days": 6,
            }
        )
        return response.json()
    finally:
        await client.aclose()


@app.get("/weather", response_model=WeatherData)
async def get_weather(city: str):
    try:
        location = await geocode_city(city)
        weather = await get_weather_data(location["latitude"], location["longitude"])

        current = weather["current"]
        code = current["weather_code"]
        weather_info = WEATHER_CODES.get(code, {"desc": "Unknown", "icon": "01d"})

        return WeatherData(
            city=location["name"],
            country=location.get("country", ""),
            temperature=round(current["temperature_2m"], 1),
            humidity=current["relative_humidity_2m"],
            description=weather_info["desc"],
            icon=weather_info["icon"],
            feels_like=round(current["apparent_temperature"], 1),
            wind_speed=round(current["wind_speed_10m"], 1)
        )
    except ValueError:
        return {"error": "City not found"}, 404
    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"error": str(e)}, 500


@app.get("/forecast", response_model=ForecastResponse)
async def get_forecast(city: str):
    try:
        location = await geocode_city(city)
        weather = await get_weather_data(location["latitude"], location["longitude"])

        daily = weather["daily"]
        days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
        forecasts = []

        for i in range(len(daily["time"])):
            date_str = daily["time"][i]
            from datetime import datetime
            dt = datetime.strptime(date_str, "%Y-%m-%d")
            day_name = days[dt.weekday()]

            code = daily["weather_code"][i]
            weather_info = WEATHER_CODES.get(code, {"desc": "Unknown", "icon": "01d"})

            forecasts.append(DailyForecast(
                date=date_str,
                day=day_name,
                temperature=round(daily["temperature_2m_max"][i], 1),
                temp_min=round(daily["temperature_2m_min"][i], 1),
                temp_max=round(daily["temperature_2m_max"][i], 1),
                humidity=daily["relative_humidity_2m_max"][i],
                description=weather_info["desc"],
                icon=weather_info["icon"]
            ))

        return ForecastResponse(
            city=location["name"],
            forecasts=forecasts[:5]
        )
    except ValueError:
        return {"error": "City not found"}, 404
    except Exception as e:
        import traceback
        traceback.print_exc()
        return {"error": str(e)}, 500


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)