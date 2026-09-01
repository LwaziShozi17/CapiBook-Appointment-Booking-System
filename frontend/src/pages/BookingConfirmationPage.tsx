import { Link, useLocation, useSearchParams } from 'react-router-dom'
import type { AppointmentResponse } from '../types'

export default function BookingConfirmationPage() {
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const appointment = location.state?.appointment as AppointmentResponse | undefined
  const ref = appointment?.referenceNumber ?? searchParams.get('ref') ?? ''

  function fmt(time: string) {
    const [h, m] = time.split(':')
    const hour = parseInt(h, 10)
    const suffix = hour >= 12 ? 'PM' : 'AM'
    const display = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
    return `${display}:${m} ${suffix}`
  }

  return (
    <div className="min-h-screen bg-[#f5f5f5] flex items-center justify-center px-4 py-12">
      <div className="max-w-md w-full text-center">
        <div className="w-16 h-16 bg-[#f0f7e6] rounded-full flex items-center justify-center mx-auto mb-5">
          <svg className="w-8 h-8 text-[#68a200]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <h1 className="text-2xl font-semibold text-[#383634] mb-2">Booking Confirmed!</h1>
        <p className="text-[#7c7c7c] text-sm mb-6">
          Your appointment has been successfully booked. You'll receive a confirmation notification shortly.
        </p>

        {ref && (
          <div className="inline-block bg-[#f2fafd] border border-[#009de0]/20 rounded-xl px-6 py-3 mb-6">
            <p className="text-xs text-[#009de0] font-medium uppercase tracking-wide mb-1">Reference Number</p>
            <p className="text-xl font-semibold text-[#00486d] font-mono">{ref}</p>
          </div>
        )}

        {appointment && (
          <div className="bg-white border border-[#e1e1e1] rounded-xl p-5 mb-6 text-left space-y-3">
            <Detail label="Service" value={appointment.serviceName} />
            <Detail label="Branch" value={appointment.branchName} />
            <Detail
              label="Date"
              value={new Date(appointment.appointmentDate + 'T00:00:00').toLocaleDateString('en-ZA', {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            />
            <Detail label="Time" value={`${fmt(appointment.startTime)} – ${fmt(appointment.endTime)}`} />
            <Detail label="Status" value={appointment.status} />
          </div>
        )}

        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          {appointment && (
            <Link
              to={`/appointments/${appointment.id}`}
              className="bg-[#009de0] hover:bg-[#0084d5] text-white font-medium px-6 py-2.5 rounded-lg transition-colors text-sm"
            >
              View Appointment
            </Link>
          )}
          <Link
            to="/book"
            className="border border-[#e1e1e1] text-[#383634] hover:bg-[#f8f8f9] font-medium px-6 py-2.5 rounded-lg transition-colors text-sm"
          >
            Book Another
          </Link>
          <Link
            to="/appointments"
            className="border border-[#e1e1e1] text-[#383634] hover:bg-[#f8f8f9] font-medium px-6 py-2.5 rounded-lg transition-colors text-sm"
          >
            My Appointments
          </Link>
        </div>
      </div>
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-[#7c7c7c]">{label}</span>
      <span className="text-[#383634] font-medium">{value}</span>
    </div>
  )
}
