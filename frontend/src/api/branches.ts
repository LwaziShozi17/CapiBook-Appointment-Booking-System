import apiClient from './apiClient'
import type { BranchResponse } from '../types'

export async function getBranches(): Promise<BranchResponse[]> {
  const { data } = await apiClient.get<BranchResponse[]>('/branches')
  return data
}
