import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createIncident } from '../services/incidentService'
import { errorMessage } from '../services/api'

const CATEGORIES = ['NETWORK', 'MALWARE', 'PHISHING', 'ACCESS', 'SOFTWARE', 'HARDWARE', 'DATA_LEAK', 'OTHER']
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

export default function NewIncident() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', category: 'OTHER', priority: 'MEDIUM' })
  const [error, setError] = useState('')

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      const created = await createIncident(form)
      navigate(`/incidents/${created.id}`)
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div className="card narrow">
      <h2>Declarer un incident</h2>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <label>Titre</label>
        <input name="title" value={form.title} onChange={change} maxLength={150} required />
        <label>Description</label>
        <textarea name="description" rows={5} value={form.description} onChange={change} required />
        <label>Categorie</label>
        <select name="category" value={form.category} onChange={change}>
          {CATEGORIES.map((c) => (
            <option key={c}>{c}</option>
          ))}
        </select>
        <label>Priorite</label>
        <select name="priority" value={form.priority} onChange={change}>
          {PRIORITIES.map((p) => (
            <option key={p}>{p}</option>
          ))}
        </select>
        <button className="btn">Envoyer</button>
      </form>
    </div>
  )
}
