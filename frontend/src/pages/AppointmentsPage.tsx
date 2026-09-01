import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyAppointments } from '../api/appointments'
import type { AppointmentResponse, AppointmentStatus } from '../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-[#fff3e0] text-[#d66700]',
  CONFIRMED: 'bg-[#f2fafd] text-[#009de0]',
  CANCELLED: 'bg-[#fdf2f4] text-[#a5132a]',
  COMPLETED: 'bg-[#f0f7e6] text-[#68a200]',
  NO_SHOW: 'bg-[#efefef] text-[#7c7c7c]',
  RESCHEDULED: 'bg-[#edf8fd] text-[#00486d]',
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
    <div className="min-h-screen bg-[#f5f5f5] px-4 py-10">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-semibold text-[#383634]">My Appointments</h1>
            <p className="text-sm text-[#7c7c7c] mt-0.5">View and manage your bookings</p>
          </div>
          <button
            onClick={() => navigate('/book')}
            className="bg-[#009de0] hover:bg-[#0084d5] text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors"
          >
            Book New
          </button>
        </div>

        {error && (
          <div className="bg-[#fdf2f4] border border-[#fde8eb] text-[#a5132a] text-sm rounded-lg px-4 py-3 mb-4">
            {error}
          </div>
        )}

        {loading ? (
          <div className="flex justify-center py-16">
            <div className="w-8 h-8 border-4 border-[#009de0] border-t-transparent rounded-full animate-spin" />
          </div>
        ) : appointments.length === 0 ? (
          <div className="text-center py-16 bg-white border border-[#e1e1e1] rounded-2xl">
            <div className="w-12 h-12 bg-[#efefef] rounded-full flex items-center justify-center mx-auto mb-3">
              <svg className="w-6 h-6 text-[#abb3b7]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
            </div>
            <p className="text-[#7c7c7c] text-sm font-medium">No appointments yet</p>
            <p className="text-[#abb3b7] text-xs mt-1">Book your first appointment to get started</p>
            <button
              onClick={() => navigate('/book')}
              className="mt-4 bg-[#009de0] hover:bg-[#0084d5] text-white text-sm font-medium px-5 py-2 rounded-lg transition-colors"
            >
              Book Appointment
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {appointments.map((appt) => (
              <button
                key={appt.id}
                onClick={() => navigate(`/appointments/${appt.id}`)}
                className="bg-white border border-[#e1e1e1] rounded-xl p-5 shadow-sm text-left hover:border-[#009de0]/50 hover:shadow-md transition-all w-full"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="font-semibold text-[#383634] text-sm">{appt.serviceName}</p>
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[appt.status]}`}
                      >
                        {appt.status}
                      </span>
                    </div>
                    <p className="text-sm text-[#7c7c7c] mt-0.5">{appt.branchName}</p>
                    <p className="text-xs text-[#abb3b7] mt-1">
                      {fmtDate(appt.appointmentDate)} · {fmt(appt.startTime)} – {fmt(appt.endTime)}
                    </p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="text-xs font-mono text-[#abb3b7]">{appt.referenceNumber}</p>
                    <svg className="w-4 h-4 text-[#abb3b7] ml-auto mt-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
              className="px-4 py-2 text-sm border border-[#e1e1e1] rounded-lg disabled:opacity-40 hover:bg-[#f8f8f9] transition-colors"
            >
              Previous
            </button>
            <span className="text-sm text-[#7c7c7c]">
              Page {page + 1} of {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="px-4 py-2 text-sm border border-[#e1e1e1] rounded-lg disabled:opacity-40 hover:bg-[#f8f8f9] transition-colors"
            >
              Next
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
