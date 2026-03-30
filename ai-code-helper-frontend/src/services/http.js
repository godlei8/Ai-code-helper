import axios from 'axios'

export const apiBaseUrl =
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000
})

export function buildSseUrl(url, params = {}) {
  return http.getUri({
    url,
    params
  })
}
