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
    <div className="flex min-h-screen bg-[#f5f5f5]">
      <aside className="w-56 bg-white border-r border-[#e1e1e1] flex flex-col">
        <div className="px-5 py-4 border-b border-[#e1e1e1]" style={{ background: 'linear-gradient(90deg, #003a5a 0%, #00486d 100%)' }}>
          <p className="text-xs font-semibold text-white/60 uppercase tracking-wide">Admin Panel</p>
          <p className="text-sm font-semibold text-white mt-0.5 truncate">
            {user?.firstName} {user?.lastName}
          </p>
          <span className="inline-flex mt-1 px-1.5 py-0.5 rounded text-xs font-medium bg-white/20 text-white">
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
                    ? 'bg-[#f2fafd] text-[#009de0] border-r-2 border-[#009de0]'
                    : 'text-[#7c7c7c] hover:bg-[#f8f8f9] hover:text-[#383634]'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="px-5 py-4 border-t border-[#e1e1e1]">
          <NavLink
            to="/"
            className="text-sm text-[#7c7c7c] hover:text-[#383634] transition-colors"
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
