import { useEffect, useState } from 'react'
import { getBranches, createBranch, updateBranch, deleteBranch } from '../../api/branches'
import { listAvailabilityExceptions, createAvailabilityException, deleteAvailabilityException } from '../../api/admin'
import type {
  BranchResponse,
  CreateBranchRequest,
  UpdateBranchRequest,
  AvailabilityExceptionResponse,
  ExceptionType,
} from '../../types'

type View = 'list' | 'create' | 'edit' | 'exceptions'

export default function AdminBranchesPage() {
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [view, setView] = useState<View>('list')
  const [selected, setSelected] = useState<BranchResponse | null>(null)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  const [form, setForm] = useState<CreateBranchRequest>({
    branchCode: '',
    name: '',
    address: '',
    city: '',
    province: '',
    postalCode: '',
    maxConcurrentAppointments: 1,
  })
  const [saving, setSaving] = useState(false)

  const [exceptions, setExceptions] = useState<AvailabilityExceptionResponse[]>([])
  const [excForm, setExcForm] = useState({ exceptionDate: '', type: 'CLOSED' as ExceptionType, reason: '' })
  const [excSaving, setExcSaving] = useState(false)

  useEffect(() => {
    load()
  }, [])

  function load() {
    setLoading(true)
    getBranches()
      .then(setBranches)
      .catch(() => setError('Failed to load branches.'))
      .finally(() => setLoading(false))
  }

  function openCreate() {
    setForm({ branchCode: '', name: '', address: '', city: '', province: '', postalCode: '', maxConcurrentAppointments: 1 })
    setError('')
    setView('create')
  }

  function openEdit(branch: BranchResponse) {
    setSelected(branch)
    setForm({
      branchCode: branch.branchCode,
      name: branch.name,
      address: branch.address,
      city: branch.city,
      province: branch.province,
      postalCode: branch.postalCode,
      latitude: branch.latitude,
      longitude: branch.longitude,
      phoneNumber: branch.phoneNumber ?? '',
      email: branch.email ?? '',
      maxConcurrentAppointments: branch.maxConcurrentAppointments,
    })
    setError('')
    setView('edit')
  }

  function openExceptions(branch: BranchResponse) {
    setSelected(branch)
    setError('')
    listAvailabilityExceptions(branch.id).then(setExceptions).catch(() => {})
    setView('exceptions')
  }

  async function handleSave() {
    setSaving(true)
    setError('')
    try {
      if (view === 'create') {
        const created = await createBranch(form)
        setBranches((prev) => [created, ...prev])
        setSuccessMsg('Branch created.')
        setView('list')
      } else if (view === 'edit' && selected) {
        const { branchCode: _bc, ...updateReq } = form
        const updated = await updateBranch(selected.id, updateReq as UpdateBranchRequest)
        setBranches((prev) => prev.map((b) => (b.id === updated.id ? updated : b)))
        setSuccessMsg('Branch updated.')
        setView('list')
      }
    } catch {
      setError('Failed to save branch.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Deactivate this branch?')) return
    try {
      await deleteBranch(id)
      setBranches((prev) => prev.filter((b) => b.id !== id))
      setSuccessMsg('Branch deactivated.')
    } catch {
      setError('Failed to deactivate branch.')
    }
  }

  async function handleAddException() {
    if (!selected || !excForm.exceptionDate) return
    setExcSaving(true)
    try {
      const created = await createAvailabilityException(selected.id, {
        exceptionDate: excForm.exceptionDate,
        type: excForm.type,
        reason: excForm.reason || undefined,
      })
      setExceptions((prev) => [created, ...prev])
      setExcForm({ exceptionDate: '', type: 'CLOSED', reason: '' })
    } catch {
      setError('Failed to add exception.')
    } finally {
      setExcSaving(false)
    }
  }

  async function handleDeleteException(exceptionId: string) {
    if (!selected) return
    try {
      await deleteAvailabilityException(selected.id, exceptionId)
      setExceptions((prev) => prev.filter((e) => e.id !== exceptionId))
    } catch {
      setError('Failed to delete exception.')
    }
  }

  if (view !== 'list') {
    return (
      <div className="px-8 py-8 max-w-2xl">
        <button
          onClick={() => setView('list')}
          className="text-sm text-gray-500 hover:text-gray-700 mb-6 flex items-center gap-1"
        >
          ← Back to branches
        </button>

        {view === 'exceptions' && selected ? (
          <>
            <h1 className="text-xl font-bold text-gray-900 mb-1">
              Availability Exceptions — {selected.name}
            </h1>
            <p className="text-sm text-gray-500 mb-6">
              Block specific dates for this branch (holidays, maintenance, etc.)
            </p>

            {error && <p className="text-red-600 text-sm mb-4">{error}</p>}

            <div className="bg-white border border-gray-200 rounded-xl p-5 mb-5">
              <h2 className="text-sm font-semibold text-gray-700 mb-3">Add Exception</h2>
              <div className="grid grid-cols-3 gap-3">
                <input
                  type="date"
                  value={excForm.exceptionDate}
                  onChange={(e) => setExcForm((f) => ({ ...f, exceptionDate: e.target.value }))}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
                <select
                  value={excForm.type}
                  onChange={(e) => setExcForm((f) => ({ ...f, type: e.target.value as ExceptionType }))}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                >
                  <option value="CLOSED">Closed</option>
                  <option value="MAINTENANCE">Maintenance</option>
                </select>
                <input
                  type="text"
                  placeholder="Reason (optional)"
                  value={excForm.reason}
                  onChange={(e) => setExcForm((f) => ({ ...f, reason: e.target.value }))}
                  className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
                />
              </div>
              <button
                onClick={handleAddException}
                disabled={excSaving || !excForm.exceptionDate}
                className="mt-3 bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors disabled:opacity-50"
              >
                {excSaving ? 'Adding…' : 'Add Exception'}
              </button>
            </div>

            <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
              {exceptions.length === 0 ? (
                <div className="px-5 py-8 text-center text-sm text-gray-400">No exceptions configured.</div>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 text-xs text-gray-500">
                      <th className="text-left px-5 py-3 font-medium">Date</th>
                      <th className="text-left px-4 py-3 font-medium">Type</th>
                      <th className="text-left px-4 py-3 font-medium">Reason</th>
                      <th className="px-4 py-3" />
                    </tr>
                  </thead>
                  <tbody>
                    {exceptions.map((exc) => (
                      <tr key={exc.id} className="border-b border-gray-50">
                        <td className="px-5 py-3 text-gray-700">{exc.exceptionDate}</td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex px-2 py-0.5 rounded text-xs font-medium ${exc.type === 'CLOSED' ? 'bg-red-50 text-red-700' : 'bg-orange-50 text-orange-700'}`}>
                            {exc.type}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-500">{exc.reason ?? '—'}</td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => handleDeleteException(exc.id)}
                            className="text-xs text-red-500 hover:text-red-700"
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        ) : (
          <>
            <h1 className="text-xl font-bold text-gray-900 mb-6">
              {view === 'create' ? 'Create Branch' : `Edit — ${selected?.name}`}
            </h1>
            {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
            <BranchForm
              form={form}
              onChange={setForm}
              isCreate={view === 'create'}
              saving={saving}
              onSave={handleSave}
              onCancel={() => setView('list')}
            />
          </>
        )}
      </div>
    )
  }

  return (
    <div className="px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Branches</h1>
          <p className="text-sm text-gray-500 mt-0.5">{branches.length} branch{branches.length !== 1 ? 'es' : ''}</p>
        </div>
        <button
          onClick={openCreate}
          className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
        >
          Add Branch
        </button>
      </div>

      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
      {successMsg && <p className="text-green-600 text-sm mb-4">{successMsg}</p>}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 text-xs text-gray-500">
                <th className="text-left px-5 py-3 font-medium">Code</th>
                <th className="text-left px-4 py-3 font-medium">Name</th>
                <th className="text-left px-4 py-3 font-medium">City</th>
                <th className="text-left px-4 py-3 font-medium">Capacity</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {branches.map((branch) => (
                <tr key={branch.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="px-5 py-3 font-mono text-xs text-gray-500">{branch.branchCode}</td>
                  <td className="px-4 py-3 font-medium text-gray-800">{branch.name}</td>
                  <td className="px-4 py-3 text-gray-500">{branch.city}</td>
                  <td className="px-4 py-3 text-gray-500">{branch.maxConcurrentAppointments}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${branch.active ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {branch.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => openExceptions(branch)}
                        className="text-xs text-gray-500 hover:text-gray-700 font-medium"
                      >
                        Exceptions
                      </button>
                      <button
                        onClick={() => openEdit(branch)}
                        className="text-xs text-purple-600 hover:text-purple-700 font-medium"
                      >
                        Edit
                      </button>
                      {branch.active && (
                        <button
                          onClick={() => handleDelete(branch.id)}
                          className="text-xs text-red-500 hover:text-red-700 font-medium"
                        >
                          Deactivate
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function BranchForm({
  form,
  onChange,
  isCreate,
  saving,
  onSave,
  onCancel,
}: {
  form: CreateBranchRequest
  onChange: (f: CreateBranchRequest) => void
  isCreate: boolean
  saving: boolean
  onSave: () => void
  onCancel: () => void
}) {
  function field(key: keyof CreateBranchRequest, label: string, type = 'text', required = false) {
    return (
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
        <input
          type={type}
          required={required}
          value={(form[key] as string | number | undefined) ?? ''}
          onChange={(e) =>
            onChange({ ...form, [key]: type === 'number' ? Number(e.target.value) : e.target.value })
          }
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
        />
      </div>
    )
  }

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
      <div className="grid grid-cols-2 gap-4">
        {isCreate && field('branchCode', 'Branch Code', 'text', true)}
        {field('name', 'Name', 'text', true)}
        {field('address', 'Address', 'text', true)}
        {field('city', 'City', 'text', true)}
        {field('province', 'Province', 'text', true)}
        {field('postalCode', 'Postal Code', 'text', true)}
        {field('phoneNumber', 'Phone Number')}
        {field('email', 'Email')}
        {field('maxConcurrentAppointments', 'Max Concurrent Appointments', 'number')}
      </div>
      <div className="flex gap-3 pt-2">
        <button
          onClick={onSave}
          disabled={saving}
          className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-5 py-2 rounded-lg transition-colors disabled:opacity-50"
        >
          {saving ? 'Saving…' : isCreate ? 'Create Branch' : 'Save Changes'}
        </button>
        <button
          onClick={onCancel}
          className="border border-gray-300 text-gray-600 text-sm font-medium px-5 py-2 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Cancel
        </button>
      </div>
    </div>
  )
}
