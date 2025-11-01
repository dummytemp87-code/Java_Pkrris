'use client'

import { useState, useEffect } from 'react'
import Navigation from '@/components/navigation'
import Dashboard from '@/components/pages/dashboard'
import GoalCreation from '@/components/pages/goal-creation'
import StudyPlan from '@/components/pages/study-plan'
import LearningScreen from '@/components/pages/learning-screen'
import QuizPage from '@/components/pages/quiz-page'
import AIChat from '@/components/pages/ai-chat'
import ResourceLibrary from '@/components/pages/resource-library'
import Analytics from '@/components/pages/analytics'
import Settings from '@/components/pages/settings'
import Auth from '@/components/pages/auth'

type Goal = { id: number; title: string; progress: number; daysLeft: number }

export default function Home() {
  const [currentPage, setCurrentPage] = useState('dashboard')
  const [goals, setGoals] = useState<Goal[]>([])
  const [dashboardRefreshKey, setDashboardRefreshKey] = useState(0)

  const [learningScreenState, setLearningScreenState] = useState({
    isCompleted: false,
    notes: "",
    messages: [
      { id: 1, role: "tutor", text: "Hi! I'm your AI tutor. What would you like to learn about derivatives?" },
    ],
    inputMessage: "",
    chatLoading: false,
    selectedGoalTitle: null as string | null,
    selectedModule: null as any,
});

  const [auth, setAuth] = useState<{ token: string | null; name?: string; email?: string; role?: string }>({ token: null })
  const [selectedGoal, setSelectedGoal] = useState<Goal | null>(null)

  useEffect(() => {
    const t = typeof window !== 'undefined' ? localStorage.getItem('token') : null
    if (!t) return
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
    fetch(`${base}/api/auth/me`, { headers: { Authorization: `Bearer ${t}` } })
      .then(res => res.ok ? res.json() : null)
      .then(data => {
        if (data) setAuth({ token: t, name: data.name, email: data.email, role: data.role })
        else setAuth({ token: null })
      })
      .catch(() => setAuth({ token: null }))
  }, [])

  useEffect(() => {
    const t = auth.token
    if (!t) return
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
    fetch(`${base}/api/goals`, { headers: { Authorization: `Bearer ${t}` } })
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

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return (
          <Dashboard
            onNavigate={setCurrentPage}
            goals={goals}
            onDeleteGoal={handleDeleteGoal}
            onSelectGoal={(goal: Goal) => { setSelectedGoal(goal); setCurrentPage('study-plan'); }}
            refreshKey={dashboardRefreshKey}
          />
        )
      case 'goals':
        return <GoalCreation setGoals={setGoals} onNavigate={setCurrentPage} />
      case 'study-plan':
        return <StudyPlan onNavigate={setCurrentPage} goal={selectedGoal || undefined} onSelectGoal={(g: Goal) => { setSelectedGoal(g); setCurrentPage('study-plan'); }} onStartLearning={(goalTitle, module) => { setLearningScreenState(prev => ({ ...prev, selectedGoalTitle: goalTitle, selectedModule: module })); setCurrentPage(module?.type === 'quiz' ? 'quiz' : 'learning'); }} />
      case 'learning':
        return <LearningScreen 
                  onNavigate={setCurrentPage} 
                  learningState={learningScreenState} 
                  setLearningState={setLearningScreenState} 
                  onProgressUpdated={refreshGoals}
                />
      case 'quiz':
        return learningScreenState.selectedGoalTitle && learningScreenState.selectedModule ? (
          <QuizPage 
            onNavigate={setCurrentPage}
            goalTitle={learningScreenState.selectedGoalTitle}
            module={learningScreenState.selectedModule}
            onProgressUpdated={refreshGoals}
          />
        ) : (
          <StudyPlan onNavigate={setCurrentPage} goal={selectedGoal || undefined} onSelectGoal={(g: Goal) => { setSelectedGoal(g); setCurrentPage('study-plan'); }} onStartLearning={(goalTitle, module) => { setLearningScreenState(prev => ({ ...prev, selectedGoalTitle: goalTitle, selectedModule: module })); setCurrentPage(module?.type === 'quiz' ? 'quiz' : 'learning'); }} />
        )
      case 'chat':
        return <AIChat />
      case 'resources':
        return <ResourceLibrary />
      case 'analytics':
        return <Analytics />
      case 'settings':
        return <Settings />
      default:
        return <Dashboard onNavigate={setCurrentPage} goals={goals} onDeleteGoal={handleDeleteGoal} refreshKey={dashboardRefreshKey} />
    }
  }

  return (
    auth.token ? (
      <div className='flex h-screen bg-background'>
        <Navigation currentPage={currentPage} onNavigate={(page) => { if (page === 'study-plan') setSelectedGoal(null); setCurrentPage(page); }} />
        <main className='flex-1 overflow-auto'>{renderPage()}</main>
      </div>
    ) : (
      <Auth onAuthenticated={(u) => setAuth(u)} />
    )
  )
}
