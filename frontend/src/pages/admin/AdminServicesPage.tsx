import { useEffect, useState } from 'react'
import { getServices, createService, updateService, deleteService } from '../../api/services'
import type { BankingServiceResponse, CreateServiceRequest } from '../../types'

type View = 'list' | 'create' | 'edit'

export default function AdminServicesPage() {
  const [services, setServices] = useState<BankingServiceResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [view, setView] = useState<View>('list')
  const [selected, setSelected] = useState<BankingServiceResponse | null>(null)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
  const [form, setForm] = useState<CreateServiceRequest>({ name: '', description: '', durationMinutes: 30 })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    load()
  }, [])

  function load() {
    setLoading(true)
    getServices()
      .then(setServices)
      .catch(() => setError('Failed to load services.'))
      .finally(() => setLoading(false))
  }

  function openCreate() {
    setForm({ name: '', description: '', durationMinutes: 30 })
    setError('')
    setView('create')
  }

  function openEdit(svc: BankingServiceResponse) {
    setSelected(svc)
    setForm({ name: svc.name, description: svc.description ?? '', durationMinutes: svc.durationMinutes })
    setError('')
    setView('edit')
  }

  async function handleSave() {
    setSaving(true)
    setError('')
    try {
      if (view === 'create') {
        const created = await createService(form)
        setServices((prev) => [created, ...prev])
        setSuccessMsg('Service created.')
        setView('list')
      } else if (view === 'edit' && selected) {
        const updated = await updateService(selected.id, form)
        setServices((prev) => prev.map((s) => (s.id === updated.id ? updated : s)))
        setSuccessMsg('Service updated.')
        setView('list')
      }
    } catch {
      setError('Failed to save service.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Deactivate this service?')) return
    try {
      await deleteService(id)
      setServices((prev) => prev.filter((s) => s.id !== id))
      setSuccessMsg('Service deactivated.')
    } catch {
      setError('Failed to deactivate service.')
    }
  }

  if (view !== 'list') {
    return (
      <div className="px-8 py-8 max-w-xl">
        <button onClick={() => setView('list')} className="text-sm text-gray-500 hover:text-gray-700 mb-6">
          ← Back to services
        </button>
        <h1 className="text-xl font-bold text-gray-900 mb-6">
          {view === 'create' ? 'Add Service' : `Edit — ${selected?.name}`}
        </h1>
        {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
        <div className="bg-white border border-gray-200 rounded-xl p-6 space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Name *</label>
            <input
              type="text"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Description</label>
            <textarea
              value={form.description ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              rows={3}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Duration (minutes) *</label>
            <input
              type="number"
              min={1}
              value={form.durationMinutes}
              onChange={(e) => setForm((f) => ({ ...f, durationMinutes: Number(e.target.value) }))}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={handleSave}
              disabled={saving}
              className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-5 py-2 rounded-lg disabled:opacity-50"
            >
              {saving ? 'Saving…' : view === 'create' ? 'Create Service' : 'Save Changes'}
            </button>
            <button
              onClick={() => setView('list')}
              className="border border-gray-300 text-gray-600 text-sm font-medium px-5 py-2 rounded-lg hover:bg-gray-50"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Banking Services</h1>
          <p className="text-sm text-gray-500 mt-0.5">{services.length} service{services.length !== 1 ? 's' : ''}</p>
        </div>
        <button
          onClick={openCreate}
          className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
        >
          Add Service
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
                <th className="text-left px-5 py-3 font-medium">Name</th>
                <th className="text-left px-4 py-3 font-medium">Description</th>
                <th className="text-left px-4 py-3 font-medium">Duration</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {services.map((svc) => (
                <tr key={svc.id} className="border-b border-gray-50 hover:bg-gray-50">
                  <td className="px-5 py-3 font-medium text-gray-800">{svc.name}</td>
                  <td className="px-4 py-3 text-gray-500 max-w-xs truncate">{svc.description || '—'}</td>
                  <td className="px-4 py-3 text-gray-500">{svc.durationMinutes} min</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${svc.active ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                      {svc.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => openEdit(svc)}
                        className="text-xs text-purple-600 hover:text-purple-700 font-medium"
                      >
                        Edit
                      </button>
                      {svc.active && (
                        <button
                          onClick={() => handleDelete(svc.id)}
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
