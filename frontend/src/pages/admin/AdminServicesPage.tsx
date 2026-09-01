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
        <button onClick={() => setView('list')} className="text-sm text-[#7c7c7c] hover:text-[#383634] mb-6">
          ← Back to services
        </button>
        <h1 className="text-xl font-bold text-[#383634] mb-6">
          {view === 'create' ? 'Add Service' : `Edit — ${selected?.name}`}
        </h1>
        {error && <p className="text-[#a5132a] text-sm mb-4">{error}</p>}
        <div className="bg-white border border-[#e1e1e1] rounded-xl p-6 space-y-4">
          <div>
            <label className="block text-xs font-medium text-[#7c7c7c] mb-1">Name *</label>
            <input
              type="text"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-[#7c7c7c] mb-1">Description</label>
            <textarea
              value={form.description ?? ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              rows={3}
              className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-[#7c7c7c] mb-1">Duration (minutes) *</label>
            <input
              type="number"
              min={1}
              value={form.durationMinutes}
              onChange={(e) => setForm((f) => ({ ...f, durationMinutes: Number(e.target.value) }))}
              className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
            />
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={handleSave}
              disabled={saving}
              className="bg-[#009de0] hover:bg-[#0084d5] text-white text-sm font-medium px-5 py-2 rounded-lg disabled:opacity-50"
            >
              {saving ? 'Saving…' : view === 'create' ? 'Create Service' : 'Save Changes'}
            </button>
            <button
              onClick={() => setView('list')}
              className="border border-[#e1e1e1] text-[#7c7c7c] text-sm font-medium px-5 py-2 rounded-lg hover:bg-[#f8f8f9]"
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
          <h1 className="text-2xl font-bold text-[#383634]">Banking Services</h1>
          <p className="text-sm text-[#7c7c7c] mt-0.5">{services.length} service{services.length !== 1 ? 's' : ''}</p>
        </div>
        <button
          onClick={openCreate}
          className="bg-[#009de0] hover:bg-[#0084d5] text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
        >
          Add Service
        </button>
      </div>

      {error && <p className="text-[#a5132a] text-sm mb-4">{error}</p>}
      {successMsg && <p className="text-[#68a200] text-sm mb-4">{successMsg}</p>}

      {loading ? (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-[#009de0] border-t-transparent rounded-full animate-spin" />
        </div>
      ) : (
        <div className="bg-white border border-[#e1e1e1] rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#e1e1e1] text-xs text-[#7c7c7c]">
                <th className="text-left px-5 py-3 font-medium">Name</th>
                <th className="text-left px-4 py-3 font-medium">Description</th>
                <th className="text-left px-4 py-3 font-medium">Duration</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody>
              {services.map((svc) => (
                <tr key={svc.id} className="border-b border-[#e1e1e1] hover:bg-[#f8f8f9]">
                  <td className="px-5 py-3 font-medium text-[#383634]">{svc.name}</td>
                  <td className="px-4 py-3 text-[#7c7c7c] max-w-xs truncate">{svc.description || '—'}</td>
                  <td className="px-4 py-3 text-[#7c7c7c]">{svc.durationMinutes} min</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${svc.active ? 'bg-[#f0f7e6] text-[#68a200]' : 'bg-[#efefef] text-[#7c7c7c]'}`}>
                      {svc.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => openEdit(svc)}
                        className="text-xs text-[#009de0] hover:text-[#0084d5] font-medium"
                      >
                        Edit
                      </button>
                      {svc.active && (
                        <button
                          onClick={() => handleDelete(svc.id)}
                          className="text-xs text-[#a5132a] hover:text-red-700 font-medium"
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
