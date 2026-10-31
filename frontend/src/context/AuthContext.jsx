import { createContext, useContext, useState } from 'react'
import * as authService from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(authService.getUser())

  const login = async (email, password) => {
    const data = await authService.login(email, password)
    setUser({ fullName: data.fullName, role: data.role, email: data.email })
    return data
  }

  const logout = async () => {
    await authService.logout()
    setUser(null)
  }

  // Apres modification du profil : nouveau token (l'email est dans le JWT)
  const refreshSession = (token, profile) => {
    localStorage.setItem('token', token)
    const updated = { fullName: profile.fullName, role: profile.role, email: profile.email }
    localStorage.setItem('user', JSON.stringify(updated))
    setUser(updated)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, refreshSession }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
