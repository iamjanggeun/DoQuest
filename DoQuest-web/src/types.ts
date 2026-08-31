export type Schedule = {
  scheduleId: number
  memoId: number | null
  title: string
  scheduledAt: string
  scheduledTime: string | null
  location: string | null
  summaryInfo: string | null
  isCompleted: boolean
}

export type Memo = {
  id: number
  content: string
  isParsed: boolean
  createdAt: string
}

export type AnalysisStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CONFIRMED'

export type MemoAnalysis = {
  memoId: number
  status: AnalysisStatus
  isSchedule: boolean
  title: string | null
  scheduledAt: string | null
  scheduledTime: string | null
  location: string | null
  summaryInfo: string | null
}

export type ScheduleInput = Pick<Schedule, 'title' | 'scheduledAt' | 'scheduledTime' | 'location' | 'summaryInfo'>
