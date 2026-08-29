import type { Memo, MemoAnalysis, Schedule, ScheduleInput } from './types'

const TOKEN_KEY = 'doquest_access_token'

export const session = {
  get token() { return localStorage.getItem(TOKEN_KEY) },
  save(token: string) { localStorage.setItem(TOKEN_KEY, token) },
  clear() { localStorage.removeItem(TOKEN_KEY) },
}

type ApiErrorBody = { message?: string; code?: string }

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  if (session.token) headers.set('Authorization', `Bearer ${session.token}`)

  const response = await fetch(path, { ...options, headers })
  if (response.status === 401 || response.status === 403) {
    session.clear()
    window.dispatchEvent(new Event('doquest:unauthorized'))
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as ApiErrorBody
    throw new Error(error.message ?? `요청에 실패했습니다. (${response.status})`)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export const api = {
  async login(email: string, password: string) {
    const result = await request<{ accessToken: string }>('/api/v1/auth/login', {
      method: 'POST', body: JSON.stringify({ email, password }),
    })
    session.save(result.accessToken)
  },
  signup(email: string, password: string, nickname: string, petName: string) {
    return request<number>('/api/v1/auth/signup', {
      method: 'POST', body: JSON.stringify({ email, password, nickname, petName }),
    })
  },
  schedules(year: number, month: number) {
    return request<Schedule[]>(`/api/v1/schedules?year=${year}&month=${month}`)
  },
  createSchedule(input: ScheduleInput) {
    return request<Schedule>('/api/v1/schedules', {
      method: 'POST', body: JSON.stringify({ memoId: null, ...input }),
    })
  },
  updateSchedule(id: number, input: ScheduleInput) {
    return request<Schedule>(`/api/v1/schedules/${id}`, {
      method: 'PATCH', body: JSON.stringify(input),
    })
  },
  completeSchedule(id: number, completed: boolean) {
    return request<Schedule>(`/api/v1/schedules/${id}/completion`, {
      method: 'PATCH', body: JSON.stringify({ completed }),
    })
  },
  deleteSchedule(id: number) {
    return request<void>(`/api/v1/schedules/${id}`, { method: 'DELETE' })
  },
  memos() { return request<Memo[]>('/api/v1/memos') },
  createMemo(content: string) {
    return request<number>('/api/v1/memos', { method: 'POST', body: JSON.stringify({ content }) })
  },
  analysis(memoId: number) {
    return request<MemoAnalysis>(`/api/v1/memos/${memoId}/analysis`)
  },
  confirmAnalysis(memoId: number) {
    return request<Schedule>(`/api/v1/memos/${memoId}/analysis/confirm`, { method: 'POST' })
  },
}
