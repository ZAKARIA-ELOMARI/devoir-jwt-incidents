import { Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Limite l'acces a certains roles : <RoleRoute roles={['ADMIN']} />
export default function RoleRoute({ roles }) {
  const { user } = useAuth()

  if (!user || !roles.includes(user.role)) {
    return (
      <div className="card">
        <h2>Acces refuse (403)</h2>
        <p>Votre role ({user?.role}) ne permet pas d'acceder a cette page.</p>
      </div>
    )
  }
  return <Outlet />
}
