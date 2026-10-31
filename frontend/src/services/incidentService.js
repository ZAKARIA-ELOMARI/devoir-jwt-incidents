import api from './api'

export const createIncident = (incident) => api.post('/api/incidents', incident).then((r) => r.data)
export const getMyIncidents = () => api.get('/api/incidents/my').then((r) => r.data)
export const getAssignedIncidents = () => api.get('/api/incidents/assigned').then((r) => r.data)
export const getAllIncidents = () => api.get('/api/incidents').then((r) => r.data)
export const getIncident = (id) => api.get(`/api/incidents/${id}`).then((r) => r.data)
export const updateStatus = (id, status) =>
  api.put(`/api/incidents/${id}/status`, { status }).then((r) => r.data)
export const assignIncident = (id, technicianId) =>
  api.put(`/api/incidents/${id}/assign/${technicianId}`).then((r) => r.data)
export const addComment = (id, content) =>
  api.post(`/api/incidents/${id}/comments`, { content }).then((r) => r.data)

// Administration
export const getUsers = () => api.get('/api/admin/users').then((r) => r.data)
export const enableUser = (id) => api.put(`/api/admin/users/${id}/enable`).then((r) => r.data)
export const disableUser = (id) => api.put(`/api/admin/users/${id}/disable`).then((r) => r.data)
export const getStatistics = () => api.get('/api/admin/statistics').then((r) => r.data)

// Profil
export const getProfile = () => api.get('/api/profile').then((r) => r.data)
export const updateProfile = (fullName, email) =>
  api.put('/api/profile', { fullName, email }).then((r) => r.data)
export const changePassword = (oldPassword, newPassword, confirmPassword) =>
  api.put('/api/profile/password', { oldPassword, newPassword, confirmPassword }).then((r) => r.data)
