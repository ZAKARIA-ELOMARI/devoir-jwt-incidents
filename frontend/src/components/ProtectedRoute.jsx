import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { getToken, isTokenExpired } from '../services/authService'
import { forceLogout } from '../services/api'

// Empeche un utilisateur non authentifie (ou dont le JWT a expire) d'acceder aux pages protegees.
// NB : c'est du confort d'utilisation ; la vraie securite est assuree par le backend.
export default function ProtectedRoute() {
  const { user } = useAuth()
  const token = getToken()

  if (!user || !token) {
    return <Navigate to="/login" replace />
  }
  if (isTokenExpired(token)) {
    // supprime le JWT et recharge /login?expired=1 (message "session expiree")
    forceLogout()
    return null
  }
  return <Outlet />
}
