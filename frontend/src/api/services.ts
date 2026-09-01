import apiClient from './apiClient'
import type { BankingServiceResponse } from '../types'

export async function getServices(): Promise<BankingServiceResponse[]> {
  const { data } = await apiClient.get<BankingServiceResponse[]>('/services')
  return data
}
