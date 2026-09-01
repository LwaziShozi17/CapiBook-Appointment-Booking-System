import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  getAppointment,
  getAppointmentHistory,
  cancelAppointment,
  rescheduleAppointment,
} from '../api/appointments'
import { getServices } from '../api/services'
import { getBranches } from '../api/branches'
import { getAvailability } from '../api/availability'
import type {
  AppointmentResponse,
  AppointmentHistoryResponse,
  AppointmentStatus,
  BankingServiceResponse,
  BranchResponse,
  SlotResponse,
} from '../types'

const STATUS_STYLES: Record<AppointmentStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  CANCELLED: 'bg-red-100 text-red-700',
  COMPLETED: 'bg-green-100 text-green-800',
  NO_SHOW: 'bg-gray-100 text-gray-600',
  RESCHEDULED: 'bg-purple-100 text-purple-700',
}

export default function AppointmentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [appointment, setAppointment] = useState<AppointmentResponse | null>(null)
  const [history, setHistory] = useState<AppointmentHistoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [cancelling, setCancelling] = useState(false)
  const [showReschedule, setShowReschedule] = useState(false)

  useEffect(() => {
    if (!id) return
    Promise.all([getAppointment(id), getAppointmentHistory(id)])
      .then(([appt, hist]) => {
        setAppointment(appt)
        setHistory(hist)
      })
      .catch(() => setError('Failed to load appointment.'))
      .finally(() => setLoading(false))
  }, [id])

  async function handleCancel() {
    if (!id || !appointment) return
    if (!confirm('Are you sure you want to cancel this appointment?')) return
    setCancelling(true)
    setError('')
    try {
      const updated = await cancelAppointment(id)
      setAppointment(updated)
      const hist = await getAppointmentHistory(id)
      setHistory(hist)
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to cancel appointment.'
      setError(msg)
    } finally {
      setCancelling(false)
    }
  }

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
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="w-8 h-8 border-4 border-purple-600 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  if (!appointment) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="text-center">
          <p className="text-gray-500">{error || 'Appointment not found.'}</p>
          <button onClick={() => navigate('/appointments')} className="mt-4 text-purple-600 text-sm hover:underline">
            Back to appointments
          </button>
        </div>
      </div>
    )
  }

  const canCancel = appointment.status === 'PENDING' || appointment.status === 'CONFIRMED'
  const canReschedule = appointment.status === 'CONFIRMED'

  return (
    <div className="min-h-screen bg-gray-50 px-4 py-10">
      <div className="max-w-2xl mx-auto">
        <button
          onClick={() => navigate('/appointments')}
          className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700 mb-6 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to My Appointments
        </button>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-4">
            {error}
          </div>
        )}

        <div className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6 mb-4">
          <div className="flex items-start justify-between mb-5">
            <div>
              <h1 className="text-xl font-bold text-gray-900">{appointment.serviceName}</h1>
              <p className="text-sm text-gray-500 mt-0.5">{appointment.branchName}</p>
            </div>
            <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${STATUS_STYLES[appointment.status]}`}>
              {appointment.status}
            </span>
          </div>

          <div className="space-y-3 border-t border-gray-100 pt-4">
            <DetailRow label="Reference" value={appointment.referenceNumber} mono />
            <DetailRow label="Date" value={fmtDate(appointment.appointmentDate)} />
            <DetailRow label="Time" value={`${fmt(appointment.startTime)} – ${fmt(appointment.endTime)}`} />
            <DetailRow
              label="Customer"
              value={`${appointment.customerFirstName} ${appointment.customerLastName}`}
            />
            {appointment.notes && <DetailRow label="Notes" value={appointment.notes} />}
            <DetailRow
              label="Booked on"
              value={new Date(appointment.createdAt).toLocaleDateString('en-ZA', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            />
          </div>

          {(canCancel || canReschedule) && (
            <div className="flex gap-3 mt-6 pt-4 border-t border-gray-100">
              {canReschedule && (
                <button
                  onClick={() => setShowReschedule(true)}
                  className="flex-1 border border-purple-300 text-purple-700 hover:bg-purple-50 font-medium py-2.5 rounded-lg transition-colors text-sm"
                >
                  Reschedule
                </button>
              )}
              {canCancel && (
                <button
                  onClick={handleCancel}
                  disabled={cancelling}
                  className="flex-1 border border-red-300 text-red-600 hover:bg-red-50 disabled:opacity-50 font-medium py-2.5 rounded-lg transition-colors text-sm"
                >
                  {cancelling ? 'Cancelling…' : 'Cancel Appointment'}
                </button>
              )}
            </div>
          )}
        </div>

        {showReschedule && appointment && (
          <ReschedulePanel
            appointment={appointment}
            onClose={() => setShowReschedule(false)}
            onSuccess={async (updated) => {
              setAppointment(updated)
              setShowReschedule(false)
              if (id) {
                const hist = await getAppointmentHistory(id)
                setHistory(hist)
              }
            }}
          />
        )}

        {history.length > 0 && (
          <div className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6">
            <h2 className="text-base font-semibold text-gray-900 mb-4">History</h2>
            <div className="space-y-3">
              {history.map((h) => (
                <div key={h.id} className="flex items-start gap-3 text-sm">
                  <div className="w-2 h-2 rounded-full bg-purple-400 mt-1.5 flex-shrink-0" />
                  <div>
                    <p className="text-gray-700">
                      <span className="font-medium">{h.changedByFirstName} {h.changedByLastName}</span>
                      {h.previousStatus ? (
                        <> changed status from <StatusBadge s={h.previousStatus} /> to <StatusBadge s={h.newStatus} /></>
                      ) : (
                        <> created with status <StatusBadge s={h.newStatus} /></>
                      )}
                    </p>
                    {h.changeReason && <p className="text-gray-400 text-xs mt-0.5">{h.changeReason}</p>}
                    <p className="text-gray-400 text-xs mt-0.5">
                      {new Date(h.changedAt).toLocaleString('en-ZA')}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function DetailRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex justify-between items-start gap-4 text-sm">
      <span className="text-gray-500 flex-shrink-0">{label}</span>
      <span className={`text-gray-900 font-medium text-right ${mono ? 'font-mono' : ''}`}>{value}</span>
    </div>
  )
}

function StatusBadge({ s }: { s: AppointmentStatus }) {
  return (
    <span className={`inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium ${STATUS_STYLES[s]}`}>
      {s}
    </span>
  )
}

function ReschedulePanel({
  appointment,
  onClose,
  onSuccess,
}: {
  appointment: AppointmentResponse
  onClose: () => void
  onSuccess: (updated: AppointmentResponse) => void
}) {
  const [services, setServices] = useState<BankingServiceResponse[]>([])
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [slots, setSlots] = useState<SlotResponse[]>([])

  const [selectedService, setSelectedService] = useState<BankingServiceResponse | null>(null)
  const [selectedBranch, setSelectedBranch] = useState<BranchResponse | null>(null)
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null)
  const [reason, setReason] = useState('')

  const [step, setStep] = useState<1 | 2 | 3 | 4>(1)
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const today = new Date()
  const minDate = new Date(today)
  minDate.setDate(minDate.getDate() + 1)
  const minDateStr = minDate.toISOString().split('T')[0]

  useEffect(() => {
    Promise.all([getServices(), getBranches()]).then(([svcs, brs]) => {
      setServices(svcs.filter((s) => s.active))
      setBranches(brs.filter((b) => b.active))
    })
  }, [])

  useEffect(() => {
    if (step !== 4 || !selectedBranch || !selectedService || !selectedDate) return
    setLoadingSlots(true)
    setSlots([])
    setSelectedSlot(null)
    getAvailability(selectedBranch.id, selectedService.id, selectedDate)
      .then((data) => setSlots(data.slots))
      .catch(() => setError('Failed to load slots.'))
      .finally(() => setLoadingSlots(false))
  }, [step, selectedBranch, selectedService, selectedDate])

  async function handleSubmit() {
    if (!selectedService || !selectedBranch || !selectedDate || !selectedSlot) return
    setSubmitting(true)
    setError('')
    try {
      const updated = await rescheduleAppointment(appointment.id, {
        branchId: selectedBranch.id,
        serviceId: selectedService.id,
        appointmentDate: selectedDate,
        startTime: selectedSlot.startTime,
        reason: reason || undefined,
      })
      onSuccess(updated)
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Reschedule failed.'
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  function fmt(time: string) {
    const [h, m] = time.split(':')
    const hour = parseInt(h, 10)
    const suffix = hour >= 12 ? 'PM' : 'AM'
    const display = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
    return `${display}:${m} ${suffix}`
  }

  return (
    <div className="bg-white border border-purple-200 rounded-2xl shadow-sm p-6 mb-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-gray-900">Reschedule Appointment</h2>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {error && <p className="text-red-600 text-sm mb-3">{error}</p>}

      {step === 1 && (
        <div>
          <p className="text-sm font-medium text-gray-700 mb-3">Choose a service</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-56 overflow-y-auto">
            {services.map((s) => (
              <button
                key={s.id}
                onClick={() => { setSelectedService(s); setStep(2) }}
                className={`text-left p-3 rounded-xl border-2 text-sm transition-all ${
                  selectedService?.id === s.id
                    ? 'border-purple-600 bg-purple-50'
                    : 'border-gray-200 hover:border-purple-300'
                }`}
              >
                <p className="font-medium text-gray-900">{s.name}</p>
                <p className="text-xs text-purple-600">{s.durationMinutes} min</p>
              </button>
            ))}
          </div>
        </div>
      )}

      {step === 2 && (
        <div>
          <p className="text-sm font-medium text-gray-700 mb-3">Choose a branch</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-56 overflow-y-auto mb-3">
            {branches.map((b) => (
              <button
                key={b.id}
                onClick={() => { setSelectedBranch(b); setStep(3) }}
                className={`text-left p-3 rounded-xl border-2 text-sm transition-all ${
                  selectedBranch?.id === b.id
                    ? 'border-purple-600 bg-purple-50'
                    : 'border-gray-200 hover:border-purple-300'
                }`}
              >
                <p className="font-medium text-gray-900">{b.name}</p>
                <p className="text-xs text-gray-500">{b.city}</p>
              </button>
            ))}
          </div>
          <button onClick={() => setStep(1)} className="text-sm text-gray-500 hover:text-gray-700">
            ← Back
          </button>
        </div>
      )}

      {step === 3 && (
        <div>
          <p className="text-sm font-medium text-gray-700 mb-3">Choose a date</p>
          <input
            type="date"
            min={minDateStr}
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 mb-3"
          />
          <div className="flex gap-2">
            <button onClick={() => setStep(2)} className="text-sm text-gray-500 hover:text-gray-700 px-3 py-2">
              ← Back
            </button>
            <button
              disabled={!selectedDate}
              onClick={() => setStep(4)}
              className="flex-1 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white text-sm font-medium py-2 rounded-lg transition-colors"
            >
              View Slots
            </button>
          </div>
        </div>
      )}

      {step === 4 && (
        <div>
          <p className="text-sm font-medium text-gray-700 mb-3">Choose a time slot</p>
          {loadingSlots ? (
            <div className="flex justify-center py-6">
              <div className="w-6 h-6 border-3 border-purple-600 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : slots.length === 0 ? (
            <p className="text-sm text-gray-500 mb-3">No slots available. Try a different date.</p>
          ) : (
            <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 mb-4 max-h-48 overflow-y-auto">
              {slots.map((slot, i) => {
                const isAvailable = slot.status === 'AVAILABLE'
                const isSelected = selectedSlot?.startTime === slot.startTime
                return (
                  <button
                    key={i}
                    disabled={!isAvailable}
                    onClick={() => isAvailable && setSelectedSlot(slot)}
                    className={`py-2 px-1 rounded-lg text-xs font-medium border-2 transition-all ${
                      isSelected
                        ? 'border-purple-600 bg-purple-600 text-white'
                        : isAvailable
                        ? 'border-green-400 bg-green-50 text-green-800 hover:bg-green-100'
                        : slot.status === 'BOOKED'
                        ? 'border-red-200 bg-red-50 text-red-400 cursor-not-allowed'
                        : 'border-gray-200 bg-gray-50 text-gray-300 cursor-not-allowed'
                    }`}
                  >
                    {fmt(slot.startTime)}
                    {slot.status === 'BOOKED' && <span className="block text-[10px]">Taken</span>}
                  </button>
                )
              })}
            </div>
          )}

          <div className="mb-3">
            <input
              type="text"
              placeholder="Reason for rescheduling (optional)"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
            />
          </div>

          <div className="flex gap-2">
            <button onClick={() => setStep(3)} className="text-sm text-gray-500 hover:text-gray-700 px-3 py-2">
              ← Back
            </button>
            <button
              disabled={!selectedSlot || submitting}
              onClick={handleSubmit}
              className="flex-1 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white text-sm font-medium py-2 rounded-lg transition-colors"
            >
              {submitting ? 'Rescheduling…' : 'Confirm Reschedule'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
