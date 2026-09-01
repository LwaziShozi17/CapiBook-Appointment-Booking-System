import { Navigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

interface Props {
  children: React.ReactNode
  requireRole?: 'BRANCH_ADMIN' | 'SYSTEM_ADMIN'
}

export default function AdminRoute({ children, requireRole }: Props) {
  const { user, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (!user) return <Navigate to="/login" replace />

  const isAdmin = user.role === 'BRANCH_ADMIN' || user.role === 'SYSTEM_ADMIN'
  if (!isAdmin) return <Navigate to="/" replace />

  if (requireRole && user.role !== requireRole) return <Navigate to="/admin" replace />

  return <>{children}</>
}
