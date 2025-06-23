## Chapter 10: Project 3: Weather Dashboard (Using a Public API)

Our third project will be a Weather Dashboard. This application will allow users to search for a city and view its current weather conditions. The key new skill we'll be focusing on here is fetching data from an external **API (Application Programming Interface)** using the `fetch` command and working with asynchronous JavaScript (`async/await`).

### 10.1 Project Overview and Features

The Weather Dashboard will:

1.  **City Search:** Allow users to input a city name.
2.  **Fetch Weather Data:** On search, fetch current weather data for that city from a public weather API.
3.  **Display Weather Info:** Show relevant information like temperature, weather description (e.g., "Cloudy", "Sunny"), humidity, wind speed, and a weather icon.
4.  **Handle Errors:** Display appropriate messages if the city is not found or if there's an API error.
5.  **(Optional) Save Last Searched City:** Use `localStorage` to remember and possibly auto-load the weather for the last searched city.

### 10.2 Choosing and Getting Access to a Weather API

There are several public weather APIs available. For this project, we'll aim to use one that is free and relatively simple to get started with. A popular choice is **OpenWeatherMap**.

**OpenWeatherMap API:**

*   **Website:** [https://openweathermap.org/](https://openweathermap.org/)
*   **API Docs (Current Weather):** [https://openweathermap.org/current](https://openweathermap.org/current)
*   **Sign-up for an API Key:** You'll typically need to sign up for a free account to get an API key (APPID). This key is used to authenticate your requests to their server.
    *   Go to their website and look for a "Sign Up" or "API" section.
    *   The free tier usually allows a generous number of requests per day, sufficient for development and small projects.
    *   **Important:** Once you get your API key, keep it safe. For a real deployed application, you wouldn't embed it directly in client-side JavaScript for security reasons (you'd use a backend proxy). But for this local learning project, we'll use it directly. **Do NOT commit your actual API key to public Git repositories.**

**Example API Request URL (Current Weather Data):**
`https://api.openweathermap.org/data/2.5/weather?q={city name}&appid={API key}&units=metric`

*   `q={city name}`: The city you're searching for (e.g., `q=London`).
*   `appid={API key}`: Your unique API key.
*   `units=metric`: To get temperature in Celsius (use `imperial` for Fahrenheit).

**Example JSON Response (Simplified):**
```json
{
  "coord": { "lon": -0.1257, "lat": 51.5085 },
  "weather": [
    {
      "id": 800,
      "main": "Clear",
      "description": "clear sky",
      "icon": "01d"
    }
  ],
  "main": {
    "temp": 15.5,
    "feels_like": 14.9,
    "temp_min": 14.0,
    "temp_max": 17.0,
    "pressure": 1012,
    "humidity": 60
  },
  "wind": { "speed": 3.5, "deg": 240 },
  "name": "London",
  "cod": 200 // HTTP status code from within the JSON
}
```
We'll be interested in `name`, `weather[0].description`, `weather[0].icon`, `main.temp`, `main.humidity`, and `wind.speed`.

**Note:** For this book, since I cannot perform the actual sign-up, I will use a placeholder for the API key. You will need to replace `"YOUR_API_KEY"` with your actual key from OpenWeatherMap for the code to work.

### 10.3 Making API Requests with Fetch / 10.4 Displaying Weather Information / 10.5 Handling API Errors and Loading States / 10.6 Step-by-Step Implementation Guide

Let's set up the HTML and then the JavaScript.

**HTML Structure (`index.html` in `Project3_WeatherDashboard` folder):**

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Weather Dashboard</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container">
        <h1>Weather Dashboard</h1>

        <div class="search-container">
            <input type="text" id="cityInput" placeholder="Enter city name...">
            <button id="searchButton">Search</button>
        </div>

        <div id="weatherInfoContainer" class="weather-info hide">
            <h2 id="cityName">City Name</h2>
            <div class="main-weather">
                <img id="weatherIcon" src="" alt="Weather icon">
                <p id="temperature">--°C</p>
            </div>
            <p id="weatherDescription">Description</p>
            <div class="details">
                <p>Humidity: <span id="humidity">--</span>%</p>
                <p>Wind Speed: <span id="windSpeed">--</span> m/s</p>
            </div>
        </div>

        <div id="loadingIndicator" class="loading hide">Loading...</div>
        <div id="errorMessage" class="error-message hide">Error message here.</div>
    </div>

    <script src="script.js"></script>
</body>
</html>
```

**CSS Styling (`style.css`):**

```css
body {
    font-family: Arial, sans-serif;
    background-color: #87ceeb; /* Sky blue */
    color: #333;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    margin: 0;
    padding: 20px;
}

.container {
    background-color: #ffffff;
    padding: 25px 30px;
    border-radius: 10px;
    box-shadow: 0 5px 15px rgba(0,0,0,0.2);
    width: 100%;
    max-width: 450px;
    text-align: center;
}

h1 {
    color: #005073; /* Darker blue */
    margin-bottom: 25px;
}

.search-container {
    display: flex;
    margin-bottom: 25px;
}

#cityInput {
    flex-grow: 1;
    padding: 12px;
    border: 1px solid #ccc;
    border-radius: 5px 0 0 5px;
    font-size: 1em;
}

#searchButton {
    padding: 12px 18px;
    background-color: #1e90ff; /* Dodger blue */
    color: white;
    border: none;
    border-radius: 0 5px 5px 0;
    cursor: pointer;
    font-size: 1em;
    transition: background-color 0.2s ease;
}

#searchButton:hover {
    background-color: #0073e6;
}

.weather-info {
    margin-top: 20px;
    padding: 20px;
    border: 1px solid #eee;
    border-radius: 8px;
    background-color: #f9f9f9;
}

#cityName {
    color: #333;
    margin-bottom: 15px;
    font-size: 1.8em;
}

.main-weather {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 10px;
}

#weatherIcon {
    width: 80px; /* Adjust size as needed */
    height: 80px;
    margin-right: 15px;
    background-color: #ddd; /* Placeholder background */
    border-radius: 50%;
}

#temperature {
    font-size: 2.8em;
    font-weight: bold;
    color: #005073;
    margin: 0;
}

#weatherDescription {
    font-size: 1.2em;
    color: #555;
    text-transform: capitalize;
    margin-bottom: 15px;
}

.details p {
    margin: 8px 0;
    font-size: 1em;
    color: #444;
}

.details span {
    font-weight: bold;
}

.loading, .error-message {
    margin-top: 20px;
    font-size: 1.1em;
    padding: 10px;
    border-radius: 5px;
}

.loading {
    color: #0073e6;
}

.error-message {
    color: #d32f2f; /* Red */
    background-color: #ffcdd2; /* Light red */
    border: 1px solid #d32f2f;
}

.hide {
    display: none !important;
}
```

**JavaScript Logic (`script.js`):**

```javascript
// script.js

// --- DOM Elements ---
const cityInput = document.getElementById("cityInput");
const searchButton = document.getElementById("searchButton");
const weatherInfoContainer = document.getElementById("weatherInfoContainer");
const cityNameElement = document.getElementById("cityName");
const weatherIconElement = document.getElementById("weatherIcon");
const temperatureElement = document.getElementById("temperature");
const weatherDescriptionElement = document.getElementById("weatherDescription");
const humidityElement = document.getElementById("humidity");
const windSpeedElement = document.getElementById("windSpeed");
const loadingIndicator = document.getElementById("loadingIndicator");
const errorMessageElement = document.getElementById("errorMessage");

// --- API Configuration ---
const API_KEY = "YOUR_API_KEY"; // IMPORTANT: Replace with your OpenWeatherMap API key!
const API_BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
const ICON_BASE_URL = "https://openweathermap.org/img/wn/"; // Base URL for weather icons

// --- Functions ---

/**
 * Fetches weather data for a given city.
 * @param {string} city - The name of the city.
 */
async function fetchWeatherData(city) {
    if (API_KEY === "YOUR_API_KEY") {
        displayError("Please replace 'YOUR_API_KEY' with your actual OpenWeatherMap API key in script.js.");
        return;
    }

    showLoading(true);
    hideError();
    weatherInfoContainer.classList.add("hide"); // Hide previous results

    const url = `${API_BASE_URL}?q=${encodeURIComponent(city)}&appid=${API_KEY}&units=metric`;

    try {
        const response = await fetch(url);

        if (!response.ok) {
            // If response is not ok (e.g., 404 Not Found, 401 Unauthorized)
            const errorData = await response.json().catch(() => ({ message: response.statusText })); // Try to parse error, fallback to statusText
            throw new Error(`City not found or API error: ${errorData.message || response.status}`);
        }

        const data = await response.json();
        console.log("Weather data:", data);
        displayWeatherData(data);
        saveLastCity(city); // Optional: save last searched city

    } catch (error) {
        console.error("Error fetching weather data:", error);
        displayError(error.message);
    } finally {
        showLoading(false);
    }
}

/**
 * Displays the fetched weather data on the page.
 * @param {object} data - The weather data object from the API.
 */
function displayWeatherData(data) {
    if (!data || data.cod !== 200) { // Check for OpenWeatherMap specific error code in data
        displayError(data.message || "Could not retrieve weather data for the city.");
        return;
    }

    cityNameElement.textContent = data.name;
    temperatureElement.textContent = `${Math.round(data.main.temp)}°C`;
    weatherDescriptionElement.textContent = data.weather[0].description;
    humidityElement.textContent = data.main.humidity;
    windSpeedElement.textContent = data.wind.speed.toFixed(1); // One decimal place

    // Set weather icon
    const iconCode = data.weather[0].icon;
    weatherIconElement.src = `${ICON_BASE_URL}${iconCode}@2x.png`; // @2x for a larger icon
    weatherIconElement.alt = data.weather[0].description;
    weatherIconElement.classList.remove("hide"); // Show icon if it was hidden

    weatherInfoContainer.classList.remove("hide"); // Show the weather info
    hideError();
}

/**
 * Shows or hides the loading indicator.
 * @param {boolean} isLoading - True to show, false to hide.
 */
function showLoading(isLoading) {
    if (isLoading) {
        loadingIndicator.classList.remove("hide");
    } else {
        loadingIndicator.classList.add("hide");
    }
}

/**
 * Displays an error message.
 * @param {string} message - The error message to display.
 */
function displayError(message) {
    errorMessageElement.textContent = message;
    errorMessageElement.classList.remove("hide");
    weatherInfoContainer.classList.add("hide"); // Hide weather info on error
}

/**
 * Hides the error message.
 */
function hideError() {
    errorMessageElement.classList.add("hide");
    errorMessageElement.textContent = "";
}

/**
 * Handles the search button click or Enter key press in input.
 */
function handleSearch() {
    const city = cityInput.value.trim();
    if (city) {
        fetchWeatherData(city);
    } else {
        displayError("Please enter a city name.");
    }
}

// --- (Optional) Local Storage for Last Searched City ---
const LAST_CITY_KEY = "weatherApp.lastCity";

function saveLastCity(city) {
    localStorage.setItem(LAST_CITY_KEY, city);
}

function loadLastCity() {
    const lastCity = localStorage.getItem(LAST_CITY_KEY);
    if (lastCity) {
        cityInput.value = lastCity; // Pre-fill input
        fetchWeatherData(lastCity); // Automatically fetch weather for it
    }
}


// --- Event Listeners ---
searchButton.addEventListener("click", handleSearch);

cityInput.addEventListener("keypress", function(event) {
    if (event.key === "Enter") {
        handleSearch();
    }
});

// --- Initial Load ---
// loadLastCity(); // Optional: Load weather for the last searched city on page load
// If you don't want to auto-load, you can just prefill the input:
const lastCity = localStorage.getItem(LAST_CITY_KEY);
if (lastCity) {
    cityInput.value = lastCity;
}
// Ensure default state is clean
weatherInfoContainer.classList.add("hide");
loadingIndicator.classList.add("hide");
errorMessageElement.classList.add("hide");
weatherIconElement.src = ""; // Clear any previous icon
```

**JavaScript Breakdown:**

1.  **DOM Elements & API Config:** Select elements and set up API key/URLs. **Remember to replace `"YOUR_API_KEY"`!**
2.  **`fetchWeatherData(city)` (async function):**
    *   Checks if the API key placeholder is still there.
    *   Shows a loading indicator, hides previous errors/results.
    *   Constructs the API URL using `encodeURIComponent(city)` to safely include city names with spaces or special characters.
    *   Uses `await fetch(url)` to make the request.
    *   **Error Handling:**
        *   `if (!response.ok)`: Checks if the HTTP response status is successful (200-299). If not (e.g., 404 for city not found, 401 for invalid API key), it tries to parse an error message from the JSON response or uses the `statusText`. It then `throw`s an `Error`. This error will be caught by the `catch` block.
        *   The `try...catch` block handles network errors or errors thrown manually.
    *   `const data = await response.json();`: If the response is ok, it parses the JSON body.
    *   Calls `displayWeatherData(data)`.
    *   Calls `saveLastCity(city)` (optional).
    *   `finally`: Hides the loading indicator regardless of success or failure.
3.  **`displayWeatherData(data)`:**
    *   Takes the parsed JSON data from the API.
    *   Checks `data.cod` (OpenWeatherMap's internal status code) to ensure the API itself didn't return an error within a 200 HTTP response.
    *   Updates the `textContent` or `src` of the respective HTML elements to show city name, temperature (rounded), description, humidity, wind speed, and the weather icon.
    *   The weather icon URL is constructed using `ICON_BASE_URL` and the `icon` code from the API data. `@2x.png` requests a larger version of the icon.
    *   Shows the `weatherInfoContainer` and hides any previous error.
4.  **`showLoading(isLoading)`:** Toggles the visibility of the loading indicator.
5.  **`displayError(message)`:** Shows an error message and hides weather info.
6.  **`hideError()`:** Hides the error message.
7.  **`handleSearch()`:** Called when the search button is clicked or Enter is pressed. Gets the city from the input and calls `fetchWeatherData()`.
8.  **Local Storage (Optional):**
    *   `LAST_CITY_KEY`: A key for `localStorage`.
    *   `saveLastCity(city)`: Saves the successfully searched city.
    *   `loadLastCity()`: If you want to automatically fetch weather for the last city on page load, call this function. The provided code in "Initial Load" just pre-fills the input.
9.  **Event Listeners:**
    *   For the search button's `click` event.
    *   For the city input's `keypress` event (specifically for "Enter").
10. **Initial Load:** Clears out any stale display elements. Optionally, you could uncomment `loadLastCity()` to fetch data for the last city on page load.

**Testing:**
*   **Replace `"YOUR_API_KEY"` in `script.js` with your actual OpenWeatherMap API key.**
*   Open `index.html` in your browser.
*   Enter a city name and click "Search" or press Enter.
*   Observe the loading indicator, then the weather information or an error message.
*   Test with invalid city names to see error handling.
*   Test the (optional) `localStorage` feature by refreshing the page or closing and reopening the tab.

### 10.7 Chapter Summary & Action Steps (Further Enhancements)

**Summary:**

*   We built a Weather Dashboard that fetches and displays real-time weather data from the OpenWeatherMap API.
*   Learned how to make API requests using `fetch()` and handle asynchronous responses with `async/await`.
*   Practiced error handling for network requests and API responses.
*   Dynamically updated the DOM to display fetched data, including images (weather icons).
*   Optionally used `localStorage` to remember the last searched city.

**Action Steps (Further Enhancements - Optional Challenges):**

1.  **Display More Data:**
    *   The OpenWeatherMap API provides more data (e.g., "feels like" temperature, min/max temperature, pressure, wind direction, sunrise/sunset times). Enhance the display to show some of these.
2.  **Unit Conversion:**
    *   Add buttons or a toggle to switch between Celsius and Fahrenheit for temperature. You'll need to make a new API request with `units=imperial` or perform the conversion mathematically.
3.  **5-Day Forecast (More Complex):**
    *   OpenWeatherMap also offers a 5-day forecast API (might be part of a different subscription or have different request limits).
    *   If accessible, try to fetch and display a simple 5-day forecast below the current weather. This will involve parsing a more complex JSON structure, likely an array of forecast objects.
4.  **Geolocation for Default Weather:**
    *   On page load, use the Geolocation API (Chapter 6) to get the user's current location.
    *   Then, use those coordinates to fetch the weather for their current location by default. OpenWeatherMap allows fetching by coordinates (`lat={lat}&lon={lon}`).
5.  **Improved UI/UX:**
    *   Enhance the visual design.
    *   Add smoother transitions or animations for when data loads or errors appear.
    *   Debounce the search input to avoid making too many API requests if the user is typing quickly (more advanced concept).
6.  **Error Handling for API Key:** If the API key is invalid (often a 401 error), display a more specific message to the user about checking their API key configuration.

This project introduced you to the exciting world of consuming external APIs, a very common task in web development. Being able to fetch and display data from various sources opens up endless possibilities for the types of applications you can build. In the next part, we'll look at some more advanced JavaScript concepts and best practices.
