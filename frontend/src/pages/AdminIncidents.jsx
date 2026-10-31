import { useEffect, useState } from 'react'
import { getAllIncidents, getUsers, assignIncident } from '../services/incidentService'
import { errorMessage } from '../services/api'
import IncidentTable from '../components/IncidentTable'

function AssignCell({ incident, technicians, onAssign }) {
  const [tech, setTech] = useState('')
  if (incident.status === 'CLOSED') return <span className="muted">-</span>
  return (
    <div className="row">
      <select value={tech} onChange={(e) => setTech(e.target.value)}>
        <option value="">Technicien...</option>
        {technicians.map((t) => (
          <option key={t.id} value={t.id}>
            {t.fullName}
          </option>
        ))}
      </select>
      <button className="btn small" disabled={!tech} onClick={() => onAssign(incident.id, tech)}>
        Affecter
      </button>
    </div>
  )
}

export default function AdminIncidents() {
  const [incidents, setIncidents] = useState(null)
  const [technicians, setTechnicians] = useState([])
  const [error, setError] = useState('')

  const load = () =>
    getAllIncidents()
      .then(setIncidents)
      .catch((e) => setError(errorMessage(e)))

  useEffect(() => {
    load()
    getUsers()
      .then((users) => setTechnicians(users.filter((u) => u.role === 'TECHNICIAN' && u.enabled)))
      .catch((e) => setError(errorMessage(e)))
  }, [])

  const assign = async (incidentId, technicianId) => {
    try {
      setError('')
      await assignIncident(incidentId, technicianId)
      await load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <div className="card">
      <h2>Tous les incidents</h2>
      {error && <div className="alert error">{error}</div>}
      {incidents ? (
        <IncidentTable
          incidents={incidents}
          showCreator
          showAssignee
          renderActions={(i) => <AssignCell incident={i} technicians={technicians} onAssign={assign} />}
        />
      ) : (
        !error && <p>Chargement...</p>
      )}
    </div>
  )
}
