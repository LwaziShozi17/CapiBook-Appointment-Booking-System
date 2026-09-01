import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { getAppointmentSummary } from '../../api/admin'
import { listAdminAppointments } from '../../api/appointments'
import type { AppointmentSummaryResponse, AppointmentResponse, AppointmentStatus } from '../../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-[#fff3e0] text-[#d66700]',
  CONFIRMED: 'bg-[#f2fafd] text-[#009de0]',
  CANCELLED: 'bg-[#fdf2f4] text-[#a5132a]',
  COMPLETED: 'bg-[#f0f7e6] text-[#68a200]',
  NO_SHOW: 'bg-[#efefef] text-[#7c7c7c]',
  RESCHEDULED: 'bg-[#edf8fd] text-[#00486d]',
}

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
      <h1 className="text-2xl font-bold text-[#383634] mb-1">Dashboard</h1>
      <p className="text-sm text-[#7c7c7c] mb-8">
        Welcome back, {user?.firstName}. Here's an overview of recent activity.
      </p>

      {isSystemAdmin && summary && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          {[
            { label: 'Total Booked', value: summary.totalBooked, color: 'text-[#383634]' },
            { label: 'Pending', value: summary.totalPending, color: 'text-[#d66700]' },
            { label: 'Confirmed', value: summary.totalConfirmed, color: 'text-[#009de0]' },
            { label: 'Completed', value: summary.totalCompleted, color: 'text-[#68a200]' },
            { label: 'Cancelled', value: summary.totalCancelled, color: 'text-[#a5132a]' },
            { label: 'No-Show', value: summary.totalNoShow, color: 'text-[#7c7c7c]' },
            { label: 'Rescheduled', value: summary.totalRescheduled, color: 'text-[#00486d]' },
          ].map((stat) => (
            <div key={stat.label} className="bg-white border border-[#e1e1e1] rounded-xl p-4">
              <p className="text-xs text-[#7c7c7c] mb-1">{stat.label}</p>
              <p className={`text-2xl font-bold ${stat.color}`}>{stat.value}</p>
            </div>
          ))}
          <div className="bg-white border border-[#e1e1e1] rounded-xl p-4 col-span-2 md:col-span-1 flex flex-col justify-center">
            <p className="text-xs text-[#abb3b7]">Last 30 days</p>
          </div>
        </div>
      )}

      <div className="bg-white border border-[#e1e1e1] rounded-xl overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#e1e1e1]">
          <h2 className="text-sm font-semibold text-[#383634]">Recent Appointments</h2>
          <button
            onClick={() => navigate('/admin/appointments')}
            className="text-xs text-[#009de0] hover:text-[#0084d5] font-medium"
          >
            View all
          </button>
        </div>
        {recent.length === 0 ? (
          <div className="px-5 py-8 text-center text-sm text-[#abb3b7]">No appointments yet.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[#e1e1e1] text-xs text-[#7c7c7c]">
                <th className="text-left px-5 py-3 font-medium">Reference</th>
                <th className="text-left px-4 py-3 font-medium">Customer</th>
                <th className="text-left px-4 py-3 font-medium">Service</th>
                <th className="text-left px-4 py-3 font-medium">Date</th>
                <th className="text-left px-4 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((appt) => (
                <tr
                  key={appt.id}
                  onClick={() => navigate('/admin/appointments')}
                  className="border-b border-[#e1e1e1] hover:bg-[#f8f8f9] cursor-pointer"
                >
                  <td className="px-5 py-3 font-mono text-xs text-[#abb3b7]">{appt.referenceNumber}</td>
                  <td className="px-4 py-3 text-[#383634]">
                    {appt.customerFirstName} {appt.customerLastName}
                  </td>
                  <td className="px-4 py-3 text-[#383634]">{appt.serviceName}</td>
                  <td className="px-4 py-3 text-[#7c7c7c]">{appt.appointmentDate}</td>
                  <td className="px-4 py-3">
                    <span
                      className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[appt.status]}`}
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
