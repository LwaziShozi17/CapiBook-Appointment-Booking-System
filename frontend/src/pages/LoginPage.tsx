import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const FEATURES = [
  'Real-time slot availability',
  'Instant booking confirmation',
  'Reschedule anytime online',
]

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = await login({ email, password })
      const isAdmin = user.role === 'BRANCH_ADMIN' || user.role === 'SYSTEM_ADMIN'
      navigate(isAdmin ? '/admin' : '/appointments')
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Invalid email or password.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-white flex">
      {/* Brand panel */}
      <div className="hidden lg:flex flex-col justify-between w-5/12 bg-[#00486d] px-12 py-14">
        <Link to="/" className="flex items-center gap-2 text-white w-fit">
          <span className="w-8 h-8 bg-[#009de0] rounded-lg flex items-center justify-center flex-shrink-0">
            <svg className="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </span>
          <span className="text-lg font-bold tracking-tight">CapiBook</span>
        </Link>

        <div>
          <h2 className="text-3xl font-bold text-white leading-tight mb-8">
            Book Capitec appointments without the wait.
          </h2>
          <ul className="space-y-3.5">
            {FEATURES.map((f) => (
              <li key={f} className="flex items-center gap-3 text-white/75 text-sm">
                <span className="w-5 h-5 bg-[#009de0]/30 rounded-full flex items-center justify-center flex-shrink-0">
                  <svg className="w-3 h-3 text-[#009de0]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                  </svg>
                </span>
                {f}
              </li>
            ))}
          </ul>
        </div>

        <p className="text-xs text-white/30">© {new Date().getFullYear()} CapiBook</p>
      </div>

      {/* Form panel */}
      <div className="flex-1 flex items-center justify-center px-6 py-16 bg-[#f8fafc]">
        <div className="w-full max-w-sm">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-[#0f172a]">Welcome back</h1>
            <p className="text-[#64748b] text-sm mt-1">Sign in to your CapiBook account</p>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-[#e2e8f0] p-8">
            <form onSubmit={handleSubmit} className="flex flex-col gap-5">
              <div>
                <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">Email</label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full border border-[#e2e8f0] rounded-lg px-3.5 py-2.5 text-sm text-[#0f172a] placeholder:text-[#94a3b8] focus:outline-none focus:ring-2 focus:ring-[#009de0]/20 focus:border-[#009de0] transition-colors bg-white"
                  placeholder="you@example.com"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">Password</label>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full border border-[#e2e8f0] rounded-lg px-3.5 py-2.5 text-sm text-[#0f172a] placeholder:text-[#94a3b8] focus:outline-none focus:ring-2 focus:ring-[#009de0]/20 focus:border-[#009de0] transition-colors bg-white"
                  placeholder="••••••••"
                />
              </div>

              {error && (
                <div className="bg-[#fef2f2] border border-[#fecaca] text-[#dc2626] text-sm rounded-lg px-3.5 py-2.5">
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full bg-[#009de0] hover:bg-[#0085c3] disabled:bg-[#94a3b8] disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-lg transition-colors text-sm mt-1"
              >
                {loading ? 'Signing in…' : 'Sign in'}
              </button>
            </form>
          </div>

          <p className="text-center text-sm text-[#64748b] mt-5">
            Don't have an account?{' '}
            <Link to="/register" className="text-[#009de0] font-semibold hover:underline">
              Register
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
