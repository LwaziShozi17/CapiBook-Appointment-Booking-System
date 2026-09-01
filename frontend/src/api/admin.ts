import apiClient from './apiClient'
import type {
  AdminUserResponse,
  CreateBranchAdminRequest,
  AppointmentSummaryResponse,
  AuditLogResponse,
  AvailabilityExceptionResponse,
  CreateAvailabilityExceptionRequest,
  BranchUtilisationResponse,
  ServicePopularityResponse,
  PageResponse,
} from '../types'

export async function listAdminUsers(
  page = 0,
  size = 20
): Promise<PageResponse<AdminUserResponse>> {
  const { data } = await apiClient.get<PageResponse<AdminUserResponse>>('/admin/users', {
    params: { page, size },
  })
  return data
}

export async function createBranchAdmin(req: CreateBranchAdminRequest): Promise<AdminUserResponse> {
  const { data } = await apiClient.post<AdminUserResponse>('/admin/users/branch-admins', req)
  return data
}

export async function deactivateUser(userId: string): Promise<AdminUserResponse> {
  const { data } = await apiClient.put<AdminUserResponse>(`/admin/users/${userId}/deactivate`, {})
  return data
}

export async function listAuditLogs(page = 0, size = 20): Promise<PageResponse<AuditLogResponse>> {
  const { data } = await apiClient.get<PageResponse<AuditLogResponse>>('/admin/audit-logs', {
    params: { page, size },
  })
  return data
}

export async function getAppointmentSummary(
  from: string,
  to: string
): Promise<AppointmentSummaryResponse> {
  const { data } = await apiClient.get<AppointmentSummaryResponse>(
    '/admin/analytics/appointments/summary',
    { params: { from, to } }
  )
  return data
}

export async function getBranchUtilisation(
  branchId: string,
  from: string,
  to: string
): Promise<BranchUtilisationResponse> {
  const { data } = await apiClient.get<BranchUtilisationResponse>(
    `/admin/analytics/branches/${branchId}/utilisation`,
    { params: { from, to } }
  )
  return data
}

export async function getServicePopularity(
  from: string,
  to: string
): Promise<ServicePopularityResponse[]> {
  const { data } = await apiClient.get<ServicePopularityResponse[]>(
    '/admin/analytics/services/popularity',
    { params: { from, to } }
  )
  return data
}

export async function createAvailabilityException(
  branchId: string,
  req: CreateAvailabilityExceptionRequest
): Promise<AvailabilityExceptionResponse> {
  const { data } = await apiClient.post<AvailabilityExceptionResponse>(
    `/admin/branches/${branchId}/exceptions`,
    req
  )
  return data
}

export async function listAvailabilityExceptions(
  branchId: string
): Promise<AvailabilityExceptionResponse[]> {
  const { data } = await apiClient.get<AvailabilityExceptionResponse[]>(
    `/admin/branches/${branchId}/exceptions`
  )
  return data
}

export async function deleteAvailabilityException(
  branchId: string,
  exceptionId: string
): Promise<void> {
  await apiClient.delete(`/admin/branches/${branchId}/exceptions/${exceptionId}`)
}
