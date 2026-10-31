import axios from 'axios'
import api, { API_URL, clearSession } from './api'

export function saveSession(data) {
  localStorage.setItem('token', data.token)
  if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken)
  localStorage.setItem(
    'user',
    JSON.stringify({ fullName: data.fullName, role: data.role, email: data.email }),
  )
}

export async function login(email, password) {
  const { data } = await api.post('/auth/login', { email, password })
  saveSession(data)
  return data
}

export async function register(fullName, email, password) {
  const { data } = await api.post('/auth/register', { fullName, email, password })
  return data
}

export async function logout() {
  const refreshToken = localStorage.getItem('refreshToken')
  try {
    if (refreshToken) await axios.post(`${API_URL}/auth/logout`, { refreshToken })
  } catch (e) {
    // on se deconnecte localement quoi qu'il arrive
  }
  clearSession()
}

export function getToken() {
  return localStorage.getItem('token')
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem('user'))
  } catch (e) {
    return null
  }
}

// Lit la date d'expiration (claim "exp") dans le payload du JWT (partie centrale en Base64)
export function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.exp * 1000 < Date.now()
  } catch (e) {
    return true
  }
}
