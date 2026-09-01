import apiClient from './apiClient'
import type { BankingServiceResponse, CreateServiceRequest, UpdateServiceRequest } from '../types'

export async function getServices(): Promise<BankingServiceResponse[]> {
  const { data } = await apiClient.get<BankingServiceResponse[]>('/services')
  return data
}

export async function createService(req: CreateServiceRequest): Promise<BankingServiceResponse> {
  const { data } = await apiClient.post<BankingServiceResponse>('/services', req)
  return data
}

export async function updateService(id: string, req: UpdateServiceRequest): Promise<BankingServiceResponse> {
  const { data } = await apiClient.put<BankingServiceResponse>(`/services/${id}`, req)
  return data
}

export async function deleteService(id: string): Promise<void> {
  await apiClient.delete(`/services/${id}`)
}
