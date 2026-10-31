import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../services/authService'
import { errorMessage } from '../services/api'

export default function Register() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [error, setError] = useState('')

  const change = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await register(form.fullName, form.email, form.password)
      navigate('/login?registered=1')
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  return (
    <div className="auth-box card">
      <h2>Creer un compte</h2>
      <p className="muted">Votre compte aura automatiquement le role EMPLOYEE.</p>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <label>Nom complet</label>
        <input name="fullName" value={form.fullName} onChange={change} required />
        <label>Email</label>
        <input name="email" type="email" value={form.email} onChange={change} required />
        <label>Mot de passe (6 caracteres minimum)</label>
        <input name="password" type="password" value={form.password} onChange={change} required minLength={6} />
        <button className="btn">S'inscrire</button>
      </form>
      <p className="muted">
        Deja un compte ? <Link to="/login">Se connecter</Link>
      </p>
    </div>
  )
}
