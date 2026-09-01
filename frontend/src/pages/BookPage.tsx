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
    <div className="min-h-screen bg-[#f5f5f5] px-4 py-10">
      <div className="max-w-2xl mx-auto">
        <h1 className="text-2xl font-bold text-[#383634] mb-1">Book an Appointment</h1>
        <p className="text-sm text-[#7c7c7c] mb-6">Complete each step to confirm your booking</p>

        <StepIndicator current={step} />

        <div className="bg-white border border-[#e1e1e1] rounded-2xl shadow-sm p-6 mt-6">
          {error && (
            <div className="mb-4 bg-[#fdf2f4] border border-[#fde8eb] text-[#a5132a] text-sm rounded-lg px-4 py-3">
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
                    ? 'bg-[#009de0] text-white'
                    : active
                    ? 'bg-[#f2fafd] text-[#009de0] ring-2 ring-[#009de0]'
                    : 'bg-[#efefef] text-[#abb3b7]'
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
              <span className={`text-xs hidden sm:block ${active ? 'text-[#009de0] font-medium' : 'text-[#abb3b7]'}`}>
                {label}
              </span>
            </div>
            {i < STEP_LABELS.length - 1 && (
              <div className={`flex-1 h-0.5 mx-1 ${done ? 'bg-[#009de0]' : 'bg-[#e1e1e1]'}`} />
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
      <h2 className="text-base font-semibold text-[#383634] mb-4">Choose a Service</h2>
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
                  ? 'border-[#009de0] bg-[#f2fafd]'
                  : 'border-[#e1e1e1] hover:border-[#009de0]/50 hover:bg-[#f8f8f9]'
              }`}
            >
              <p className="font-medium text-[#383634] text-sm">{s.name}</p>
              {s.description && (
                <p className="text-xs text-[#7c7c7c] mt-1 line-clamp-2">{s.description}</p>
              )}
              <p className="text-xs text-[#009de0] font-medium mt-2">{s.durationMinutes} min</p>
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
      <h2 className="text-base font-semibold text-[#383634] mb-4">Choose a Branch</h2>
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
                  ? 'border-[#009de0] bg-[#f2fafd]'
                  : 'border-[#e1e1e1] hover:border-[#009de0]/50 hover:bg-[#f8f8f9]'
              }`}
            >
              <p className="font-medium text-[#383634] text-sm">{b.name}</p>
              <p className="text-xs text-[#7c7c7c] mt-1">{b.address}</p>
              <p className="text-xs text-[#abb3b7]">{b.city}, {b.province}</p>
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
      <h2 className="text-base font-semibold text-[#383634] mb-4">Choose a Date</h2>
      <input
        type="date"
        min={minDate}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0] mb-4"
      />
      <div className="flex gap-3">
        <BackButton onClick={onBack} />
        <button
          onClick={onNext}
          disabled={!value}
          className="flex-1 bg-[#009de0] hover:bg-[#0084d5] disabled:opacity-50 text-white font-medium py-2.5 rounded-lg transition-colors text-sm"
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
      <h2 className="text-base font-semibold text-[#383634] mb-4">Choose a Time Slot</h2>
      {loading ? (
        <LoadingSpinner />
      ) : slots.length === 0 ? (
        <p className="text-sm text-[#7c7c7c]">No slots available for this date. Please choose another date.</p>
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
                    ? 'border-[#009de0] bg-[#009de0] text-white'
                    : isAvailable
                    ? 'border-green-400 bg-green-50 text-green-800 hover:bg-green-100'
                    : slot.status === 'BOOKED'
                    ? 'border-red-200 bg-red-50 text-red-400 cursor-not-allowed'
                    : 'border-[#e1e1e1] bg-[#f5f5f5] text-[#abb3b7] cursor-not-allowed'
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
      <div className="flex gap-4 text-xs text-[#7c7c7c] mb-4">
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-green-400" /> Available
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-red-300" /> Taken
        </span>
        <span className="flex items-center gap-1">
          <span className="w-3 h-3 rounded bg-[#e1e1e1]" /> Unavailable
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
      <h2 className="text-base font-semibold text-[#383634] mb-4">Confirm Your Booking</h2>
      <div className="bg-[#f5f5f5] rounded-xl border border-[#e1e1e1] p-5 mb-5 space-y-3">
        <SummaryRow label="Service" value={service.name} />
        <SummaryRow label="Duration" value={`${service.durationMinutes} minutes`} />
        <SummaryRow label="Branch" value={`${branch.name}, ${branch.city}`} />
        <SummaryRow label="Date" value={dateDisplay} />
        <SummaryRow label="Time" value={`${fmt(slot.startTime)} – ${fmt(slot.endTime)}`} />
      </div>

      <div className="mb-5">
        <label className="block text-sm font-medium text-[#383634] mb-1">
          Notes <span className="text-[#abb3b7] font-normal">(optional)</span>
        </label>
        <textarea
          value={notes}
          onChange={(e) => onNotesChange(e.target.value)}
          rows={3}
          placeholder="Any additional information for the branch…"
          className="w-full border border-[#e1e1e1] rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-[#009de0] resize-none"
        />
      </div>

      <div className="flex gap-3">
        <BackButton onClick={onBack} />
        <button
          onClick={onBook}
          disabled={submitting}
          className="flex-1 bg-[#009de0] hover:bg-[#0084d5] disabled:opacity-60 text-white font-semibold py-2.5 rounded-lg transition-colors text-sm"
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
      <span className="text-[#7c7c7c] flex-shrink-0">{label}</span>
      <span className="text-[#383634] font-medium text-right">{value}</span>
    </div>
  )
}

function BackButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="px-4 py-2.5 border border-[#e1e1e1] rounded-lg text-sm font-medium text-[#7c7c7c] hover:bg-[#f8f8f9] transition-colors"
    >
      Back
    </button>
  )
}

function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="w-6 h-6 border-3 border-[#009de0] border-t-transparent rounded-full animate-spin" />
    </div>
  )
}
