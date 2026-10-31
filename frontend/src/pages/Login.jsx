import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { errorMessage } from '../services/api'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate('/dashboard')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-box card">
      <h2>Connexion</h2>
      {params.get('expired') && (
        <div className="alert warning">Votre session a expire. Veuillez vous reconnecter.</div>
      )}
      {params.get('registered') && (
        <div className="alert success">Compte cree. Vous pouvez vous connecter.</div>
      )}
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={handleSubmit}>
        <label>Email</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label>Mot de passe</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="btn" disabled={loading}>
          {loading ? 'Connexion...' : 'Se connecter'}
        </button>
      </form>
      <p className="muted">
        Pas de compte ? <Link to="/register">Creer un compte</Link>
      </p>
    </div>
  )
}
