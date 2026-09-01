import { useState } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { updateMe } from '../api/users'

export default function ProfilePage() {
  const { user, refreshUser } = useAuth()
  const [form, setForm] = useState({
    firstName: user?.firstName ?? '',
    lastName: user?.lastName ?? '',
    phoneNumber: user?.phoneNumber ?? '',
  })
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  function update(field: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm((f) => ({ ...f, [field]: e.target.value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSuccess(false)
    setLoading(true)
    try {
      await updateMe({
        firstName: form.firstName,
        lastName: form.lastName,
        phoneNumber: form.phoneNumber || undefined,
      })
      await refreshUser()
      setSuccess(true)
    } catch {
      setError('Failed to update profile. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 px-4 py-10">
      <div className="max-w-lg mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Your Profile</h1>
        <p className="text-sm text-gray-500 mb-8">Manage your personal information</p>

        <div className="bg-white border border-gray-200 rounded-2xl shadow-sm p-8">
          <div className="flex items-center gap-4 mb-8 pb-6 border-b border-gray-100">
            <div className="w-14 h-14 rounded-full bg-purple-100 flex items-center justify-center text-purple-700 text-xl font-bold">
              {user?.firstName?.[0]}{user?.lastName?.[0]}
            </div>
            <div>
              <p className="font-semibold text-gray-900">{user?.firstName} {user?.lastName}</p>
              <p className="text-sm text-gray-500">{user?.email}</p>
              <span className="inline-block mt-1 text-xs font-medium text-purple-600 bg-purple-50 px-2 py-0.5 rounded-full">
                {user?.role}
              </span>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                disabled
                value={user?.email ?? ''}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm bg-gray-50 text-gray-400 cursor-not-allowed"
              />
              <p className="text-xs text-gray-400 mt-1">Email cannot be changed</p>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">First name</label>
                <input
                  type="text"
                  required
                  value={form.firstName}
                  onChange={update('firstName')}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Last name</label>
                <input
                  type="text"
                  required
                  value={form.lastName}
                  onChange={update('lastName')}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Phone number <span className="text-gray-400 font-normal">(optional)</span>
              </label>
              <input
                type="tel"
                value={form.phoneNumber}
                onChange={update('phoneNumber')}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              />
            </div>

            {error && <p className="text-red-600 text-sm">{error}</p>}
            {success && <p className="text-green-600 text-sm">Profile updated successfully.</p>}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-purple-600 hover:bg-purple-700 disabled:opacity-60 text-white font-medium py-2.5 rounded-lg transition-colors text-sm"
            >
              {loading ? 'Saving…' : 'Save changes'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
