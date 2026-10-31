import axios from 'axios'

export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'
const ENABLE_REFRESH = import.meta.env.VITE_ENABLE_REFRESH === 'true'

const api = axios.create({
  baseURL: API_URL,
  headers: { 'Content-Type': 'application/json' },
})

export function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
}

// Supprime le JWT et renvoie vers la page Login avec le message "session expiree"
export function forceLogout() {
  clearSession()
  if (!window.location.pathname.startsWith('/login')) {
    window.location.href = '/login?expired=1'
  }
}

// 1) Interceptor de requete : ajoute automatiquement "Authorization: Bearer <JWT>"
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 2) Interceptor de reponse : 401 => token expire/invalide => retour au Login
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const isAuthCall = original?.url?.startsWith('/auth/')

    if (status === 401 && !isAuthCall) {
      // Option (section 23) : tenter de renouveler l'access token avec le refresh token
      const refreshToken = localStorage.getItem('refreshToken')
      if (ENABLE_REFRESH && refreshToken && !original._retry) {
        original._retry = true
        try {
          const { data } = await axios.post(`${API_URL}/auth/refresh`, { refreshToken })
          localStorage.setItem('token', data.token)
          original.headers.Authorization = `Bearer ${data.token}`
          return api(original)
        } catch (e) {
          // refresh impossible : on tombe dans le cas "session expiree"
        }
      }
      forceLogout()
    }
    return Promise.reject(error)
  },
)

// Message d'erreur lisible a partir de la reponse { status, message, errors }
export function errorMessage(error) {
  const data = error.response?.data
  if (data?.errors) {
    return Object.values(data.errors).join(' - ')
  }
  return data?.message || (error.request ? 'Serveur injoignable (backend demarre ?)' : error.message)
}

export default api
