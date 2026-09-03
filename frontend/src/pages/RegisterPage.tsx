import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

const FEATURES = [
  'Book in under two minutes',
  'Manage all your appointments in one place',
  'Get notified before your appointment',
]

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phoneNumber: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function update(field: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm((f) => ({ ...f, [field]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        password: form.password,
        phoneNumber: form.phoneNumber || undefined,
      })
      navigate('/appointments')
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Registration failed. Please try again.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  const inputClass =
    'w-full border border-[#e2e8f0] rounded-lg px-3.5 py-2.5 text-sm text-[#0f172a] placeholder:text-[#94a3b8] focus:outline-none focus:ring-2 focus:ring-[#009de0]/20 focus:border-[#009de0] transition-colors bg-white'

  return (
    <div className="min-h-screen bg-white flex">
      {/* Brand panel */}
      <div className="hidden lg:flex flex-col justify-between w-5/12 bg-[#003a5a] px-12 py-14">
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
            Start booking Capitec appointments in minutes.
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
        <div className="w-full max-w-md">
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-[#0f172a]">Create your account</h1>
            <p className="text-[#64748b] text-sm mt-1">Book Capitec appointments online</p>
          </div>

          <div className="bg-white rounded-2xl shadow-sm border border-[#e2e8f0] p-8">
            <form onSubmit={handleSubmit} className="flex flex-col gap-5">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">First name</label>
                  <input
                    type="text"
                    required
                    value={form.firstName}
                    onChange={update('firstName')}
                    className={inputClass}
                    placeholder="Thabo"
                  />
                </div>
                <div>
                  <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">Last name</label>
                  <input
                    type="text"
                    required
                    value={form.lastName}
                    onChange={update('lastName')}
                    className={inputClass}
                    placeholder="Nkosi"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">Email</label>
                <input
                  type="email"
                  required
                  value={form.email}
                  onChange={update('email')}
                  className={inputClass}
                  placeholder="you@example.com"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">Password</label>
                <input
                  type="password"
                  required
                  minLength={8}
                  value={form.password}
                  onChange={update('password')}
                  className={inputClass}
                  placeholder="Minimum 8 characters"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-[#0f172a] mb-1.5">
                  Phone number{' '}
                  <span className="text-[#94a3b8] font-normal">(optional)</span>
                </label>
                <input
                  type="tel"
                  value={form.phoneNumber}
                  onChange={update('phoneNumber')}
                  className={inputClass}
                  placeholder="+27 82 000 0000"
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
                {loading ? 'Creating account…' : 'Create account'}
              </button>
            </form>
          </div>

          <p className="text-center text-sm text-[#64748b] mt-5">
            Already have an account?{' '}
            <Link to="/login" className="text-[#009de0] font-semibold hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
