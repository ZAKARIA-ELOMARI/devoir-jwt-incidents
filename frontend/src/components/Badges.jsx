export const StatusBadge = ({ value }) => <span className={`badge status-${value}`}>{value}</span>
export const PriorityBadge = ({ value }) => <span className={`badge prio-${value}`}>{value}</span>

export const formatDate = (iso) =>
  iso
    ? new Date(iso).toLocaleString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : '-'
