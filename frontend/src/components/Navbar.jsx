import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <span className="brand">Incidents IT</span>
      <div className="links">
        {user && (
          <>
            <NavLink to="/dashboard">Dashboard</NavLink>
            {user.role === 'EMPLOYEE' && (
              <>
                <NavLink to="/incidents/my">Mes incidents</NavLink>
                <NavLink to="/incidents/new">Nouvel incident</NavLink>
              </>
            )}
            {user.role === 'TECHNICIAN' && <NavLink to="/incidents/assigned">Incidents affectes</NavLink>}
            {user.role === 'ADMIN' && (
              <>
                <NavLink to="/admin/incidents">Incidents</NavLink>
                <NavLink to="/admin/users">Utilisateurs</NavLink>
              </>
            )}
            <NavLink to="/profile">Profil</NavLink>
          </>
        )}
      </div>
      {user && (
        <div className="user">
          <span>
            {user.fullName} <span className="badge role">{user.role}</span>
          </span>
          <button className="btn small" onClick={handleLogout}>
            Deconnexion
          </button>
        </div>
      )}
    </nav>
  )
}
