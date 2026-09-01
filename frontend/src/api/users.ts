import apiClient from './apiClient'
import type { UserProfileResponse, UpdateProfileRequest } from '../types'

export async function getMe(): Promise<UserProfileResponse> {
  const { data } = await apiClient.get<UserProfileResponse>('/users/me')
  return data
}

export async function updateMe(req: UpdateProfileRequest): Promise<UserProfileResponse> {
  const { data } = await apiClient.put<UserProfileResponse>('/users/me', req)
  return data
}
