import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

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

  return (
    <div className="min-h-screen bg-[#f5f5f5] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-2xl font-semibold text-[#383634]">Create your account</h1>
          <p className="text-[#7c7c7c] text-sm mt-1">Book Capitec appointments online</p>
        </div>

        <div className="bg-white border border-[#e1e1e1] rounded-2xl shadow-sm p-8">
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-[#383634] mb-1">First name</label>
                <input
                  type="text"
                  required
                  value={form.firstName}
                  onChange={update('firstName')}
                  className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#383634] mb-1">Last name</label>
                <input
                  type="text"
                  required
                  value={form.lastName}
                  onChange={update('lastName')}
                  className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#383634] mb-1">Email</label>
              <input
                type="email"
                required
                value={form.email}
                onChange={update('email')}
                className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
                placeholder="you@example.com"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-[#383634] mb-1">Password</label>
              <input
                type="password"
                required
                minLength={8}
                value={form.password}
                onChange={update('password')}
                className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
                placeholder="Min. 8 characters"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-[#383634] mb-1">
                Phone number <span className="text-[#abb3b7] font-normal">(optional)</span>
              </label>
              <input
                type="tel"
                value={form.phoneNumber}
                onChange={update('phoneNumber')}
                className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
                placeholder="+27 00 000 0000"
              />
            </div>

            {error && <p className="text-[#a5132a] text-sm">{error}</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-[#009de0] hover:bg-[#0084d5] disabled:bg-[#a2a9ac] text-white font-medium py-2.5 rounded-lg transition-colors text-sm"
            >
              {loading ? 'Creating account…' : 'Create account'}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-[#7c7c7c] mt-5">
          Already have an account?{' '}
          <Link to="/login" className="text-[#009de0] font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
