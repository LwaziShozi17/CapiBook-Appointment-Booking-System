import { useEffect, useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { listAdminAppointments, confirmAppointment, completeAppointment, markNoShow, cancelAppointment } from '../../api/appointments'
import { getBranches } from '../../api/branches'
import type { AppointmentResponse, AppointmentStatus, BranchResponse, PageResponse } from '../../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-[#fff3e0] text-[#d66700]',
  CONFIRMED: 'bg-[#f2fafd] text-[#009de0]',
  CANCELLED: 'bg-[#fdf2f4] text-[#a5132a]',
  COMPLETED: 'bg-[#f0f7e6] text-[#68a200]',
  NO_SHOW: 'bg-[#efefef] text-[#7c7c7c]',
  RESCHEDULED: 'bg-[#edf8fd] text-[#00486d]',
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
          <h1 className="text-2xl font-bold text-[#383634]">Appointments</h1>
          <p className="text-sm text-[#7c7c7c] mt-0.5">
            {isSystemAdmin ? 'All branches' : 'Your branch'}
          </p>
        </div>
        {isSystemAdmin && branches.length > 0 && (
          <select
            value={selectedBranch}
            onChange={(e) => { setSelectedBranch(e.target.value); setCurrentPage(0) }}
            className="border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0]"
          >
            <option value="">All branches</option>
            {branches.map((b) => (
              <option key={b.id} value={b.id}>{b.name}</option>
            ))}
          </select>
        )}
      </div>

      {error && (
        <div className="bg-[#fdf2f4] border border-[#fde8eb] text-[#a5132a] text-sm rounded-lg px-4 py-3 mb-4">
          {error}
        </div>
      )}

      <div className="bg-white border border-[#e1e1e1] rounded-xl overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-4 border-[#009de0] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : appointments.length === 0 ? (
          <div className="px-5 py-12 text-center text-sm text-[#abb3b7]">No appointments found.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[#e1e1e1] text-xs text-[#7c7c7c]">
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
                  <tr key={appt.id} className="border-b border-[#e1e1e1] hover:bg-[#f8f8f9]">
                    <td className="px-5 py-3 font-mono text-xs text-[#abb3b7]">{appt.referenceNumber}</td>
                    <td className="px-4 py-3 text-[#383634]">
                      {appt.customerFirstName} {appt.customerLastName}
                    </td>
                    <td className="px-4 py-3 text-[#383634]">{appt.serviceName}</td>
                    <td className="px-4 py-3 text-[#7c7c7c]">{appt.branchName}</td>
                    <td className="px-4 py-3 text-[#7c7c7c] whitespace-nowrap">
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
            className="px-4 py-2 text-sm border border-[#e1e1e1] rounded-lg disabled:opacity-40 hover:bg-[#f8f8f9]"
          >
            Previous
          </button>
          <span className="text-sm text-[#7c7c7c]">
            Page {currentPage + 1} of {page.totalPages}
          </span>
          <button
            disabled={currentPage >= page.totalPages - 1}
            onClick={() => setCurrentPage((p) => p + 1)}
            className="px-4 py-2 text-sm border border-[#e1e1e1] rounded-lg disabled:opacity-40 hover:bg-[#f8f8f9]"
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
    blue: 'bg-[#f2fafd] text-[#009de0] hover:bg-[#e0f4fc]',
    green: 'bg-[#f0f7e6] text-[#68a200] hover:bg-[#e6f2d8]',
    gray: 'bg-[#efefef] text-[#7c7c7c] hover:bg-[#e1e1e1]',
    red: 'bg-[#fdf2f4] text-[#a5132a] hover:bg-[#f9e5e8]',
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
