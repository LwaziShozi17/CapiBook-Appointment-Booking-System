import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-medium transition-colors ${
      isActive ? 'text-purple-600' : 'text-gray-600 hover:text-purple-600'
    }`

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
        <Link to="/" className="text-xl font-semibold text-gray-900 hover:text-purple-600 transition-colors">
          CapiBook
        </Link>

        <div className="hidden md:flex items-center gap-6">
          {isAuthenticated && (
            <>
              <NavLink to="/book" className={navLinkClass}>
                Book Appointment
              </NavLink>
              <NavLink to="/appointments" className={navLinkClass}>
                My Appointments
              </NavLink>
              <NavLink to="/profile" className={navLinkClass}>
                Profile
              </NavLink>
            </>
          )}
        </div>

        <div className="hidden md:flex items-center gap-3">
          {isAuthenticated ? (
            <>
              <span className="text-sm text-gray-600">
                {user?.firstName} {user?.lastName}
              </span>
              <button
                onClick={handleLogout}
                className="text-sm font-medium text-gray-600 hover:text-red-600 transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-gray-600 hover:text-purple-600 transition-colors"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="text-sm font-medium bg-purple-600 hover:bg-purple-700 text-white px-4 py-2 rounded-lg transition-colors"
              >
                Register
              </Link>
            </>
          )}
        </div>

        <button
          className="md:hidden p-2 text-gray-600"
          onClick={() => setMenuOpen((o) => !o)}
          aria-label="Toggle menu"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {menuOpen ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>
      </div>

      {menuOpen && (
        <div className="md:hidden border-t border-gray-200 bg-white px-4 py-3 flex flex-col gap-3">
          {isAuthenticated ? (
            <>
              <NavLink to="/book" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                Book Appointment
              </NavLink>
              <NavLink to="/appointments" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                My Appointments
              </NavLink>
              <NavLink to="/profile" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                Profile
              </NavLink>
              <button
                onClick={() => { setMenuOpen(false); handleLogout() }}
                className="text-left text-sm font-medium text-red-600"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium text-gray-600" onClick={() => setMenuOpen(false)}>
                Login
              </Link>
              <Link to="/register" className="text-sm font-medium text-purple-600" onClick={() => setMenuOpen(false)}>
                Register
              </Link>
            </>
          )}
        </div>
      )}
    </nav>
  )
}
