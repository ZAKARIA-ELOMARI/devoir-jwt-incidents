import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getMyIncidents, getAssignedIncidents, getStatistics } from '../services/incidentService'
import { errorMessage } from '../services/api'
import IncidentTable from '../components/IncidentTable'

const Stat = ({ label, value, tone }) => (
  <div className={`stat ${tone || ''}`}>
    <div className="value">{value}</div>
    <div className="label">{label}</div>
  </div>
)

const count = (list, ...statuses) => list.filter((i) => statuses.includes(i.status)).length

function EmployeeDashboard() {
  const [incidents, setIncidents] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getMyIncidents().then(setIncidents).catch((e) => setError(errorMessage(e)))
  }, [])

  if (error) return <div className="alert error">{error}</div>
  if (!incidents) return <p>Chargement...</p>

  return (
    <>
      <div className="stats">
        <Stat label="Mes incidents" value={incidents.length} />
        <Stat label="OPEN" value={count(incidents, 'OPEN')} tone="blue" />
        <Stat label="IN_PROGRESS" value={count(incidents, 'IN_PROGRESS')} tone="orange" />
        <Stat label="RESOLVED / CLOSED" value={count(incidents, 'RESOLVED', 'CLOSED')} tone="green" />
      </div>
      <div className="card">
        <h3>Mes derniers incidents</h3>
        <IncidentTable incidents={incidents.slice(0, 5)} showAssignee />
      </div>
    </>
  )
}

function TechnicianDashboard() {
  const [incidents, setIncidents] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getAssignedIncidents().then(setIncidents).catch((e) => setError(errorMessage(e)))
  }, [])

  if (error) return <div className="alert error">{error}</div>
  if (!incidents) return <p>Chargement...</p>

  return (
    <>
      <div className="stats">
        <Stat label="Incidents affectes" value={incidents.length} />
        <Stat label="Critiques" value={incidents.filter((i) => i.priority === 'CRITICAL').length} tone="red" />
        <Stat label="En cours" value={count(incidents, 'IN_PROGRESS')} tone="orange" />
        <Stat label="Resolus" value={count(incidents, 'RESOLVED', 'CLOSED')} tone="green" />
      </div>
      <div className="card">
        <h3>Derniers incidents affectes</h3>
        <IncidentTable incidents={incidents.slice(0, 5)} showCreator />
      </div>
    </>
  )
}

function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getStatistics().then(setStats).catch((e) => setError(errorMessage(e)))
  }, [])

  if (error) return <div className="alert error">{error}</div>
  if (!stats) return <p>Chargement...</p>

  return (
    <>
      <h3>Utilisateurs</h3>
      <div className="stats">
        <Stat label="Total utilisateurs" value={stats.totalUsers} />
        <Stat label="Employes" value={stats.employees} />
        <Stat label="Techniciens" value={stats.technicians} />
      </div>
      <h3>Incidents</h3>
      <div className="stats">
        <Stat label="Total incidents" value={stats.totalIncidents} />
        <Stat label="OPEN" value={stats.open} tone="blue" />
        <Stat label="IN_PROGRESS" value={stats.inProgress} tone="orange" />
        <Stat label="CLOSED" value={stats.closed} tone="green" />
        <Stat label="Critiques" value={stats.critical} tone="red" />
      </div>
    </>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  return (
    <div>
      <h2>Bonjour {user.fullName}</h2>
      {user.role === 'EMPLOYEE' && <EmployeeDashboard />}
      {user.role === 'TECHNICIAN' && <TechnicianDashboard />}
      {user.role === 'ADMIN' && <AdminDashboard />}
    </div>
  )
}
