import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const NAV_ITEMS = [
  { to: '/admin', label: 'Dashboard', end: true, roles: ['BRANCH_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/admin/appointments', label: 'Appointments', roles: ['BRANCH_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/admin/branches', label: 'Branches', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/services', label: 'Services', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/users', label: 'Users', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/analytics', label: 'Analytics', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/audit', label: 'Audit Logs', roles: ['SYSTEM_ADMIN'] },
]

export default function AdminLayout() {
  const { user } = useAuth()

  const visibleItems = NAV_ITEMS.filter((item) => user && item.roles.includes(user.role))

  return (
    <div className="flex min-h-screen bg-gray-50">
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col">
        <div className="px-5 py-4 border-b border-gray-100">
          <p className="text-xs font-semibold text-gray-400 uppercase tracking-wide">Admin Panel</p>
          <p className="text-sm font-medium text-gray-700 mt-0.5 truncate">
            {user?.firstName} {user?.lastName}
          </p>
          <span className="inline-flex mt-1 px-1.5 py-0.5 rounded text-xs font-medium bg-purple-100 text-purple-700">
            {user?.role?.replace('_', ' ')}
          </span>
        </div>
        <nav className="flex-1 py-3">
          {visibleItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `block px-5 py-2.5 text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-purple-50 text-purple-700 border-r-2 border-purple-600'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="px-5 py-4 border-t border-gray-100">
          <NavLink
            to="/"
            className="text-sm text-gray-500 hover:text-gray-700 transition-colors"
          >
            ← Back to App
          </NavLink>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
