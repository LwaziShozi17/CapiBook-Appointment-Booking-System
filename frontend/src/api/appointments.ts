import apiClient from './apiClient'
import type {
  AppointmentResponse,
  AppointmentHistoryResponse,
  CreateAppointmentRequest,
  RescheduleAppointmentRequest,
  PageResponse,
} from '../types'

export async function createAppointment(req: CreateAppointmentRequest): Promise<AppointmentResponse> {
  const { data } = await apiClient.post<AppointmentResponse>('/appointments', req)
  return data
}

export async function getMyAppointments(
  page = 0,
  size = 10
): Promise<PageResponse<AppointmentResponse>> {
  const { data } = await apiClient.get<PageResponse<AppointmentResponse>>('/appointments/my', {
    params: { page, size },
  })
  return data
}

export async function getAppointment(id: string): Promise<AppointmentResponse> {
  const { data } = await apiClient.get<AppointmentResponse>(`/appointments/${id}`)
  return data
}

export async function getAppointmentHistory(id: string): Promise<AppointmentHistoryResponse[]> {
  const { data } = await apiClient.get<AppointmentHistoryResponse[]>(`/appointments/${id}/history`)
  return data
}

export async function cancelAppointment(id: string): Promise<AppointmentResponse> {
  const { data } = await apiClient.patch<AppointmentResponse>(`/appointments/${id}/cancel`, {})
  return data
}

export async function rescheduleAppointment(
  id: string,
  req: RescheduleAppointmentRequest
): Promise<AppointmentResponse> {
  const { data } = await apiClient.patch<AppointmentResponse>(`/appointments/${id}/reschedule`, req)
  return data
}
