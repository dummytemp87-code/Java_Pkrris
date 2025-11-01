'use client'

import { Card } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { CheckCircle2, Clock, Zap, TrendingUp, X } from "lucide-react"
import { useState, useEffect, useMemo } from "react"

interface Goal {
  id: number;
  title: string;
  progress: number;
  daysLeft: number;
}

export default function Dashboard({ 
  onNavigate, 
  goals, 
  onDeleteGoal,
  onSelectGoal,
  refreshKey,
}: { 
  onNavigate: (page: string) => void; 
  goals: Goal[];
  onDeleteGoal: (id: number) => void;
  onSelectGoal?: (goal: Goal) => void;
  refreshKey?: number;
}) {
  const [loadingSummary, setLoadingSummary] = useState(false)
  const [tasksCompletedToday, setTasksCompletedToday] = useState(0)
  const [studyMinutesToday, setStudyMinutesToday] = useState(0)
  const [streakDays, setStreakDays] = useState(0)
  const [todaysTasks, setTodaysTasks] = useState<Array<any>>([])

  useEffect(() => {
    let cancelled = false
    const run = async () => {
      setLoadingSummary(true)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/dashboard/summary`, {
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          }
        })
        const data = await res.json().catch(() => ({}))
        if (!cancelled && res.ok) {
          setTasksCompletedToday(Number(data?.tasksCompletedToday || 0))
          setStudyMinutesToday(Number(data?.studyMinutesToday || 0))
          setStreakDays(Number(data?.streakDays || 0))
          setTodaysTasks(Array.isArray(data?.todaysTasks) ? data.todaysTasks : [])
        }
      } catch {
        if (!cancelled) {
          setTasksCompletedToday(0)
          setStudyMinutesToday(0)
          setStreakDays(0)
          setTodaysTasks([])
        }
      } finally {
        if (!cancelled) setLoadingSummary(false)
      }
    }
    run()
    return () => { cancelled = true }
  }, [refreshKey])

  const completedToday = tasksCompletedToday
  const totalTime = studyMinutesToday

  const overallProgress = Math.round(
    goals.reduce((acc, goal) => acc + goal.progress, 0) / goals.length
  )

  const openTaskGoal = (goalTitle: string) => {
    const g = goals.find(x => x.title === goalTitle)
    if (g) {
      onSelectGoal ? onSelectGoal(g) : onNavigate('study-plan')
    }
  }

  const visibleTasks = useMemo(() => {
    return Array.isArray(todaysTasks)
      ? todaysTasks.filter(t => goals.some(g => g.title === t.goalTitle))
      : []
  }, [todaysTasks, goals])

  const visibleTasksSorted = useMemo(() => {
    return [...visibleTasks].sort((a, b) => (a.completed === b.completed ? 0 : a.completed ? 1 : -1))
  }, [visibleTasks])

  return (
    <div className="p-6 md:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Welcome back, Learner!</h1>
        <p className="text-muted-foreground">Keep up the momentum with your studies</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground mb-1">Tasks Completed</p>
              <p className="text-3xl font-bold text-primary">{completedToday}</p>
            </div>
            <CheckCircle2 className="text-primary" size={32} />
          </div>
        </Card>

        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground mb-1">Study Time</p>
              <p className="text-3xl font-bold text-secondary">{totalTime} min</p>
            </div>
            <Clock className="text-secondary" size={32} />
          </div>
        </Card>

        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground mb-1">Streak</p>
              <p className="text-3xl font-bold text-accent">{streakDays} days</p>
            </div>
            <Zap className="text-accent" size={32} />
          </div>
        </Card>

        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-muted-foreground mb-1">Overall Progress</p>
              <p className="text-3xl font-bold text-primary">{overallProgress}%</p>
            </div>
            <TrendingUp className="text-primary" size={32} />
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Daily Tasks */}
        <div className="lg:col-span-2">
          <Card className="p-6 bg-card border border-border">
            <h2 className="text-xl font-bold text-foreground mb-4">Today's Tasks</h2>
            {loadingSummary ? (
              <p className="text-sm text-muted-foreground">Loading today's tasks...</p>
            ) : visibleTasks.length === 0 ? (
              <p className="text-sm text-muted-foreground">No tasks scheduled for today.</p>
            ) : (
              <div className="space-y-3">
                {visibleTasksSorted.map((t, idx) => (
                  <div
                    key={`${t.goalTitle}-${t.moduleId}-${idx}`}
                    className="flex items-center gap-4 p-4 rounded-lg bg-muted/30 hover:bg-muted/50 transition-colors cursor-pointer"
                    onClick={() => openTaskGoal(t.goalTitle)}
                  >
                    <input
                      type="checkbox"
                      checked={!!t.completed}
                      readOnly
                      onClick={(e) => e.stopPropagation()}
                      className="w-5 h-5 rounded border-2 border-primary"
                    />
                    <div className="flex-1">
                      <p className={`font-medium ${t.completed ? 'line-through text-muted-foreground' : 'text-foreground'}`}>
                        {t.moduleTitle} <span className="text-xs text-muted-foreground">({t.goalTitle})</span>
                      </p>
                      <p className="text-xs text-muted-foreground capitalize">{t.type}</p>
                    </div>
                    <span className="text-sm text-muted-foreground">{t.duration}</span>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>

        {/* Active Goals */}
        <div>
          <Card className="p-6 bg-card border border-border">
            <h2 className="text-xl font-bold text-foreground mb-4">Active Goals</h2>
            <div className="space-y-4">
              {goals.map((goal) => (
                <div key={goal.id} className="space-y-2">
                  <div className="flex justify-between items-start">
                    <h3 className="font-medium text-foreground text-sm cursor-pointer" onClick={() => onSelectGoal ? onSelectGoal(goal) : onNavigate("study-plan")}>
                      {goal.title}
                    </h3>
                    <div className="flex items-center gap-2">
                        <span className="text-xs text-muted-foreground">{goal.daysLeft}d left</span>
                        <button onClick={() => onDeleteGoal(goal.id)} className="text-muted-foreground hover:text-foreground">
                            <X size={16} />
                        </button>
                    </div>
                  </div>
                  <Progress value={goal.progress} className="h-2" />
                  <p className="text-xs text-muted-foreground">{goal.progress}% complete</p>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
