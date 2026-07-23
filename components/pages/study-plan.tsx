'use client'

import { useEffect, useMemo, useRef, useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { AILoading } from "@/components/ui/ai-loading"
import { CheckCircle2, Circle, ChevronRight, Sparkles } from "lucide-react"
import TaskDetailsModal from "./task-details-modal"
import { apiFetch } from "@/lib/api"

interface Module {
  id: number;
  title: string;
  duration: string;
  completed: boolean;
  type: string;
  description: string;
}

type Goal = { id: number; title: string; progress: number; daysLeft: number }

export default function StudyPlan({ onNavigate, goal, onSelectGoal, onStartLearning }: { onNavigate: (page: string) => void; goal?: Goal; onSelectGoal?: (goal: Goal) => void; onStartLearning?: (goalTitle: string, module: Module) => void }) {
  const [selectedModule, setSelectedModule] = useState<Module | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [subscriptionRequired, setSubscriptionRequired] = useState(false)

  const [studyPlan, setStudyPlan] = useState<any[]>([])
  const [goalsList, setGoalsList] = useState<Goal[]>([])
  const [loadingGoals, setLoadingGoals] = useState(false)

  useEffect(() => {
    if (!goal?.title) return
    let cancelled = false
    const fetchPlan = async () => {
      setLoading(true)
      setError(null)
      setSubscriptionRequired(false)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const res = await apiFetch(`${base}/api/study-plan/generate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            goalTitle: goal?.title || 'General Study',
            days: goal?.daysLeft ? Math.max(1, goal.daysLeft) : 7,
            level: 'beginner',
          })
        })
        if (cancelled) return
        if (res.status === 402) {
          setSubscriptionRequired(true)
          return
        }
        const data = await res.json()
        if (cancelled) return
        if (!res.ok) throw new Error(data?.error || 'Failed to generate plan')
        const planNode = data?.plan
        if (planNode?.days && Array.isArray(planNode.days)) {
          const normalized = planNode.days.map((d: any, i: number) => ({
            ...d,
            day: `Day ${i + 1}`,
            modules: Array.isArray(d.modules) ? d.modules.map((m: any) => ({ ...m, type: (m.type === 'article' ? 'video' : m.type) || 'video' })) : []
          }))
          setStudyPlan(normalized)
        } else {
          const parsed = typeof data?.planText === 'string' ? JSON.parse(data.planText) : null
          if (parsed?.days && Array.isArray(parsed.days)) {
            const normalized = parsed.days.map((d: any, i: number) => ({
              ...d,
              day: `Day ${i + 1}`,
              modules: Array.isArray(d.modules) ? d.modules.map((m: any) => ({ ...m, type: (m.type === 'article' ? 'video' : m.type) || 'video' })) : []
            }))
            setStudyPlan(normalized)
          }
        }
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Failed to load study plan')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    fetchPlan()
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [goal?.title, goal?.daysLeft])

  useEffect(() => {
    if (goal?.title) return
    let cancelled = false
    const fetchGoals = async () => {
      setLoadingGoals(true)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const res = await apiFetch(`${base}/api/goals`)
        const list = await res.json()
        if (!cancelled) setGoalsList(Array.isArray(list) ? list : [])
      } catch {
        if (!cancelled) setGoalsList([])
      } finally {
        if (!cancelled) setLoadingGoals(false)
      }
    }
    fetchGoals()
    return () => { cancelled = true }
  }, [goal?.title])

  // The AI invents a `day.date` string (seen values like "2023-11-01") that
  // isn't tied to real calendar time, so it can't be used to find "today".
  // Instead: the plan was generated for exactly `goal.daysLeft` days *at
  // generation time* (see the fetchPlan request above), and `goal.daysLeft`
  // is a live, server-computed countdown that shrinks by 1 each real day --
  // so `studyPlan.length - goal.daysLeft` is a reliable, backend-free count
  // of days elapsed since the plan started.
  const currentDayIndex = useMemo(() => {
    if (!studyPlan.length) return -1
    const elapsed = studyPlan.length - (goal?.daysLeft ?? 0)
    return Math.max(0, Math.min(elapsed, studyPlan.length - 1))
  }, [studyPlan.length, goal?.daysLeft])

  // First incomplete module from today onward -- where "Continue here" points.
  const nextModule = useMemo(() => {
    if (currentDayIndex < 0) return null
    for (let d = currentDayIndex; d < studyPlan.length; d++) {
      const found = studyPlan[d]?.modules?.find((m: any) => !m.completed)
      if (found) return found
    }
    return null
  }, [studyPlan, currentDayIndex])

  const moduleRefs = useRef<Record<number, HTMLDivElement | null>>({})
  const hasScrolledToNext = useRef(false)

  useEffect(() => {
    if (!nextModule || hasScrolledToNext.current) return
    const el = moduleRefs.current[nextModule.id]
    if (el) {
      hasScrolledToNext.current = true
      const t = setTimeout(() => {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }, 300)
      return () => clearTimeout(t)
    }
  }, [nextModule])

  const handleModuleClick = (module: Module) => {
    setSelectedModule(module)
    setIsModalOpen(true)
  }

  const handleStartLearning = () => {
    if (selectedModule) {
      setIsModalOpen(false)
      if (goal?.title && onStartLearning) {
        onStartLearning(goal.title, selectedModule)
      }
    }
  }

  // Left-border accent only -- deliberately no bg-* override here, so the
  // glass Card background/blur stays intact instead of being painted over
  // by a flat solid color.
  const getTypeColor = (type: string) => {
    switch (type) {
      case "video":
        return "border-l-4 border-l-blue-500 dark:border-l-blue-400"
      case "article":
        return "border-l-4 border-l-emerald-500 dark:border-l-emerald-400"
      case "quiz":
        return "border-l-4 border-l-orange-500 dark:border-l-orange-400"
      default:
        return "border-l-4 border-l-purple-500 dark:border-l-purple-400"
    }
  }

  const getTypeAccent = (type: string) => {
    switch (type) {
      case "video":
        return "text-blue-600 dark:text-blue-300"
      case "article":
        return "text-emerald-600 dark:text-emerald-300"
      case "quiz":
        return "text-orange-600 dark:text-orange-300"
      default:
        return "text-purple-600 dark:text-purple-300"
    }
  }

  const getTypeLabel = (type: string) => {
    if (!type) return "Module"
    return type.charAt(0).toUpperCase() + type.slice(1)
  }

  if (!goal?.title) {
    return (
      <div className="p-6 md:p-8 max-w-4xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-foreground mb-1">Select a Goal</h1>
          <p className="text-muted-foreground text-sm">Choose a goal to view its study plan</p>
        </div>
        {loadingGoals ? (
          <div className="space-y-3">
            {[0, 1, 2].map((i) => (
              <Card key={i} className="p-4">
                <div className="flex items-center justify-between">
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-40" />
                    <Skeleton className="h-3 w-16" />
                  </div>
                  <Skeleton className="h-4 w-4 rounded-full" />
                </div>
              </Card>
            ))}
          </div>
        ) : goalsList.length === 0 ? (
          <div className="text-center py-6">
            <p className="text-sm text-muted-foreground mb-4">No goals yet.</p>
            <Button onClick={() => onNavigate('goals')}>
              Create a goal
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {goalsList.map((g) => (
              <Card key={g.id} className="p-4 hover:bg-muted/40 hover:-translate-y-0.5 transition-all cursor-pointer animate-in fade-in-0 duration-200" onClick={() => onSelectGoal && onSelectGoal(g)}>
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="font-semibold text-foreground">{g.title}</h3>
                    <p className="text-xs text-muted-foreground">{g.daysLeft} days</p>
                  </div>
                  <ChevronRight size={18} className="text-muted-foreground" />
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="p-6 md:p-8 max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Your Study Plan</h1>
        <p className="text-muted-foreground">{goal?.title || 'Custom Plan'} - {goal?.daysLeft ? `${goal.daysLeft} days` : 'Flexible'}</p>
      </div>

      {loading ? (
        <AILoading
          messages={[
            "Reading your goal…",
            "Breaking it into daily modules…",
            "Matching videos and articles…",
            "Almost ready…",
          ]}
        />
      ) : subscriptionRequired ? (
        <Card className="p-6 mb-6">
          <p className="text-sm font-medium text-foreground mb-3">Your trial has ended. Upgrade to generate new study plans.</p>
          <Button onClick={() => onNavigate('billing')}>
            View plans
          </Button>
        </Card>
      ) : error ? (
        <p className="text-sm text-red-600">{error}</p>
      ) : null}

      <div className="space-y-6">
        {studyPlan.map((day, dayIndex) => {
          const isToday = dayIndex === currentDayIndex
          return (
          <div
            key={dayIndex}
            className={`animate-in fade-in-0 duration-200 ${isToday ? 'ring-2 ring-primary/50 rounded-2xl p-4 -m-4' : ''}`}
          >
            <div className="flex items-center gap-4 mb-4">
              <div className="flex-1 flex items-center gap-2">
                <h2 className="text-xl font-bold text-foreground">{day.day}</h2>
                {isToday && (
                  <Badge className="border-transparent bg-gradient-to-r from-primary to-secondary text-primary-foreground">
                    Today
                  </Badge>
                )}
              </div>
              <div className="text-sm font-semibold text-primary">
                {day.modules.filter((m: any) => m.completed).length}/{day.modules.length} completed
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {day.modules.map((module: any) => {
                const isNext = nextModule?.id === module.id
                return (
                <Card
                  key={module.id}
                  ref={(el) => { moduleRefs.current[module.id] = el }}
                  className={`group p-4 cursor-pointer transition-all hover:shadow-lg hover:-translate-y-0.5 ${getTypeColor(module.type)} ${isNext ? 'ring-2 ring-primary glow-primary' : ''}`}
                  onClick={() => handleModuleClick(module)}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2 flex-wrap">
                        <span className={`text-xs font-semibold uppercase ${getTypeAccent(module.type)}`}>{getTypeLabel(module.type)}</span>
                        <span className="text-xs text-muted-foreground">{module.duration}</span>
                        {isNext && (
                          <Badge className="border-transparent bg-gradient-to-r from-primary to-secondary text-primary-foreground gap-1">
                            <Sparkles size={10} /> Continue here
                          </Badge>
                        )}
                      </div>
                      <h3 className="font-semibold text-foreground">{module.title}</h3>
                    </div>
                    <div className="flex-shrink-0">
                      {module.completed ? (
                        <CheckCircle2 size={24} className={`${getTypeAccent(module.type)} animate-in zoom-in-50 duration-300`} />
                      ) : (
                        <Circle size={24} className="text-muted-foreground opacity-40" />
                      )}
                    </div>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    className={`w-full mt-3 ${getTypeAccent(module.type)}`}
                  >
                    {module.completed ? "Review" : "Start"}
                    <ChevronRight size={16} className="transition-transform group-hover:translate-x-0.5" />
                  </Button>
                </Card>
                )
              })}
            </div>
          </div>
          )
        })}
      </div>

      <TaskDetailsModal
        module={selectedModule}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onStart={handleStartLearning}
      />
    </div>
  )
}
