import { http } from '@/utils/http'

/**
 * 登录请求参数
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * 注册请求参数
 */
export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
}

/**
 * 认证响应
 */
export interface AuthResponse {
  token: string
  userId: string
  username: string
  nickname: string
  role: string
}

/**
 * 用户登录
 */
export const login = (data: LoginRequest): Promise<AuthResponse> => {
  return http.post<AuthResponse>('/v1/user/login', data)
}

/**
 * 用户注册
 */
export const register = (data: RegisterRequest): Promise<AuthResponse> => {
  return http.post<AuthResponse>('/v1/user/register', data)
}

