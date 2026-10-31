import { useEffect, useState } from 'react'
import { getAssignedIncidents } from '../services/incidentService'
import { errorMessage } from '../services/api'
import IncidentTable from '../components/IncidentTable'

export default function AssignedIncidents() {
  const [incidents, setIncidents] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getAssignedIncidents().then(setIncidents).catch((e) => setError(errorMessage(e)))
  }, [])

  return (
    <div className="card">
      <h2>Incidents qui me sont affectes</h2>
      {error && <div className="alert error">{error}</div>}
      {incidents ? <IncidentTable incidents={incidents} showCreator /> : !error && <p>Chargement...</p>}
    </div>
  )
}
