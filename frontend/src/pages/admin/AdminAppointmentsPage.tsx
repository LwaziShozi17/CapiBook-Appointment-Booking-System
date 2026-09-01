import { useEffect, useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { listAdminAppointments, confirmAppointment, completeAppointment, markNoShow, cancelAppointment } from '../../api/appointments'
import { getBranches } from '../../api/branches'
import type { AppointmentResponse, AppointmentStatus, BranchResponse, PageResponse } from '../../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  CANCELLED: 'bg-red-100 text-red-700',
  COMPLETED: 'bg-green-100 text-green-800',
  NO_SHOW: 'bg-gray-100 text-gray-600',
  RESCHEDULED: 'bg-purple-100 text-purple-700',
}

export default function AdminAppointmentsPage() {
  const { user } = useAuth()
  const isSystemAdmin = user?.role === 'SYSTEM_ADMIN'

  const [page, setPage] = useState<PageResponse<AppointmentResponse> | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [selectedBranch, setSelectedBranch] = useState('')
  const [loading, setLoading] = useState(false)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (isSystemAdmin) {
      getBranches().then(setBranches).catch(() => {})
    }
  }, [isSystemAdmin])

  useEffect(() => {
    setLoading(true)
    setError('')
    listAdminAppointments(currentPage, 20, selectedBranch || undefined)
      .then(setPage)
      .catch(() => setError('Failed to load appointments.'))
      .finally(() => setLoading(false))
  }, [currentPage, selectedBranch])

  async function handleAction(
    id: string,
    action: 'confirm' | 'complete' | 'no-show' | 'cancel'
  ) {
    setActionLoading(id + action)
    try {
      let updated: AppointmentResponse
      if (action === 'confirm') updated = await confirmAppointment(id)
      else if (action === 'complete') updated = await completeAppointment(id)
      else if (action === 'no-show') updated = await markNoShow(id)
      else updated = await cancelAppointment(id)

      setPage((prev) =>
        prev
          ? { ...prev, content: prev.content.map((a) => (a.id === updated.id ? updated : a)) }
          : prev
      )
    } catch {
      setError('Action failed. Please try again.')
    } finally {
      setActionLoading(null)
    }
  }

  const appointments = page?.content ?? []

  return (
    <div className="px-8 py-8">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Appointments</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {isSystemAdmin ? 'All branches' : 'Your branch'}
          </p>
        </div>
        {isSystemAdmin && branches.length > 0 && (
          <select
            value={selectedBranch}
            onChange={(e) => { setSelectedBranch(e.target.value); setCurrentPage(0) }}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            <option value="">All branches</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
        )}
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">
          {error}
        </div>
      )}

      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
          </div>
        ) : appointments.length === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-gray-400">No appointments found.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 text-xs text-gray-500">
                  <th className="text-left px-5 py-3 font-medium">Reference</th>
                  <th className="text-left px-4 py-3 font-medium">Customer</th>
                  <th className="text-left px-4 py-3 font-medium">Service</th>
                  <th className="text-left px-4 py-3 font-medium">Branch</th>
                  <th className="text-left px-4 py-3 font-medium">Date / Time</th>
                  <th className="text-left px-4 py-3 font-medium">Status</th>
                  <th className="text-left px-4 py-3 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map((appt) => (
                  <tr key={appt.id} className="border-b border-gray-50 hover:bg-gray-50">
                    <td className="px-5 py-3 font-mono text-xs text-gray-500">{appt.referenceNumber}</td>
                    <td className="px-4 py-3 text-gray-700">
                      {appt.customerFirstName} {appt.customerLastName}
                    </td>
                    <td className="px-4 py-3 text-gray-700">{appt.serviceName}</td>
                    <td className="px-4 py-3 text-gray-500">{appt.branchName}</td>
                    <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                      {appt.appointmentDate} {appt.startTime}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[appt.status]}`}>
                        {appt.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-1 flex-wrap">
                        {appt.status === 'PENDING' && (
                          <ActionBtn
                            label="Confirm"
                            color="blue"
                            loading={actionLoading === appt.id + 'confirm'}
                            onClick={() => handleAction(appt.id, 'confirm')}
                          />
                        )}
                        {appt.status === 'CONFIRMED' && (
                          <>
                            <ActionBtn
                              label="Complete"
                              color="green"
                              loading={actionLoading === appt.id + 'complete'}
                              onClick={() => handleAction(appt.id, 'complete')}
                            />
                            <ActionBtn
                              label="No-show"
                              color="gray"
                              loading={actionLoading === appt.id + 'no-show'}
                              onClick={() => handleAction(appt.id, 'no-show')}
                            />
                          </>
                        )}
                        {(appt.status === 'PENDING' || appt.status === 'CONFIRMED') && (
                          <ActionBtn
                            label="Cancel"
                            color="red"
                            loading={actionLoading === appt.id + 'cancel'}
                            onClick={() => handleAction(appt.id, 'cancel')}
                          />
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

function ActionBtn({
  label,
  color,
  loading,
  onClick,
}: {
  label: string
  color: 'blue' | 'green' | 'gray' | 'red'
  loading: boolean
  onClick: () => void
}) {
  const colors = {
    blue: 'bg-blue-50 text-blue-700 hover:bg-blue-100',
    green: 'bg-green-50 text-green-700 hover:bg-green-100',
    gray: 'bg-gray-100 text-gray-600 hover:bg-gray-200',
    red: 'bg-red-50 text-red-600 hover:bg-red-100',
  }
  return (
    <button
      onClick={onClick}
      disabled={loading}
      className={`px-2 py-1 text-xs font-medium rounded transition-colors disabled:opacity-50 ${colors[color]}`}
    >
      {loading ? '...' : label}
    </button>
  )
}
