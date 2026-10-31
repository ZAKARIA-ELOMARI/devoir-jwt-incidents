import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getProfile, updateProfile, changePassword } from '../services/incidentService'
import { errorMessage } from '../services/api'

export default function Profile() {
  const { refreshSession } = useAuth()
  const [profile, setProfile] = useState({ fullName: '', email: '' })
  const [role, setRole] = useState('')
  const [msg, setMsg] = useState({ type: '', text: '' })
  const [pwd, setPwd] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [pwdMsg, setPwdMsg] = useState({ type: '', text: '' })

  useEffect(() => {
    getProfile()
      .then((p) => {
        setProfile({ fullName: p.fullName, email: p.email })
        setRole(p.role)
      })
      .catch((e) => setMsg({ type: 'error', text: errorMessage(e) }))
  }, [])

  const saveProfile = async (e) => {
    e.preventDefault()
    try {
      const res = await updateProfile(profile.fullName, profile.email)
      refreshSession(res.token, res.user)
      setMsg({ type: 'success', text: 'Profil mis a jour.' })
    } catch (err) {
      setMsg({ type: 'error', text: errorMessage(err) })
    }
  }

  const savePassword = async (e) => {
    e.preventDefault()
    try {
      await changePassword(pwd.oldPassword, pwd.newPassword, pwd.confirmPassword)
      setPwd({ oldPassword: '', newPassword: '', confirmPassword: '' })
      setPwdMsg({ type: 'success', text: 'Mot de passe modifie.' })
    } catch (err) {
      setPwdMsg({ type: 'error', text: errorMessage(err) })
    }
  }

  return (
    <div className="two-cols">
      <div className="card">
        <h3>Mon profil</h3>
        <p className="muted">Role : {role}</p>
        {msg.text && <div className={`alert ${msg.type}`}>{msg.text}</div>}
        <form onSubmit={saveProfile}>
          <label>Nom complet</label>
          <input value={profile.fullName} onChange={(e) => setProfile({ ...profile, fullName: e.target.value })} required />
          <label>Email</label>
          <input type="email" value={profile.email} onChange={(e) => setProfile({ ...profile, email: e.target.value })} required />
          <button className="btn">Enregistrer</button>
        </form>
      </div>

      <div className="card">
        <h3>Changer le mot de passe</h3>
        {pwdMsg.text && <div className={`alert ${pwdMsg.type}`}>{pwdMsg.text}</div>}
        <form onSubmit={savePassword}>
          <label>Ancien mot de passe</label>
          <input type="password" value={pwd.oldPassword} onChange={(e) => setPwd({ ...pwd, oldPassword: e.target.value })} required />
          <label>Nouveau mot de passe</label>
          <input type="password" value={pwd.newPassword} onChange={(e) => setPwd({ ...pwd, newPassword: e.target.value })} required minLength={6} />
          <label>Confirmation</label>
          <input type="password" value={pwd.confirmPassword} onChange={(e) => setPwd({ ...pwd, confirmPassword: e.target.value })} required />
          <button className="btn">Modifier</button>
        </form>
      </div>
    </div>
  )
}
