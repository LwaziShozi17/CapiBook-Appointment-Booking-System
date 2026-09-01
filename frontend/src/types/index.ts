export interface UserProfileResponse {
  id: string
  email: string
  firstName: string
  lastName: string
  phoneNumber: string | null
  role: 'CUSTOMER' | 'BRANCH_ADMIN' | 'SYSTEM_ADMIN'
  branchId: string | null
  createdAt: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  firstName: string
  lastName: string
  phoneNumber?: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserProfileResponse
}

export interface UpdateProfileRequest {
  firstName: string
  lastName: string
  phoneNumber?: string
}

export interface BankingServiceResponse {
  id: string
  name: string
  description: string
  durationMinutes: number
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface OperatingHoursEntry {
  dayOfWeek: string
  openTime: string
  closeTime: string
  closed: boolean
}

export interface BranchResponse {
  id: string
  branchCode: string
  name: string
  address: string
  city: string
  province: string
  postalCode: string
  latitude: number | null
  longitude: number | null
  phoneNumber: string | null
  email: string | null
  active: boolean
  maxConcurrentAppointments: number
  operatingHours: OperatingHoursEntry[]
  createdAt: string
  updatedAt: string
}

export type SlotStatus = 'AVAILABLE' | 'BOOKED' | 'UNAVAILABLE'

export interface SlotResponse {
  startTime: string
  endTime: string
  status: SlotStatus
}

export interface AvailabilityResponse {
  branchId: string
  serviceId: string
  date: string
  slots: SlotResponse[]
}

export type AppointmentStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'COMPLETED'
  | 'NO_SHOW'
  | 'RESCHEDULED'

export interface AppointmentResponse {
  id: string
  referenceNumber: string
  customerId: string
  customerFirstName: string
  customerLastName: string
  branchId: string
  branchName: string
  serviceId: string
  serviceName: string
  appointmentDate: string
  startTime: string
  endTime: string
  status: AppointmentStatus
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateAppointmentRequest {
  branchId: string
  serviceId: string
  appointmentDate: string
  startTime: string
  notes?: string
}

export interface RescheduleAppointmentRequest {
  branchId: string
  serviceId: string
  appointmentDate: string
  startTime: string
  reason?: string
  notes?: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface AppointmentHistoryResponse {
  id: string
  appointmentId: string
  previousStatus: AppointmentStatus | null
  newStatus: AppointmentStatus
  changedById: string
  changedByFirstName: string
  changedByLastName: string
  changeReason: string | null
  changedAt: string
}
