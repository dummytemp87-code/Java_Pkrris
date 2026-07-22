'use client'

import { Card } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { CheckCircle2, Clock, Zap, TrendingUp, X, Target, Sparkles, Flame, Trophy, Star } from "lucide-react"
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
  userName,
}: {
  onNavigate: (page: string) => void;
  goals: Goal[];
  onDeleteGoal: (id: number) => void;
  onSelectGoal?: (goal: Goal) => void;
  refreshKey?: number;
  userName?: string;
}) {
  const [loadingSummary, setLoadingSummary] = useState(false)
  const [tasksCompletedToday, setTasksCompletedToday] = useState(0)
  const [studyMinutesToday, setStudyMinutesToday] = useState(0)
  const [streakDays, setStreakDays] = useState(0)
  const [todaysTasks, setTodaysTasks] = useState<Array<any>>([])
  const [billingStatus, setBillingStatus] = useState<{ plan: string; status: string; trialEndsAt?: string | null } | null>(null)

  useEffect(() => {
    const run = async () => {
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/billing/status`, { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        const data = await res.json().catch(() => ({}))
        if (res.ok) setBillingStatus(data)
      } catch {
        // ignore -- banner just won't render
      }
    }
    run()
  }, [])

  const trialDaysLeft = (() => {
    if (billingStatus?.status !== 'TRIALING' || !billingStatus.trialEndsAt) return null
    const ms = new Date(billingStatus.trialEndsAt).getTime() - Date.now()
    return Math.ceil(ms / (1000 * 60 * 60 * 24))
  })()

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

  const overallProgress = goals.length
    ? Math.round(goals.reduce((acc, goal) => acc + goal.progress, 0) / goals.length)
    : 0

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

  const streakColor = streakDays === 0
    ? "text-muted-foreground"
    : streakDays < 3
      ? "text-orange-400"
      : streakDays < 7
        ? "text-orange-500"
        : "text-red-500"

  const achievements = useMemo(() => [
    { id: "first-goal", label: "First Goal", icon: Target, unlocked: goals.length >= 1 },
    { id: "streak-3", label: "3-Day Streak", icon: Flame, unlocked: streakDays >= 3 },
    { id: "streak-7", label: "7-Day Streak", icon: Flame, unlocked: streakDays >= 7 },
    { id: "goal-complete", label: "Goal Completed", icon: Trophy, unlocked: goals.some(g => g.progress >= 100) },
    { id: "multi-goal", label: "Multi-Tasker", icon: Star, unlocked: goals.length >= 3 },
  ], [goals, streakDays])

  const motivationalLine = useMemo(() => {
    if (streakDays >= 7) return `You're on a ${streakDays}-day streak — unstoppable!`
    if (streakDays >= 3) return `${streakDays}-day streak going — keep it up!`
    const closeGoal = goals.find(g => g.progress >= 50 && g.progress < 100)
    if (closeGoal) return `You're ${closeGoal.progress}% through "${closeGoal.title}" — almost there!`
    if (goals.length === 0) return "Set your first goal to start your learning journey."
    return "Every study session counts. Let's make today count."
  }, [streakDays, goals])

  return (
    <div className="p-6 md:p-8 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8 animate-in fade-in-0 slide-in-from-bottom-2 duration-500">
        <h1 className="text-3xl font-bold text-foreground mb-2">Welcome back{userName ? `, ${userName}` : ""}!</h1>
        <p className="text-muted-foreground">{motivationalLine}</p>
      </div>

      {trialDaysLeft !== null && (
        <Card className="p-4 bg-primary/5 border border-primary/20 mb-8 flex items-center justify-between flex-wrap gap-3 animate-in fade-in-0 slide-in-from-bottom-2 duration-500">
          <div className="flex items-center gap-3">
            <Sparkles className="text-primary shrink-0" size={20} />
            <p className="text-sm font-medium text-foreground">
              {trialDaysLeft > 0
                ? `${trialDaysLeft} day${trialDaysLeft === 1 ? '' : 's'} left in your free trial.`
                : "Your free trial has ended."}
            </p>
          </div>
          <Button onClick={() => onNavigate('billing')} className="bg-primary text-primary-foreground hover:bg-primary/90">
            View plans
          </Button>
        </Card>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        {loadingSummary ? (
          [0, 1, 2, 3].map((i) => (
            <Card key={i} className="p-6 bg-card border border-border">
              <div className="flex items-center justify-between">
                <div className="space-y-2">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-8 w-16" />
                </div>
                <Skeleton className="h-8 w-8 rounded-full" />
              </div>
            </Card>
          ))
        ) : (
          <>
            <Card className="p-6 bg-card border border-border transition-all hover:shadow-md hover:-translate-y-0.5 animate-in fade-in-0 slide-in-from-bottom-2 duration-500">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Tasks Completed</p>
                  <p className="text-3xl font-bold text-primary">{completedToday}</p>
                </div>
                <CheckCircle2 className="text-primary" size={32} />
              </div>
            </Card>

            <Card className="p-6 bg-card border border-border transition-all hover:shadow-md hover:-translate-y-0.5 animate-in fade-in-0 slide-in-from-bottom-2 duration-500 delay-75">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Study Time</p>
                  <p className="text-3xl font-bold text-secondary">{totalTime} min</p>
                </div>
                <Clock className="text-secondary" size={32} />
              </div>
            </Card>

            <Card className="p-6 bg-card border border-border transition-all hover:shadow-md hover:-translate-y-0.5 animate-in fade-in-0 slide-in-from-bottom-2 duration-500 delay-150">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Streak</p>
                  <p className={`text-3xl font-bold ${streakColor}`}>{streakDays} days</p>
                </div>
                <Flame className={`${streakColor} ${streakDays > 0 ? "animate-in zoom-in-50 duration-700" : ""}`} size={32} fill={streakDays >= 3 ? "currentColor" : "none"} />
              </div>
            </Card>

            <Card className="p-6 bg-card border border-border transition-all hover:shadow-md hover:-translate-y-0.5 animate-in fade-in-0 slide-in-from-bottom-2 duration-500 delay-200">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground mb-1">Overall Progress</p>
                  <p className="text-3xl font-bold text-primary">{overallProgress}%</p>
                </div>
                <TrendingUp className="text-primary" size={32} />
              </div>
            </Card>
          </>
        )}
      </div>

      {/* Achievements */}
      <Card className="p-5 bg-card border border-border mb-8 animate-in fade-in-0 slide-in-from-bottom-2 duration-500">
        <div className="flex items-center gap-2 mb-3">
          <Trophy size={16} className="text-muted-foreground" />
          <h2 className="text-sm font-semibold text-foreground">Achievements</h2>
        </div>
        <div className="flex flex-wrap gap-2">
          {achievements.map((a) => {
            const Icon = a.icon
            return (
              <Badge
                key={a.id}
                variant={a.unlocked ? "success" : "outline"}
                className={a.unlocked ? "" : "opacity-50"}
              >
                <Icon size={12} />
                {a.label}
              </Badge>
            )
          })}
        </div>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Daily Tasks */}
        <div className="lg:col-span-2">
          <Card className="p-6 bg-card border border-border animate-in fade-in-0 slide-in-from-bottom-2 duration-500">
            <h2 className="text-xl font-bold text-foreground mb-4">Today's Tasks</h2>
            {loadingSummary ? (
              <div className="space-y-3">
                {[0, 1, 2].map((i) => (
                  <div key={i} className="flex items-center gap-4 p-4 rounded-lg bg-muted/30">
                    <Skeleton className="w-5 h-5 rounded" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-4 w-3/5" />
                      <Skeleton className="h-3 w-1/4" />
                    </div>
                    <Skeleton className="h-4 w-10" />
                  </div>
                ))}
              </div>
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
          <Card className="p-6 bg-card border border-border animate-in fade-in-0 slide-in-from-bottom-2 duration-500">
            <h2 className="text-xl font-bold text-foreground mb-4">Active Goals</h2>
            {goals.length === 0 ? (
              <div className="text-center py-6">
                <Target size={32} className="mx-auto text-muted-foreground mb-3 opacity-50" />
                <p className="text-sm text-muted-foreground mb-4">You haven't set a learning goal yet.</p>
                <Button onClick={() => onNavigate("goals")} className="bg-primary text-primary-foreground hover:bg-primary/90">
                  Create your first goal
                </Button>
              </div>
            ) : (
            <div className="space-y-4">
              {goals.map((goal) => (
                <div key={goal.id} className="space-y-2 p-2 -m-2 rounded-lg transition-colors hover:bg-muted/30">
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
                  <Progress
                    value={goal.progress}
                    className="h-2"
                    indicatorClassName={goal.progress >= 100 ? "bg-emerald-500" : goal.progress >= 80 ? "bg-emerald-400" : undefined}
                  />
                  <p className="text-xs text-muted-foreground">
                    {goal.progress >= 100 ? "🎉 Complete!" : `${goal.progress}% complete`}
                  </p>
                </div>
              ))}
            </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}
