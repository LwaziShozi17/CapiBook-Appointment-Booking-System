import { useEffect, useState } from 'react'
import { listAdminUsers, createBranchAdmin, deactivateUser } from '../../api/admin'
import { getBranches } from '../../api/branches'
import type { AdminUserResponse, CreateBranchAdminRequest, BranchResponse, PageResponse } from '../../types'

export default function AdminUsersPage() {
  const [page, setPage] = useState<PageResponse<AdminUserResponse> | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [form, setForm] = useState<CreateBranchAdminRequest>({
    email: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    branchId: '',
    password: '',
  })
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    getBranches().then(setBranches).catch(() => {})
  }, [])

  useEffect(() => {
    setLoading(true)
    listAdminUsers(currentPage, 20)
      .then(setPage)
      .catch(() => setError('Failed to load users.'))
      .finally(() => setLoading(false))
  }, [currentPage])

  async function handleCreate() {
    setCreating(true)
    setError('')
    try {
      await createBranchAdmin(form)
      setSuccessMsg('Branch admin created.')
      setShowCreateForm(false)
      setForm({ email: '', firstName: '', lastName: '', phoneNumber: '', branchId: '', password: '' })
      listAdminUsers(0, 20).then(setPage).catch(() => {})
      setCurrentPage(0)
    } catch {
      setError('Failed to create branch admin.')
    } finally {
      setCreating(false)
    }
  }

  async function handleDeactivate(userId: string) {
    if (!confirm('Deactivate this user?')) return
    try {
      const updated = await deactivateUser(userId)
      setPage((prev) =>
        prev ? { ...prev, content: prev.content.map((u) => (u.id === updated.id ? updated : u)) } : prev
      )
      setSuccessMsg('User deactivated.')
    } catch {
      setError('Failed to deactivate user.')
    }
  }

  const roleBadge = (role: string) => {
    const styles: Record<string, string> = {
      CUSTOMER: 'bg-gray-100 text-gray-600',
      BRANCH_ADMIN: 'bg-blue-100 text-blue-700',
      SYSTEM_ADMIN: 'bg-purple-100 text-purple-700',
    }
    return styles[role] ?? 'bg-gray-100 text-gray-600'
  }

  return (
    <div className="px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Users</h1>
          <p className="text-sm text-gray-500 mt-0.5">All registered accounts</p>
        </div>
        <button
          onClick={() => setShowCreateForm((v) => !v)}
          className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
        >
          {showCreateForm ? 'Cancel' : 'Add Branch Admin'}
        </button>
      </div>

      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
      {successMsg && <p className="text-green-600 text-sm mb-4">{successMsg}</p>}

      {showCreateForm && (
        <div className="bg-white border border-gray-200 rounded-xl p-6 mb-6">
          <h2 className="text-sm font-semibold text-gray-700 mb-4">Create Branch Admin</h2>
          <div className="grid grid-cols-2 gap-4">
            {[
              { key: 'email', label: 'Email', type: 'email' },
              { key: 'firstName', label: 'First Name' },
              { key: 'lastName', label: 'Last Name' },
              { key: 'phoneNumber', label: 'Phone Number' },
              { key: 'password', label: 'Password', type: 'password' },
            ].map(({ key, label, type = 'text' }) => (
              <div key={key}>
                <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
                <input
                  type={type}
                  value={(form as unknown as Record<string, string>)[key] ?? ''}
                  onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
            ))}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Branch</label>
              <select
                value={form.branchId}
                onChange={(e) => setForm((f) => ({ ...f, branchId: e.target.value }))}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
              >
                <option value="">Select branch…</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </div>
          </div>
          <button
            onClick={handleCreate}
            disabled={creating || !form.email || !form.branchId || !form.password}
            className="mt-4 bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-5 py-2 rounded-lg disabled:opacity-50"
          >
            {creating ? 'Creating…' : 'Create Branch Admin'}
          </button>
        </div>
      )}

      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 text-xs text-gray-500">
                  <th className="text-left px-5 py-3 font-medium">Name</th>
                  <th className="text-left px-4 py-3 font-medium">Email</th>
                  <th className="text-left px-4 py-3 font-medium">Role</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-left px-4 py-3 font-medium">Joined</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {(page?.content ?? []).map((u) => (
                  <tr key={u.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="px-5 py-3 font-medium text-gray-800">
                      {u.firstName} {u.lastName}
                    </td>
                    <td className="px-4 py-3 text-gray-500">{u.email}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${roleBadge(u.role)}`}>
                        {u.role.replace('_', ' ')}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                        {u.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-400 text-xs">
                      {new Date(u.createdAt).toLocaleDateString('en-ZA')}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {u.active && u.role !== 'SYSTEM_ADMIN' && (
                        <button
                          onClick={() => handleDeactivate(u.id)}
                          className="text-xs text-red-500 hover:text-red-700 font-medium"
                        >
                          Deactivate
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {page && page.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          <button
            disabled={currentPage === 0}
            onClick={() => setCurrentPage((p) => p - 1)}
            className="px-4 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50"
          >
            Previous
          </button>
          <span className="text-sm text-gray-500">
            Page {currentPage + 1} of {page.totalPages}
          </span>
          <button
            disabled={currentPage >= page.totalPages - 1}
            onClick={() => setCurrentPage((p) => p + 1)}
            className="px-4 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
