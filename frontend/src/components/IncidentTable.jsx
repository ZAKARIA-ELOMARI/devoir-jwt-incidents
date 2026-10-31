import { Link } from 'react-router-dom'
import { StatusBadge, PriorityBadge, formatDate } from './Badges'

// Tableau reutilise par MyIncidents, AssignedIncidents et AdminIncidents
// `renderActions` (optionnel) permet d'ajouter une colonne d'actions.
export default function IncidentTable({ incidents, showCreator = false, showAssignee = false, renderActions }) {
  if (!incidents.length) {
    return <p className="muted">Aucun incident.</p>
  }
  return (
    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Titre</th>
          <th>Categorie</th>
          <th>Priorite</th>
          <th>Statut</th>
          {showCreator && <th>Declare par</th>}
          {showAssignee && <th>Technicien</th>}
          <th>Cree le</th>
          {renderActions && <th>Actions</th>}
        </tr>
      </thead>
      <tbody>
        {incidents.map((i) => (
          <tr key={i.id}>
            <td>{i.id}</td>
            <td>
              <Link to={`/incidents/${i.id}`}>{i.title}</Link>
            </td>
            <td>{i.category}</td>
            <td>
              <PriorityBadge value={i.priority} />
            </td>
            <td>
              <StatusBadge value={i.status} />
            </td>
            {showCreator && <td>{i.createdBy?.fullName}</td>}
            {showAssignee && <td>{i.assignedTo?.fullName || '-'}</td>}
            <td>{formatDate(i.createdAt)}</td>
            {renderActions && <td>{renderActions(i)}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
