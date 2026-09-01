import apiClient from './apiClient'
import type { AvailabilityResponse } from '../types'

export async function getAvailability(
  branchId: string,
  serviceId: string,
  date: string
): Promise<AvailabilityResponse> {
  const { data } = await apiClient.get<AvailabilityResponse>('/availability', {
    params: { branchId, serviceId, date },
  })
  return data
}
