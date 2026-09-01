import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getServices } from '../api/services'
import { getBranches } from '../api/branches'
import { getAvailability } from '../api/availability'
import { createAppointment } from '../api/appointments'
import type {
  BankingServiceResponse,
  BranchResponse,
  SlotResponse,
  AppointmentResponse,
} from '../types'

type Step = 1 | 2 | 3 | 4 | 5

const STEP_LABELS = [
  'Service',
  'Branch',
  'Date',
  'Time Slot',
  'Confirm',
]

export default function BookPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<Step>(1)

  const [services, setServices] = useState<BankingServiceResponse[]>([])
  const [branches, setBranches] = useState<BranchResponse[]>([])
  const [slots, setSlots] = useState<SlotResponse[]>([])

  const [selectedService, setSelectedService] = useState<BankingServiceResponse | null>(null)
  const [selectedBranch, setSelectedBranch] = useState<BranchResponse | null>(null)
  const [selectedDate, setSelectedDate] = useState('')
  const [selectedSlot, setSelectedSlot] = useState<SlotResponse | null>(null)
  const [notes, setNotes] = useState('')

  const [loadingServices, setLoadingServices] = useState(false)
  const [loadingBranches, setLoadingBranches] = useState(false)
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const today = new Date()
  const minDate = new Date(today)
  minDate.setDate(minDate.getDate() + 1)
  const minDateStr = minDate.toISOString().split('T')[0]

  useEffect(() => {
    setLoadingServices(true)
    getServices()
      .then((data) => setServices(data.filter((s) => s.active)))
      .catch(() => setError('Failed to load services.'))
      .finally(() => setLoadingServices(false))
  }, [])

  useEffect(() => {
    if (step !== 2) return
    setLoadingBranches(true)
    getBranches()
      .then((data) => setBranches(data.filter((b) => b.active)))
      .catch(() => setError('Failed to load branches.'))
      .finally(() => setLoadingBranches(false))
  }, [step])

  useEffect(() => {
    if (step !== 4 || !selectedBranch || !selectedService || !selectedDate) return
    setLoadingSlots(true)
    setSlots([])
    setSelectedSlot(null)
    getAvailability(selectedBranch.id, selectedService.id, selectedDate)
      .then((data) => setSlots(data.slots))
      .catch(() => setError('Failed to load availability.'))
      .finally(() => setLoadingSlots(false))
  }, [step, selectedBranch, selectedService, selectedDate])

  function next() {
    setError('')
    setStep((s) => (s + 1) as Step)
  }

  function back() {
    setError('')
    setStep((s) => (s - 1) as Step)
  }

  async function handleBook() {
    if (!selectedService || !selectedBranch || !selectedDate || !selectedSlot) return
    setError('')
    setSubmitting(true)
    try {
      const appt: AppointmentResponse = await createAppointment({
        branchId: selectedBranch.id,
        serviceId: selectedService.id,
        appointmentDate: selectedDate,
        startTime: selectedSlot.startTime,
        notes: notes || undefined,
      })
      navigate(`/booking-confirmation?ref=${appt.referenceNumber}`, { state: { appointment: appt } })
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Booking failed. The slot may no longer be available.'
      setError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 px-4 py-10">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-gray-900 mb-1">Book an Appointment</h1>
        <p className="text-sm text-gray-500 mb-6">Complete each step to confirm your booking</p>

        <StepIndicator current={step} />

        <div className="bg-white border border-gray-200 rounded-2xl shadow-sm p-6 mt-6">
          {error && (
            <div className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3">
              {error}
            </div>
          )}

          {step === 1 && (
            <Step1
              services={services}
              loading={loadingServices}
              selected={selectedService}
              onSelect={(s) => { setSelectedService(s); next() }}
            />
          )}
          {step === 2 && (
            <Step2
              branches={branches}
              loading={loadingBranches}
              selected={selectedBranch}
              onSelect={(b) => { setSelectedBranch(b); next() }}
              onBack={back}
            />
          )}
          {step === 3 && (
            <Step3
              value={selectedDate}
              minDate={minDateStr}
              onChange={setSelectedDate}
              onNext={() => { if (selectedDate) { next() } else { setError('Please select a date.') } }}
              onBack={back}
            />
          )}
          {step === 4 && (
            <Step4
              slots={slots}
              loading={loadingSlots}
              selected={selectedSlot}
              onSelect={(slot) => { setSelectedSlot(slot); next() }}
              onBack={back}
            />
          )}
          {step === 5 && selectedService && selectedBranch && selectedSlot && (
            <Step5
              service={selectedService}
              branch={selectedBranch}
              date={selectedDate}
              slot={selectedSlot}
              notes={notes}
              onNotesChange={setNotes}
              onBook={handleBook}
              onBack={back}
              submitting={submitting}
            />
          )}
        </div>
      </div>
    </div>
  )
}

function StepIndicator({ current }: { current: Step }) {
  return (
    <div className="flex items-center gap-1">
      {STEP_LABELS.map((label, i) => {
        const num = (i + 1) as Step
        const done = num < current
        const active = num === current
        return (
          <div key={label} className="flex items-center flex-1 min-w-0">
            <div className="flex flex-col items-center gap-1 flex-shrink-0">
              <div
                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-semibold ${
                  done
                    ? 'bg-purple-600 text-white'
                    : active
                    ? 'bg-purple-100 text-purple-700 ring-2 ring-purple-600'
                    : 'bg-gray-100 text-gray-400'
                }`}
              >
                {done ? (
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                ) : (
                  num
                )}
              </div>
              <span className={`text-xs hidden sm:block ${active ? 'text-purple-700 font-medium' : 'text-gray-400'}`}>
                {label}
              </span>
            </div>
            {i < STEP_LABELS.length - 1 && (
              <div className={`flex-1 h-0.5 mx-1 ${done ? 'bg-purple-600' : 'bg-gray-200'}`} />
            )}
          </div>
        )
      })}
    </div>
  )
}

function Step1({
  services,
  loading,
  selected,
  onSelect,
}: {
  services: BankingServiceResponse[]
  loading: boolean
  selected: BankingServiceResponse | null
  onSelect: (s: BankingServiceResponse) => void
}) {
  return (
    <div>
      <h2 className="text-base font-semibold text-gray-900 mb-4">Choose a Service</h2>
      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {services.map((s) => (
            <button
              key={s.id}
              onClick={() => onSelect(s)}
              className={`text-left p-4 rounded-xl border-2 transition-all ${
                selected?.id === s.id
                  ? 'border-purple-600 bg-purple-50'
                  : 'border-gray-200 hover:border-purple-300 hover:bg-gray-50'
              }`}
            >
              <p className="font-medium text-gray-900 text-sm">{s.name}</p>
              {s.description && (
                <p className="text-xs text-gray-500 mt-1 line-clamp-2">{s.description}</p>
              )}
              <p className="text-xs text-purple-600 font-medium mt-2">{s.durationMinutes} min</p>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function Step2({
  branches,
  loading,
  selected,
  onSelect,
  onBack,
}: {
  branches: BranchResponse[]
  loading: boolean
  selected: BranchResponse | null
  onSelect: (b: BranchResponse) => void
  onBack: () => void
}) {
  return (
    <div>
      <h2 className="text-base font-semibold text-gray-900 mb-4">Choose a Branch</h2>
      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {branches.map((b) => (
            <button
              key={b.id}
              onClick={() => onSelect(b)}
              className={`text-left p-4 rounded-xl border-2 transition-all ${
                selected?.id === b.id
                  ? 'border-purple-600 bg-purple-50'
                  : 'border-gray-200 hover:border-purple-300 hover:bg-gray-50'
              }`}
            >
              <p className="font-medium text-gray-900 text-sm">{b.name}</p>
              <p className="text-xs text-gray-500 mt-1">{b.address}</p>
              <p className="text-xs text-gray-400">{b.city}, {b.province}</p>
            </button>
          ))}
        </div>
      )}
      <BackButton onClick={onBack} />
    </div>
  )
}

function Step3({
  value,
  minDate,
  onChange,
  onNext,
  onBack,
}: {
  value: string
  minDate: string
  onChange: (d: string) => void
  onNext: () => void
  onBack: () => void
}) {
  return (
    <div>
      <h2 className="text-base font-semibold text-gray-900 mb-4">Choose a Date</h2>
      <input
        type="date"
        min={minDate}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 mb-4"
      />
      <div className="flex gap-3">
        <BackButton onClick={onBack} />
        <button
          onClick={onNext}
          disabled={!value}
          className="flex-1 bg-purple-600 hover:bg-purple-700 disabled:opacity-50 text-white font-medium py-2.5 rounded-lg transition-colors text-sm"
        >
          View Available Slots
        </button>
      </div>
    </div>
  )
}

function Step4({
  slots,
  loading,
  selected,
  onSelect,
  onBack,
}: {
  slots: SlotResponse[]
  loading: boolean
  selected: SlotResponse | null
  onSelect: (s: SlotResponse) => void
  onBack: () => void
}) {
  function fmt(time: string) {
    const [h, m] = time.split(':')
    const hour = parseInt(h, 10)
    const suffix = hour >= 12 ? 'PM' : 'AM'
    const display = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
    return `${display}:${m} ${suffix}`
  }

  return (
    <div>
      <h2 className="text-base font-semibold text-gray-900 mb-4">Choose a Time Slot</h2>
      {loading ? (
        <LoadingSpinner />
      ) : slots.length === 0 ? (
        <p className="text-sm text-gray-500">No slots available for this date. Please choose another date.</p>
      ) : (
        <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 mb-4">
          {slots.map((slot, i) => {
            const isAvailable = slot.status === 'AVAILABLE'
            const isSelected = selected?.startTime === slot.startTime
            return (
              <button
                key={i}
                disabled={!isAvailable}
                onClick={() => isAvailable && onSelect(slot)}
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
                {slot.status === 'BOOKED' && (
                  <span className="block text-[10px] mt-0.5">Taken</span>
                )}
              </button>
            )
          })}
        </div>
      )}
      <div className="flex gap-4 text-xs text-gray-500 mb-4">
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-green-400" /> Available
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-red-300" /> Taken
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-gray-200" /> Unavailable
        </span>
      </div>
      <BackButton onClick={onBack} />
    </div>
  )
}

function Step5({
  service,
  branch,
  date,
  slot,
  notes,
  onNotesChange,
  onBook,
  onBack,
  submitting,
}: {
  service: BankingServiceResponse
  branch: BranchResponse
  date: string
  slot: SlotResponse
  notes: string
  onNotesChange: (n: string) => void
  onBook: () => void
  onBack: () => void
  submitting: boolean
}) {
  function fmt(time: string) {
    const [h, m] = time.split(':')
    const hour = parseInt(h, 10)
    const suffix = hour >= 12 ? 'PM' : 'AM'
    const display = hour > 12 ? hour - 12 : hour === 0 ? 12 : hour
    return `${display}:${m} ${suffix}`
  }

  const [year, month, day] = date.split('-')
  const dateDisplay = new Date(parseInt(year), parseInt(month) - 1, parseInt(day)).toLocaleDateString('en-ZA', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })

  return (
    <div>
      <h2 className="text-base font-semibold text-gray-900 mb-4">Confirm Your Booking</h2>
      <div className="bg-gray-50 rounded-xl border border-gray-200 p-5 mb-5 space-y-3">
        <SummaryRow label="Service" value={service.name} />
        <SummaryRow label="Duration" value={`${service.durationMinutes} minutes`} />
        <SummaryRow label="Branch" value={`${branch.name}, ${branch.city}`} />
        <SummaryRow label="Date" value={dateDisplay} />
        <SummaryRow label="Time" value={`${fmt(slot.startTime)} – ${fmt(slot.endTime)}`} />
      </div>

      <div className="mb-5">
        <label className="block text-sm font-medium text-gray-700 mb-1">
          Notes <span className="text-gray-400 font-normal">(optional)</span>
        </label>
        <textarea
          value={notes}
          onChange={(e) => onNotesChange(e.target.value)}
          rows={3}
          placeholder="Any additional information for the branch…"
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none"
        />
      </div>

      <div className="flex gap-3">
        <BackButton onClick={onBack} />
        <button
          onClick={onBook}
          disabled={submitting}
          className="flex-1 bg-purple-600 hover:bg-purple-700 disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
        >
          {submitting ? 'Booking…' : 'Confirm Booking'}
        </button>
      </div>
    </div>
  )
}

function SummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between items-start gap-4 text-sm">
      <span className="text-gray-500 flex-shrink-0">{label}</span>
      <span className="text-gray-900 font-medium text-right">{value}</span>
    </div>
  )
}

function BackButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="px-4 py-2.5 border border-gray-300 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
    >
      Back
    </button>
  )
}

function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="w-6 h-6 border-3 border-purple-600 border-t-transparent rounded-full animate-spin" />
    </div>
  )
}
