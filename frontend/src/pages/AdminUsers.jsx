import { useEffect, useState } from 'react'
import { getUsers, enableUser, disableUser } from '../services/incidentService'
import { errorMessage } from '../services/api'
import { formatDate } from '../components/Badges'

export default function AdminUsers() {
  const [users, setUsers] = useState([])
  const [error, setError] = useState('')

  const load = () =>
    getUsers()
      .then(setUsers)
      .catch((e) => setError(errorMessage(e)))

  useEffect(() => {
    load()
  }, [])

  const toggle = async (u) => {
    try {
      setError('')
      await (u.enabled ? disableUser(u.id) : enableUser(u.id))
      await load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <div className="card">
      <h2>Utilisateurs</h2>
      {error && <div className="alert error">{error}</div>}
      <table>
        <thead>
          <tr>
            <th>#</th>
            <th>Nom</th>
            <th>Email</th>
            <th>Role</th>
            <th>Compte</th>
            <th>Cree le</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.id}</td>
              <td>{u.fullName}</td>
              <td>{u.email}</td>
              <td>
                <span className="badge role">{u.role}</span>
              </td>
              <td>{u.enabled ? 'Actif' : 'Desactive'}</td>
              <td>{formatDate(u.createdAt)}</td>
              <td>
                <button className={`btn small ${u.enabled ? 'danger' : ''}`} onClick={() => toggle(u)}>
                  {u.enabled ? 'Desactiver' : 'Activer'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
