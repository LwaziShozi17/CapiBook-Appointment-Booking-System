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
      isActive ? 'text-white border-b-2 border-white pb-0.5' : 'text-white/80 hover:text-white'
    }`

  return (
    <nav className="sticky top-0 z-50 shadow-sm" style={{ background: 'linear-gradient(90deg, #0084d5 0%, #009de0 100%)' }}>
      <div className="max-w-6xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
        <Link to="/" className="text-xl font-semibold text-white hover:text-white/90 transition-colors tracking-tight">
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
              <span className="text-sm text-white/80">
                {user?.firstName} {user?.lastName}
              </span>
              <button
                onClick={handleLogout}
                className="text-sm font-medium text-white/80 hover:text-white transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-white/80 hover:text-white transition-colors"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="text-sm font-medium bg-white text-[#009de0] hover:bg-white/90 px-4 py-2 rounded-lg transition-colors"
              >
                Register
              </Link>
            </>
          )}
        </div>

        <button
          className="md:hidden p-2 text-white"
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
        <div className="md:hidden border-t border-white/20 px-4 py-3 flex flex-col gap-3" style={{ background: 'linear-gradient(90deg, #0084d5 0%, #009de0 100%)' }}>
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
                className="text-left text-sm font-medium text-white/80 hover:text-white"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium text-white/80 hover:text-white" onClick={() => setMenuOpen(false)}>
                Login
              </Link>
              <Link to="/register" className="text-sm font-medium text-white" onClick={() => setMenuOpen(false)}>
                Register
              </Link>
            </>
          )}
        </div>
      )}
    </nav>
  )
}
