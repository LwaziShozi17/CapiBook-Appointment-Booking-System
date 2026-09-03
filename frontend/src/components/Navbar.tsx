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
    `text-sm font-medium transition-colors px-3 py-1.5 rounded-md ${
      isActive
        ? 'bg-white/15 text-white'
        : 'text-white/70 hover:text-white hover:bg-white/10'
    }`

  return (
    <nav className="sticky top-0 z-50 bg-[#00486d] border-b border-white/10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
        <Link
          to="/"
          className="flex items-center gap-2 text-white hover:text-white/90 transition-colors"
        >
          <span className="w-8 h-8 bg-[#009de0] rounded-lg flex items-center justify-center flex-shrink-0">
            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </span>
          <span className="text-lg font-bold tracking-tight">CapiBook</span>
        </Link>

        <div className="hidden md:flex items-center gap-1">
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
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 bg-[#009de0] rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                  {user?.firstName?.[0]}{user?.lastName?.[0]}
                </div>
                <span className="text-sm text-white/70 font-medium">
                  {user?.firstName}
                </span>
              </div>
              <button
                onClick={handleLogout}
                className="text-sm font-medium text-white/60 hover:text-white transition-colors px-3 py-1.5 rounded-md hover:bg-white/10"
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-white/70 hover:text-white transition-colors px-3 py-1.5 rounded-md hover:bg-white/10"
              >
                Sign in
              </Link>
              <Link
                to="/register"
                className="text-sm font-semibold bg-[#009de0] hover:bg-[#0085c3] text-white px-4 py-2 rounded-lg transition-colors"
              >
                Register
              </Link>
            </>
          )}
        </div>

        <button
          className="md:hidden p-2 text-white/80 hover:text-white rounded-md"
          onClick={() => setMenuOpen((o) => !o)}
          aria-label="Toggle menu"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {menuOpen ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            )}
          </svg>
        </button>
      </div>

      {menuOpen && (
        <div className="md:hidden border-t border-white/10 px-4 py-3 flex flex-col gap-1">
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
                className="text-left text-sm font-medium text-white/60 hover:text-white px-3 py-1.5 rounded-md hover:bg-white/10 transition-colors"
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-white/70 hover:text-white px-3 py-1.5 rounded-md hover:bg-white/10"
                onClick={() => setMenuOpen(false)}
              >
                Sign in
              </Link>
              <Link
                to="/register"
                className="text-sm font-semibold text-white px-3 py-1.5"
                onClick={() => setMenuOpen(false)}
              >
                Register
              </Link>
            </>
          )}
        </div>
      )}
    </nav>
  )
}
