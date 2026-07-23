'use client'

import { useState, useEffect } from 'react'
import Navigation from '@/components/navigation'
import ThemeToggle from '@/components/theme-toggle'
import Dashboard from '@/components/pages/dashboard'
import GoalCreation from '@/components/pages/goal-creation'
import StudyPlan from '@/components/pages/study-plan'
import LearningScreen from '@/components/pages/learning-screen'
import QuizPage from '@/components/pages/quiz-page'
import AIChat from '@/components/pages/ai-chat'
import Analytics from '@/components/pages/analytics'
import Billing from '@/components/pages/billing'
import Settings from '@/components/pages/settings'
import Auth from '@/components/pages/auth'
import Landing from '@/components/pages/landing'
import { apiFetch } from '@/lib/api'

type Goal = { id: number; title: string; progress: number; daysLeft: number }

export default function Home() {
  const [currentPage, setCurrentPage] = useState('dashboard')
  const [view, setView] = useState<'landing' | 'auth' | 'app'>('landing')
  const [authChecked, setAuthChecked] = useState(false)
  const [goals, setGoals] = useState<Goal[]>([])
  const [dashboardRefreshKey, setDashboardRefreshKey] = useState(0)

  const [learningScreenState, setLearningScreenState] = useState({
    isCompleted: false,
    notes: "",
    messages: [
      { id: 1, role: "tutor", text: "Hi! I'm your AI tutor. What would you like to know about this topic?" },
    ],
    inputMessage: "",
    chatLoading: false,
    selectedGoalTitle: null as string | null,
    selectedModule: null as any,
});

  const [auth, setAuth] = useState<{ token: string | null; name?: string; email?: string; role?: string }>({ token: null })
  const [selectedGoal, setSelectedGoal] = useState<Goal | null>(null)
  const [focusModuleId, setFocusModuleId] = useState<number | undefined>(undefined)

  useEffect(() => {
    const t = typeof window !== 'undefined' ? localStorage.getItem('token') : null
    if (!t) { setAuthChecked(true); return }
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
    fetch(`${base}/api/auth/me`, { headers: { Authorization: `Bearer ${t}` } })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) {
          setAuth({ token: t, name: data.name, email: data.email, role: data.role })
          setView('app')
        } else {
          // Token is invalid/expired -- clear it so it doesn't linger forever
          // silently failing on every future load.
          localStorage.removeItem('token')
          setAuth({ token: null })
        }
      })
      .catch(() => setAuth({ token: null }))
      .finally(() => setAuthChecked(true))
  }, [])

  useEffect(() => {
    const t = auth.token
    if (!t) return
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
    apiFetch(`${base}/api/goals`)
      .then(res => res.ok ? res.json() : [])
      .then((list) => setGoals(Array.isArray(list) ? list : []))
      .catch(() => setGoals([]))
  }, [auth.token])

  const refreshGoals = async () => {
    const t = typeof window !== 'undefined' ? localStorage.getItem('token') : null
    if (!t) return
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
    try {
      const res = await fetch(`${base}/api/goals`, { headers: { Authorization: `Bearer ${t}` } })
      const list = await res.json().catch(() => [])
      setGoals(Array.isArray(list) ? list : [])
      setDashboardRefreshKey((k) => k + 1)
    } catch {
      // ignore
    }
  }

  const handleDeleteGoal = async (id: number) => {
    const t = typeof window !== 'undefined' ? localStorage.getItem('token') : null
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
    try {
      if (!t) {
        console.warn('Not authenticated; cannot delete goal')
        return
      }
      const res = await fetch(`${base}/api/goals/${id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${t}` } })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) {
        // Fallback path for environments blocking DELETE (403/405): try POST /delete
        if (res.status === 403 || res.status === 405) {
          const res2 = await fetch(`${base}/api/goals/delete/${id}`, { method: 'POST', headers: { Authorization: `Bearer ${t}`, 'Content-Type': 'application/json' } })
          const data2 = await res2.json().catch(() => ({}))
          if (!res2.ok) {
            throw new Error(data2?.error || 'Failed to delete goal')
          }
        } else {
          throw new Error(data?.error || 'Failed to delete goal')
        }
      }
      setGoals(prev => prev.filter(goal => goal.id !== id))
      if (selectedGoal && selectedGoal.id === id) setSelectedGoal(null)
    } catch (e) {
      console.error(e)
    }
  };

  const handleStartLearning = (goalTitle: string, module: any) => {
    setLearningScreenState(prev => {
      const isSameModule = prev.selectedGoalTitle === goalTitle && prev.selectedModule?.id === module?.id
      if (isSameModule) {
        return { ...prev, selectedGoalTitle: goalTitle, selectedModule: module }
      }
      // Different module: start a fresh chat/notes thread instead of carrying over
      // the previous module's conversation and pinning the AI to the wrong topic.
      return {
        ...prev,
        selectedGoalTitle: goalTitle,
        selectedModule: module,
        messages: [
          { id: 1, role: "tutor", text: `Hi! I'm your AI tutor. What would you like to know about ${module?.title || "this topic"}?` },
        ],
        notes: "",
        isCompleted: false,
        inputMessage: "",
        chatLoading: false,
      }
    })
    setCurrentPage(module?.type === 'quiz' ? 'quiz' : 'learning')
  }

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return (
          <Dashboard
            onNavigate={setCurrentPage}
            goals={goals}
            onDeleteGoal={handleDeleteGoal}
            onSelectGoal={(goal: Goal, moduleId?: number) => { setSelectedGoal(goal); setFocusModuleId(moduleId); setCurrentPage('study-plan'); }}
            refreshKey={dashboardRefreshKey}
            userName={auth.name}
          />
        )
      case 'goals':
        return (
          <GoalCreation
            setGoals={setGoals}
            onNavigate={setCurrentPage}
            onGoalCreated={(goal: Goal) => { setSelectedGoal(goal); setFocusModuleId(undefined); setCurrentPage('study-plan'); }}
          />
        )
      case 'study-plan':
        return <StudyPlan onNavigate={setCurrentPage} goal={selectedGoal || undefined} focusModuleId={focusModuleId} onSelectGoal={(g: Goal) => { setSelectedGoal(g); setFocusModuleId(undefined); setCurrentPage('study-plan'); }} onStartLearning={handleStartLearning} />
      case 'learning':
        return learningScreenState.selectedGoalTitle && learningScreenState.selectedModule ? (
          <LearningScreen
            onNavigate={setCurrentPage}
            learningState={learningScreenState}
            setLearningState={setLearningScreenState}
            onProgressUpdated={refreshGoals}
          />
        ) : (
          <StudyPlan onNavigate={setCurrentPage} goal={selectedGoal || undefined} focusModuleId={focusModuleId} onSelectGoal={(g: Goal) => { setSelectedGoal(g); setFocusModuleId(undefined); setCurrentPage('study-plan'); }} onStartLearning={handleStartLearning} />
        )
      case 'quiz':
        return learningScreenState.selectedGoalTitle && learningScreenState.selectedModule ? (
          <QuizPage
            onNavigate={setCurrentPage}
            goalTitle={learningScreenState.selectedGoalTitle}
            module={learningScreenState.selectedModule}
            onProgressUpdated={refreshGoals}
          />
        ) : (
          <StudyPlan onNavigate={setCurrentPage} goal={selectedGoal || undefined} focusModuleId={focusModuleId} onSelectGoal={(g: Goal) => { setSelectedGoal(g); setFocusModuleId(undefined); setCurrentPage('study-plan'); }} onStartLearning={handleStartLearning} />
        )
      case 'chat':
        return <AIChat goals={goals} onNavigate={setCurrentPage} />
      case 'analytics':
        return <Analytics />
      case 'billing':
        return <Billing />
      case 'settings':
        return <Settings />
      default:
        return <Dashboard onNavigate={setCurrentPage} goals={goals} onDeleteGoal={handleDeleteGoal} refreshKey={dashboardRefreshKey} userName={auth.name} />
    }
  }

  if (!authChecked) {
    // Avoid flashing the marketing Landing page for returning users while the
    // token-restore check is still in flight.
    return <div className='h-screen bg-background' />
  }

  if (auth.token) {
    return (
      <div className='flex h-screen'>
        <ThemeToggle />
        <Navigation
          currentPage={currentPage}
          onNavigate={(page) => { if (page === 'study-plan') { setSelectedGoal(null); setFocusModuleId(undefined); } setCurrentPage(page); }}
          showLearn={!!learningScreenState.selectedModule}
        />
        <main className='flex-1 overflow-auto'>{renderPage()}</main>
      </div>
    )
  }

  if (view === 'auth') {
    return <Auth onAuthenticated={(u) => { setAuth(u); setView('app') }} onBack={() => setView('landing')} />
  }

  return <Landing onGetStarted={() => setView('auth')} onLogin={() => setView('auth')} />
}
