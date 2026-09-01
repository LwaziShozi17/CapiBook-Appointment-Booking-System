import apiClient from './apiClient'
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types'

export async function login(req: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', req)
  return data
}

export async function register(req: RegisterRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', req)
  return data
}

export async function logout(refreshToken: string): Promise<void> {
  await apiClient.post('/auth/logout', { refreshToken })
}

export async function refresh(refreshToken: string): Promise<{ accessToken: string }> {
  const { data } = await apiClient.post<{ accessToken: string }>('/auth/refresh', { refreshToken })
  return data
}
