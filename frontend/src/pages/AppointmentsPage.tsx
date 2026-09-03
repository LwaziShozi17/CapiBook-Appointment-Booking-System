import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyAppointments } from '../api/appointments'
import type { AppointmentResponse, AppointmentStatus } from '../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-[#fff7ed] text-[#ea580c] border border-[#fed7aa]',
  CONFIRMED: 'bg-[#eff9ff] text-[#009de0] border border-[#bae6fd]',
  CANCELLED: 'bg-[#fef2f2] text-[#dc2626] border border-[#fecaca]',
  COMPLETED: 'bg-[#f0fdf4] text-[#16a34a] border border-[#bbf7d0]',
  NO_SHOW: 'bg-[#f8fafc] text-[#64748b] border border-[#e2e8f0]',
  RESCHEDULED: 'bg-[#ecfeff] text-[#0891b2] border border-[#a5f3fc]',
}

export default function AppointmentsPage() {
  const navigate = useNavigate()
  const [appointments, setAppointments] = useState<AppointmentResponse[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    getMyAppointments(page, 10)
      .then((res) => {
        setAppointments(res.content)
        setTotalPages(res.totalPages)
      })
      .catch(() => setError('Failed to load appointments.'))
      .finally(() => setLoading(false))
  }, [page])

  function fmt(time: string) {
    const [h, m] = time.split(':')
    const hour = parseInt(h, 10)
    const suffix = hour >= 12 ? 'PM' : 'AM'
    const display = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
    return `${display}:${m} ${suffix}`
  }

  function fmtDate(dateStr: string) {
    const [y, mo, d] = dateStr.split('-')
    return new Date(parseInt(y), parseInt(mo) - 1, parseInt(d)).toLocaleDateString('en-ZA', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  }

  return (
    <div className="min-h-screen bg-[#f8fafc] px-4 py-10">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-[#0f172a]">My Appointments</h1>
            <p className="text-sm text-[#64748b] mt-0.5">View and manage your bookings</p>
          </div>
          <button
            onClick={() => navigate('/book')}
            className="bg-[#009de0] hover:bg-[#0085c3] text-white text-sm font-semibold px-4 py-2.5 rounded-lg transition-colors"
          >
            Book new
          </button>
        </div>

        {error && (
          <div className="bg-[#fef2f2] border border-[#fecaca] text-[#dc2626] text-sm rounded-xl px-4 py-3 mb-4">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex justify-center py-20">
            <div className="w-8 h-8 border-4 border-[#009de0] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : appointments.length === 0 ? (
          <div className="text-center py-16 bg-white border border-[#e2e8f0] rounded-2xl">
            <div className="w-12 h-12 bg-[#f1f5f9] rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-[#94a3b8]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <p className="text-[#0f172a] text-sm font-semibold mb-1">No appointments yet</p>
            <p className="text-[#94a3b8] text-xs">Book your first appointment to get started</p>
            <button
              onClick={() => navigate('/book')}
              className="mt-5 bg-[#009de0] hover:bg-[#0085c3] text-white text-sm font-semibold px-5 py-2.5 rounded-lg transition-colors"
            >
              Book appointment
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {appointments.map((appt) => (
              <button
                key={appt.id}
                onClick={() => navigate(`/appointments/${appt.id}`)}
                className="bg-white border border-[#e2e8f0] rounded-xl p-5 text-left hover:border-[#009de0]/50 hover:shadow-md transition-all w-full group"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap mb-1">
                      <p className="font-semibold text-[#0f172a] text-sm">{appt.serviceName}</p>
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${STATUS_STYLES[appt.status]}`}
                      >
                        {appt.status}
                      </span>
                    </div>
                    <p className="text-sm text-[#64748b]">{appt.branchName}</p>
                    <p className="text-xs text-[#94a3b8] mt-1">
                      {fmtDate(appt.appointmentDate)} · {fmt(appt.startTime)} – {fmt(appt.endTime)}
                    </p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="text-xs font-mono text-[#94a3b8]">{appt.referenceNumber}</p>
                    <svg
                      className="w-4 h-4 text-[#cbd5e1] group-hover:text-[#009de0] ml-auto mt-2 transition-colors"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-6">
            <button
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
              className="px-4 py-2 text-sm font-medium border border-[#e2e8f0] rounded-lg disabled:opacity-40 disabled:cursor-not-allowed hover:bg-white transition-colors text-[#64748b]"
            >
              Previous
            </button>
            <span className="text-sm text-[#64748b] px-2">
              {page + 1} / {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-4 py-2 text-sm font-medium border border-[#e2e8f0] rounded-lg disabled:opacity-40 disabled:cursor-not-allowed hover:bg-white transition-colors text-[#64748b]"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
