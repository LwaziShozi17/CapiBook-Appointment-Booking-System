import apiClient from './apiClient'
import type { BranchResponse, CreateBranchRequest, UpdateBranchRequest, OperatingHoursEntry } from '../types'

export async function getBranches(): Promise<BranchResponse[]> {
  const { data } = await apiClient.get<BranchResponse[]>('/branches')
  return data
}

export async function getBranch(id: string): Promise<BranchResponse> {
  const { data } = await apiClient.get<BranchResponse>(`/branches/${id}`)
  return data
}

export async function createBranch(req: CreateBranchRequest): Promise<BranchResponse> {
  const { data } = await apiClient.post<BranchResponse>('/branches', req)
  return data
}

export async function updateBranch(id: string, req: UpdateBranchRequest): Promise<BranchResponse> {
  const { data } = await apiClient.put<BranchResponse>(`/branches/${id}`, req)
  return data
}

export async function deleteBranch(id: string): Promise<void> {
  await apiClient.delete(`/branches/${id}`)
}

export async function updateOperatingHours(
  id: string,
  hours: OperatingHoursEntry[]
): Promise<BranchResponse> {
  const { data } = await apiClient.put<BranchResponse>(`/branches/${id}/operating-hours`, hours)
  return data
}
