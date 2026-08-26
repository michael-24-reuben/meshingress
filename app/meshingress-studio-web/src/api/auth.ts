import { apiRequest } from './client'
import { generateUuid } from '../utils/uuid'

interface NativeLoginValidationResponse {
  accepted: boolean
  message: string
  identifierFingerprint: string | null
  requestId: string | null
}

export async function validateNativeLogin(username: string, password: string): Promise<NativeLoginValidationResponse> {
  return apiRequest<NativeLoginValidationResponse>('/api/v1/auth/native/validate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Request-Id': generateUuid(),
    },
    body: JSON.stringify({ username, password }),
  })
}
