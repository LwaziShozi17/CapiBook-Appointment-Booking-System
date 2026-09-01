import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { login as apiLogin, logout as apiLogout, register as apiRegister } from '../api/auth'
import { getMe } from '../api/users'
import type { UserProfileResponse, LoginRequest, RegisterRequest } from '../types'

interface AuthContextValue {
  user: UserProfileResponse | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (req: LoginRequest) => Promise<UserProfileResponse>
  logout: () => Promise<void>
  register: (req: RegisterRequest) => Promise<void>
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfileResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setIsLoading(false)
      return
    }
    getMe()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
      })
      .finally(() => setIsLoading(false))
  }, [])

  async function login(req: LoginRequest): Promise<UserProfileResponse> {
    const res = await apiLogin(req)
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    setUser(res.user)
    return res.user
  }

  async function register(req: RegisterRequest) {
    const res = await apiRegister(req)
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    setUser(res.user)
  }

  async function logout() {
    const refreshToken = localStorage.getItem('refreshToken')
    try {
      if (refreshToken) await apiLogout(refreshToken)
    } catch {}
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
  }

  async function refreshUser() {
    const updated = await getMe()
    setUser(updated)
  }

  return (
    <AuthContext.Provider
      value={{ user, isAuthenticated: !!user, isLoading, login, logout, register, refreshUser }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
