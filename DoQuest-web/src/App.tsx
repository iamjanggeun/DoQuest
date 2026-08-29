import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import {
  ArrowLeft, ArrowRight, CalendarDays, Check, CheckCircle2, Clock3,
  LoaderCircle, LogOut, MapPin, Plus, Sparkles, Trash2, X,
} from 'lucide-react'
import { api, session } from './api'
import type { Memo, MemoAnalysis, Schedule, ScheduleInput } from './types'

const weekdayNames = ['일', '월', '화', '수', '목', '금', '토']
const emptyInput = (date: string): ScheduleInput => ({ title: '', scheduledAt: date, location: '', summaryInfo: '' })

function formatMonth(date: Date) {
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long' }).format(date)
}

function dateKey(year: number, monthIndex: number, day: number) {
  return `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function todayKey() {
  const now = new Date()
  return dateKey(now.getFullYear(), now.getMonth(), now.getDate())
}

function App() {
  const [authenticated, setAuthenticated] = useState(Boolean(session.token))
  useEffect(() => {
    const unauthorized = () => setAuthenticated(false)
    window.addEventListener('doquest:unauthorized', unauthorized)
    return () => window.removeEventListener('doquest:unauthorized', unauthorized)
  }, [])

  if (!authenticated) return <AuthScreen onAuthenticated={() => setAuthenticated(true)} />
  return <Workspace onLogout={() => { session.clear(); setAuthenticated(false) }} />
}

function AuthScreen({ onAuthenticated }: { onAuthenticated: () => void }) {
  const [signup, setSignup] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState({ email: '', password: '', nickname: '', petName: '' })

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true); setError('')
    try {
      if (signup) await api.signup(form.email, form.password, form.nickname, form.petName)
      await api.login(form.email, form.password)
      onAuthenticated()
    } catch (e) { setError(e instanceof Error ? e.message : '로그인에 실패했습니다.') }
    finally { setLoading(false) }
  }

  return <main className="auth-shell">
    <section className="auth-story">
      <div className="brand brand-large"><span className="brand-mark">D</span><span>DoQuest</span></div>
      <div className="story-copy">
        <p className="eyebrow">CAPTURE · DISCOVER · DO</p>
        <h1>생각을 적으면,<br/><em>할 일</em>이 선명해져요.</h1>
        <p>흩어진 메모 속 약속과 일정을 AI가 찾아내고, 확인한 순간 캘린더에 정리합니다.</p>
      </div>
      <div className="story-note"><Sparkles size={18}/><span>메모는 가볍게, 실행은 분명하게.</span></div>
    </section>
    <section className="auth-panel">
      <form className="auth-card" onSubmit={submit}>
        <p className="eyebrow">WELCOME TO DOQUEST</p>
        <h2>{signup ? '새 여정을 시작해요' : '다시 만나 반가워요'}</h2>
        <p className="muted">{signup ? '계정을 만들고 첫 메모를 남겨보세요.' : '오늘의 계획을 이어서 정리해볼까요?'}</p>
        <label>이메일<input type="email" required value={form.email} onChange={e => setForm({...form, email: e.target.value})} placeholder="you@example.com"/></label>
        <label>비밀번호<input type="password" required minLength={6} value={form.password} onChange={e => setForm({...form, password: e.target.value})} placeholder="6자 이상 입력"/></label>
        {signup && <div className="form-row">
          <label>닉네임<input required value={form.nickname} onChange={e => setForm({...form, nickname: e.target.value})} placeholder="도퀘스터"/></label>
          <label>펫 이름<input required value={form.petName} onChange={e => setForm({...form, petName: e.target.value})} placeholder="모모"/></label>
        </div>}
        {error && <p className="error-message">{error}</p>}
        <button className="primary wide" disabled={loading}>{loading && <LoaderCircle className="spin" size={17}/>} {signup ? '가입하고 시작하기' : '로그인'}</button>
        <button className="text-button" type="button" onClick={() => { setSignup(!signup); setError('') }}>{signup ? '이미 계정이 있어요' : '처음이신가요? 계정 만들기'}</button>
      </form>
    </section>
  </main>
}

function Workspace({ onLogout }: { onLogout: () => void }) {
  const now = new Date()
  const [month, setMonth] = useState(new Date(now.getFullYear(), now.getMonth(), 1))
  const [schedules, setSchedules] = useState<Schedule[]>([])
  const [memos, setMemos] = useState<Memo[]>([])
  const [selectedDate, setSelectedDate] = useState(todayKey())
  const [editing, setEditing] = useState<Schedule | null | 'new'>(null)
  const [memoOpen, setMemoOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [scheduleData, memoData] = await Promise.all([
        api.schedules(month.getFullYear(), month.getMonth() + 1), api.memos(),
      ])
      setSchedules(scheduleData); setMemos(memoData)
    } catch (e) { setToast(e instanceof Error ? e.message : '데이터를 불러오지 못했습니다.') }
    finally { setLoading(false) }
  }, [month])

  useEffect(() => { void load() }, [load])
  useEffect(() => { if (!toast) return; const id = setTimeout(() => setToast(''), 3200); return () => clearTimeout(id) }, [toast])

  const selectedSchedules = schedules.filter(item => item.scheduledAt === selectedDate)
  const doneCount = schedules.filter(item => item.isCompleted).length

  async function toggle(item: Schedule) {
    try {
      const updated = await api.completeSchedule(item.scheduleId, !item.isCompleted)
      setSchedules(current => current.map(value => value.scheduleId === item.scheduleId ? updated : value))
    } catch (e) { setToast(e instanceof Error ? e.message : '상태를 변경하지 못했습니다.') }
  }

  async function remove(item: Schedule) {
    if (!window.confirm(`‘${item.title}’ 일정을 삭제할까요?`)) return
    try {
      await api.deleteSchedule(item.scheduleId)
      setSchedules(current => current.filter(value => value.scheduleId !== item.scheduleId))
      setEditing(null); setToast('일정을 삭제했습니다.')
    } catch (e) { setToast(e instanceof Error ? e.message : '삭제하지 못했습니다.') }
  }

  return <div className="app-shell">
    <header className="topbar">
      <div className="brand"><span className="brand-mark">D</span><span>DoQuest</span></div>
      <nav><button className="nav-active"><CalendarDays size={17}/>캘린더</button><button onClick={() => setMemoOpen(true)}><Sparkles size={17}/>AI 메모</button></nav>
      <button className="icon-text" onClick={onLogout}><LogOut size={16}/>로그아웃</button>
    </header>

    <main className="workspace">
      <section className="calendar-panel">
        <div className="calendar-heading">
          <div><p className="eyebrow">MY QUEST CALENDAR</p><h1>{formatMonth(month)}</h1></div>
          <div className="calendar-actions">
            <button className="outline" onClick={() => { setMonth(new Date(now.getFullYear(), now.getMonth(), 1)); setSelectedDate(todayKey()) }}>오늘</button>
            <div className="month-nav"><button aria-label="이전 달" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}><ArrowLeft/></button><button aria-label="다음 달" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}><ArrowRight/></button></div>
            <button className="primary" onClick={() => setEditing('new')}><Plus size={17}/>일정 추가</button>
          </div>
        </div>
        <div className="progress-line"><span>{schedules.length ? `이번 달 ${schedules.length}개 중 ${doneCount}개 완료` : '이번 달의 첫 일정을 만들어보세요'}</span><div><i style={{width: `${schedules.length ? doneCount / schedules.length * 100 : 0}%`}}/></div></div>
        <CalendarGrid month={month} schedules={schedules} selectedDate={selectedDate} onSelect={setSelectedDate} onOpen={setEditing}/>
      </section>

      <aside className="day-panel">
        <p className="eyebrow">SELECTED DAY</p>
        <h2>{new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date(`${selectedDate}T00:00:00`))}</h2>
        <div className="day-list">
          {loading ? <Empty icon={<LoaderCircle className="spin"/>} title="일정을 불러오는 중"/> : selectedSchedules.length === 0 ? <Empty icon={<CalendarDays/>} title="아직 일정이 없어요" body="이 날에 새로운 퀘스트를 추가해보세요."/> : selectedSchedules.map(item =>
            <article className={`schedule-card ${item.isCompleted ? 'completed' : ''}`} key={item.scheduleId} onClick={() => setEditing(item)}>
              <button className="check-button" aria-label="완료 상태 변경" onClick={e => { e.stopPropagation(); void toggle(item) }}>{item.isCompleted && <Check size={15}/>}</button>
              <div><h3>{item.title}</h3>{item.location && <p><MapPin size={14}/>{item.location}</p>}{item.summaryInfo && <span>{item.summaryInfo}</span>}</div>
            </article>)}
        </div>
        <button className="memo-cta" onClick={() => setMemoOpen(true)}><span><Sparkles/></span><div><b>떠오른 일이 있나요?</b><small>AI 메모로 일정을 찾아보세요</small></div><ArrowRight/></button>
      </aside>
    </main>

    {editing && <ScheduleModal value={editing === 'new' ? null : editing} date={selectedDate} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); void load(); setToast('캘린더에 반영했습니다.') }} onDelete={remove}/>} 
    {memoOpen && <MemoDrawer memos={memos} onClose={() => setMemoOpen(false)} onChanged={() => void load()} onToast={setToast}/>} 
    {toast && <div className="toast"><CheckCircle2 size={18}/>{toast}</div>}
  </div>
}

function CalendarGrid({ month, schedules, selectedDate, onSelect, onOpen }: { month: Date; schedules: Schedule[]; selectedDate: string; onSelect: (date: string) => void; onOpen: (schedule: Schedule) => void }) {
  const cells = useMemo(() => {
    const year = month.getFullYear(), monthIndex = month.getMonth()
    const leading = new Date(year, monthIndex, 1).getDay()
    const total = new Date(year, monthIndex + 1, 0).getDate()
    return Array.from({length: 42}, (_, index) => {
      const day = index - leading + 1
      return day > 0 && day <= total ? { day, key: dateKey(year, monthIndex, day) } : null
    })
  }, [month])
  return <div className="calendar-grid">
    {weekdayNames.map(day => <div className="weekday" key={day}>{day}</div>)}
    {cells.map((cell, index) => cell ? <button className={`day-cell ${cell.key === selectedDate ? 'selected' : ''} ${cell.key === todayKey() ? 'today' : ''}`} key={cell.key} onClick={() => onSelect(cell.key)}>
      <span className="date-number">{cell.day}</span>
      <div className="cell-events">{schedules.filter(item => item.scheduledAt === cell.key).slice(0, 3).map(item => <span key={item.scheduleId} className={item.isCompleted ? 'done' : ''} onClick={e => {e.stopPropagation(); onOpen(item)}}><i/>{item.title}</span>)}</div>
    </button> : <div className="day-cell empty-cell" key={index}/>) }
  </div>
}

function ScheduleModal({ value, date, onClose, onSaved, onDelete }: { value: Schedule | null; date: string; onClose: () => void; onSaved: () => void; onDelete: (value: Schedule) => void }) {
  const [form, setForm] = useState<ScheduleInput>(value ? { title: value.title, scheduledAt: value.scheduledAt, location: value.location ?? '', summaryInfo: value.summaryInfo ?? '' } : emptyInput(date))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setError('')
    try {
      if (value) await api.updateSchedule(value.scheduleId, form)
      else await api.createSchedule(form)
      onSaved()
    }
    catch (e) { setError(e instanceof Error ? e.message : '저장하지 못했습니다.') }
    finally { setSaving(false) }
  }
  return <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
    <form className="modal" onSubmit={submit}>
      <div className="modal-head"><div><p className="eyebrow">SCHEDULE</p><h2>{value ? '일정 다듬기' : '새 일정 만들기'}</h2></div><button type="button" className="icon-button" onClick={onClose}><X/></button></div>
      <label>일정 이름<input autoFocus required value={form.title} onChange={e => setForm({...form, title: e.target.value})} placeholder="무엇을 할 예정인가요?"/></label>
      <div className="form-row"><label>날짜<input type="date" required value={form.scheduledAt} onChange={e => setForm({...form, scheduledAt: e.target.value})}/></label><label>장소<input value={form.location ?? ''} onChange={e => setForm({...form, location: e.target.value})} placeholder="선택 입력"/></label></div>
      <label>메모<textarea rows={4} value={form.summaryInfo ?? ''} onChange={e => setForm({...form, summaryInfo: e.target.value})} placeholder="준비물이나 참고할 내용을 적어주세요."/></label>
      {error && <p className="error-message">{error}</p>}
      <div className="modal-actions">{value && <button className="danger" type="button" onClick={() => onDelete(value)}><Trash2 size={16}/>삭제</button>}<span/><button className="outline" type="button" onClick={onClose}>취소</button><button className="primary" disabled={saving}>{saving && <LoaderCircle className="spin" size={16}/>}저장</button></div>
    </form>
  </div>
}

function MemoDrawer({ memos, onClose, onChanged, onToast }: { memos: Memo[]; onClose: () => void; onChanged: () => void; onToast: (message: string) => void }) {
  const [content, setContent] = useState('')
  const [saving, setSaving] = useState(false)
  const [selected, setSelected] = useState<number | null>(memos[0]?.id ?? null)
  const [analysis, setAnalysis] = useState<MemoAnalysis | null>(null)
  const [checking, setChecking] = useState(false)

  const check = useCallback(async (id: number) => {
    setChecking(true)
    try { setAnalysis(await api.analysis(id)) } catch { setAnalysis(null) }
    finally { setChecking(false) }
  }, [])
  useEffect(() => { if (selected) void check(selected) }, [selected, check])

  async function create(event: FormEvent) {
    event.preventDefault(); if (!content.trim()) return
    setSaving(true)
    try { const id = await api.createMemo(content); setContent(''); setSelected(id); onChanged(); onToast('메모를 저장했어요. AI가 분석 중입니다.') }
    catch (e) { onToast(e instanceof Error ? e.message : '메모를 저장하지 못했습니다.') }
    finally { setSaving(false) }
  }
  async function confirm() {
    if (!analysis) return
    setChecking(true)
    try { await api.confirmAnalysis(analysis.memoId); await check(analysis.memoId); onChanged(); onToast('AI 제안을 캘린더에 추가했습니다.') }
    catch (e) { onToast(e instanceof Error ? e.message : '일정을 확정하지 못했습니다.'); setChecking(false) }
  }

  return <div className="drawer-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
    <aside className="drawer">
      <div className="modal-head"><div><p className="eyebrow">AI NOTE</p><h2>생각을 일정으로</h2></div><button className="icon-button" onClick={onClose}><X/></button></div>
      <form className="quick-note" onSubmit={create}><textarea rows={4} value={content} onChange={e => setContent(e.target.value)} placeholder="예: 다음 주 금요일 오후 2시 선릉에서 프로젝트 회의"/><button className="primary" disabled={saving || !content.trim()}>{saving ? <LoaderCircle className="spin"/> : <Sparkles/>}AI로 분석하기</button></form>
      <div className="memo-layout">
        <div className="memo-list"><p className="section-label">최근 메모</p>{memos.length === 0 ? <p className="muted compact">아직 메모가 없어요.</p> : memos.map(memo => <button className={selected === memo.id ? 'active' : ''} key={memo.id} onClick={() => setSelected(memo.id)}><span>{memo.content}</span><small>{memo.isParsed ? '분석 완료' : '분석 중'}</small></button>)}</div>
        <div className="analysis-card">{checking ? <Empty icon={<LoaderCircle className="spin"/>} title="AI 결과 확인 중"/> : !analysis ? <Empty icon={<Sparkles/>} title="메모를 선택하세요" body="AI가 찾아낸 일정을 여기서 확인할 수 있어요."/> : <AnalysisView value={analysis} onRefresh={() => void check(analysis.memoId)} onConfirm={() => void confirm()}/>}</div>
      </div>
    </aside>
  </div>
}

function AnalysisView({ value, onRefresh, onConfirm }: { value: MemoAnalysis; onRefresh: () => void; onConfirm: () => void }) {
  if (value.status === 'PENDING') return <Empty icon={<Clock3/>} title="AI가 읽고 있어요" body="잠시 뒤 결과를 다시 확인해주세요." action={<button className="outline" onClick={onRefresh}>새로고침</button>}/>
  if (value.status === 'FAILED') return <Empty icon={<X/>} title="분석하지 못했어요" body="원본 메모는 안전하게 보관되어 있습니다."/>
  if (!value.isSchedule) return <Empty icon={<Sparkles/>} title="일정이 아닌 메모예요" body="메모로 보관하고 필요할 때 다시 확인하세요."/>
  return <div className="analysis-result"><span className={`status-badge ${value.status.toLowerCase()}`}>{value.status === 'CONFIRMED' ? '캘린더 등록 완료' : 'AI 일정 제안'}</span><h3>{value.title}</h3><p><CalendarDays/>{value.scheduledAt}</p>{value.location && <p><MapPin/>{value.location}</p>}{value.summaryInfo && <blockquote>{value.summaryInfo}</blockquote>}<button className="primary wide" disabled={value.status === 'CONFIRMED'} onClick={onConfirm}>{value.status === 'CONFIRMED' ? <><Check/>등록된 일정</> : '이 일정으로 확정하기'}</button></div>
}

function Empty({ icon, title, body, action }: { icon: React.ReactNode; title: string; body?: string; action?: React.ReactNode }) {
  return <div className="empty-state"><span>{icon}</span><b>{title}</b>{body && <p>{body}</p>}{action}</div>
}

export default App
