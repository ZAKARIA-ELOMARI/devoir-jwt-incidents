import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  getIncident,
  updateStatus,
  addComment,
  assignIncident,
  getUsers,
} from '../services/incidentService'
import { errorMessage } from '../services/api'
import { StatusBadge, PriorityBadge, formatDate } from '../components/Badges'

export default function IncidentDetails() {
  const { id } = useParams()
  const { user } = useAuth()
  const [incident, setIncident] = useState(null)
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [technicians, setTechnicians] = useState([])
  const [selectedTech, setSelectedTech] = useState('')
  const [note, setNote] = useState('')

  const load = useCallback(async () => {
    try {
      setError('')
      setIncident(await getIncident(id))
    } catch (e) {
      setIncident(null)
      setError(errorMessage(e))
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (user.role === 'ADMIN') {
      getUsers()
        .then((users) => setTechnicians(users.filter((u) => u.role === 'TECHNICIAN' && u.enabled)))
        .catch(() => {})
    }
  }, [user.role])

  const run = async (action, okMessage) => {
    try {
      setInfo('')
      await action()
      setInfo(okMessage)
      await load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  if (error && !incident) {
    return (
      <div className="card">
        <div className="alert error">{error}</div>
        <Link to="/dashboard">Retour</Link>
      </div>
    )
  }
  if (!incident) return <p>Chargement...</p>

  const closed = incident.status === 'CLOSED'
  const canChangeStatus = (user.role === 'TECHNICIAN' || user.role === 'ADMIN') && !closed
  const statusOptions =
    user.role === 'ADMIN' ? ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'] : ['IN_PROGRESS', 'RESOLVED']
  const canComment = user.role === 'TECHNICIAN' || user.role === 'ADMIN'

  return (
    <div>
      <div className="card">
        <div className="row between">
          <h2>
            #{incident.id} - {incident.title}
          </h2>
          <div>
            <PriorityBadge value={incident.priority} /> <StatusBadge value={incident.status} />
          </div>
        </div>
        {error && <div className="alert error">{error}</div>}
        {info && <div className="alert success">{info}</div>}
        <p className="pre">{incident.description}</p>
        <dl className="meta">
          <dt>Categorie</dt>
          <dd>{incident.category}</dd>
          <dt>Declare par</dt>
          <dd>{incident.createdBy?.fullName}</dd>
          <dt>Technicien</dt>
          <dd>{incident.assignedTo?.fullName || 'Non affecte'}</dd>
          <dt>Cree le</dt>
          <dd>{formatDate(incident.createdAt)}</dd>
          <dt>Mis a jour</dt>
          <dd>{formatDate(incident.updatedAt)}</dd>
        </dl>

        {user.role === 'ADMIN' && !closed && (
          <div className="row">
            <select value={selectedTech} onChange={(e) => setSelectedTech(e.target.value)}>
              <option value="">-- Choisir un technicien --</option>
              {technicians.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.fullName}
                </option>
              ))}
            </select>
            <button
              className="btn"
              disabled={!selectedTech}
              onClick={() => run(() => assignIncident(incident.id, selectedTech), 'Incident affecte.')}
            >
              Affecter
            </button>
          </div>
        )}

        {canChangeStatus && (
          <div className="row">
            <span>Changer le statut :</span>
            {statusOptions
              .filter((s) => s !== incident.status)
              .map((s) => (
                <button
                  key={s}
                  className="btn small"
                  onClick={() => run(() => updateStatus(incident.id, s), `Statut : ${s}`)}
                >
                  {s}
                </button>
              ))}
          </div>
        )}
        {closed && <p className="muted">Incident CLOSED : le statut ne peut plus etre modifie.</p>}
      </div>

      <div className="two-cols">
        <div className="card">
          <h3>Notes de traitement</h3>
          {incident.comments.length === 0 && <p className="muted">Aucune note.</p>}
          <ul className="timeline">
            {incident.comments.map((c) => (
              <li key={c.id}>
                <strong>{c.author?.fullName}</strong> <span className="muted">{formatDate(c.createdAt)}</span>
                <div className="pre">{c.content}</div>
              </li>
            ))}
          </ul>
          {canComment && (
            <form
              onSubmit={(e) => {
                e.preventDefault()
                run(async () => {
                  await addComment(incident.id, note)
                  setNote('')
                }, 'Note ajoutee.')
              }}
            >
              <textarea rows={3} placeholder="Ajouter une note de traitement..." value={note} onChange={(e) => setNote(e.target.value)} required />
              <button className="btn">Ajouter la note</button>
            </form>
          )}
        </div>

        <div className="card">
          <h3>Historique</h3>
          <ul className="timeline">
            {incident.history.map((h) => (
              <li key={h.id}>
                <span className="muted">{formatDate(h.createdAt)}</span> : {h.message}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
