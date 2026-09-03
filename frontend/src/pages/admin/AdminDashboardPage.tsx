import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { getAppointmentSummary } from '../../api/admin'
import { listAdminAppointments } from '../../api/appointments'
import type { AppointmentSummaryResponse, AppointmentResponse, AppointmentStatus } from '../../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-[#fff7ed] text-[#ea580c] border border-[#fed7aa]',
  CONFIRMED: 'bg-[#eff9ff] text-[#009de0] border border-[#bae6fd]',
  CANCELLED: 'bg-[#fef2f2] text-[#dc2626] border border-[#fecaca]',
  COMPLETED: 'bg-[#f0fdf4] text-[#16a34a] border border-[#bbf7d0]',
  NO_SHOW: 'bg-[#f8fafc] text-[#64748b] border border-[#e2e8f0]',
  RESCHEDULED: 'bg-[#ecfeff] text-[#0891b2] border border-[#a5f3fc]',
}

const STAT_CONFIG = [
  { key: 'totalBooked',      label: 'Total booked',  valueColor: 'text-[#0f172a]' },
  { key: 'totalPending',     label: 'Pending',        valueColor: 'text-[#ea580c]' },
  { key: 'totalConfirmed',   label: 'Confirmed',      valueColor: 'text-[#009de0]' },
  { key: 'totalCompleted',   label: 'Completed',      valueColor: 'text-[#16a34a]' },
  { key: 'totalCancelled',   label: 'Cancelled',      valueColor: 'text-[#dc2626]' },
  { key: 'totalNoShow',      label: 'No-show',        valueColor: 'text-[#64748b]' },
  { key: 'totalRescheduled', label: 'Rescheduled',    valueColor: 'text-[#0891b2]' },
] as const

function today(): string {
  return new Date().toISOString().split('T')[0]
}

function thirtyDaysAgo(): string {
  const d = new Date()
  d.setDate(d.getDate() - 30)
  return d.toISOString().split('T')[0]
}

export default function AdminDashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const isSystemAdmin = user?.role === 'SYSTEM_ADMIN'

  const [summary, setSummary] = useState<AppointmentSummaryResponse | null>(null)
  const [recent, setRecent] = useState<AppointmentResponse[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const from = thirtyDaysAgo()
    const to = today()
    const promises: Promise<unknown>[] = [listAdminAppointments(0, 5)]
    if (isSystemAdmin) promises.push(getAppointmentSummary(from, to))

    Promise.all(promises)
      .then(([appts, sum]) => {
        const page = appts as Awaited<ReturnType<typeof listAdminAppointments>>
        setRecent(page?.content ?? [])
        if (sum) setSummary(sum as AppointmentSummaryResponse)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [isSystemAdmin])

  if (loading) {
    return (
      <div className="flex justify-center py-24">
        <div className="w-8 h-8 border-4 border-[#009de0] border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <div className="px-8 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-[#0f172a]">Dashboard</h1>
        <p className="text-sm text-[#64748b] mt-1">
          Welcome back, {user?.firstName}. Here's an overview of recent activity.
        </p>
      </div>

      {isSystemAdmin && summary && (
        <>
          <p className="text-xs font-semibold text-[#94a3b8] mb-3 tracking-wide">
            Last 30 days
          </p>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
            {STAT_CONFIG.map(({ key, label, valueColor }) => (
              <div key={key} className="bg-white border border-[#e2e8f0] rounded-xl p-4">
                <p className="text-xs text-[#94a3b8] font-medium mb-1">{label}</p>
                <p className={`text-2xl font-bold ${valueColor}`}>
                  {summary[key as keyof AppointmentSummaryResponse]}
                </p>
              </div>
            ))}
          </div>
        </>
      )}

      <div className="bg-white border border-[#e2e8f0] rounded-xl overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#f1f5f9]">
          <h2 className="text-sm font-semibold text-[#0f172a]">Recent appointments</h2>
          <button
            onClick={() => navigate('/admin/appointments')}
            className="text-xs text-[#009de0] hover:text-[#0085c3] font-semibold transition-colors"
          >
            View all
          </button>
        </div>
        {recent.length === 0 ? (
          <div className="px-5 py-10 text-center text-sm text-[#94a3b8]">
            No appointments yet.
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-[#f8fafc] border-b border-[#e2e8f0]">
                <th className="text-left px-5 py-3 text-xs font-semibold text-[#64748b]">Reference</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-[#64748b]">Customer</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-[#64748b]">Service</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-[#64748b]">Date</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-[#64748b]">Status</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((appt, i) => (
                <tr
                  key={appt.id}
                  onClick={() => navigate('/admin/appointments')}
                  className={`border-b border-[#f1f5f9] hover:bg-[#f8fafc] cursor-pointer transition-colors ${
                    i === recent.length - 1 ? 'border-b-0' : ''
                  }`}
                >
                  <td className="px-5 py-3 font-mono text-xs text-[#94a3b8]">{appt.referenceNumber}</td>
                  <td className="px-4 py-3 text-[#0f172a] font-medium">
                    {appt.customerFirstName} {appt.customerLastName}
                  </td>
                  <td className="px-4 py-3 text-[#334155]">{appt.serviceName}</td>
                  <td className="px-4 py-3 text-[#64748b] text-xs">{appt.appointmentDate}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex px-2 py-0.5 rounded-full text-xs font-semibold ${STATUS_STYLES[appt.status]}`}
                    >
                      {appt.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
