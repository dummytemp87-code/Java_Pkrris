'use client'

import { useEffect, useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { CheckCircle2, Circle, ChevronRight } from "lucide-react"
import TaskDetailsModal from "./task-details-modal"

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

  const [studyPlan, setStudyPlan] = useState<any[]>([])
  const [goalsList, setGoalsList] = useState<Goal[]>([])
  const [loadingGoals, setLoadingGoals] = useState(false)

  useEffect(() => {
    if (!goal?.title) return
    const fetchPlan = async () => {
      setLoading(true)
      setError(null)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/study-plan/generate`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify({
            goalTitle: goal?.title || 'General Study',
            days: goal?.daysLeft ? Math.max(1, Math.min(7, goal.daysLeft)) : 4,
            level: 'beginner',
          })
        })
        const data = await res.json()
        if (!res.ok) throw new Error(data?.error || 'Failed to generate plan')
        const planNode = data?.plan
        if (planNode?.days && Array.isArray(planNode.days)) {
          setStudyPlan(planNode.days)
        } else {
          const parsed = typeof data?.planText === 'string' ? JSON.parse(data.planText) : null
          if (parsed?.days && Array.isArray(parsed.days)) setStudyPlan(parsed.days)
        }
      } catch (e: any) {
        setError(e?.message || 'Failed to load study plan')
      } finally {
        setLoading(false)
      }
    }
    fetchPlan()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [goal?.title])

  useEffect(() => {
    if (goal?.title) return
    const fetchGoals = async () => {
      setLoadingGoals(true)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/goals`, { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        const list = await res.json()
        setGoalsList(Array.isArray(list) ? list : [])
      } catch {
        setGoalsList([])
      } finally {
        setLoadingGoals(false)
      }
    }
    fetchGoals()
  }, [goal?.title])

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
      onNavigate("learning")
    }
  }

  const getTypeColor = (type: string) => {
    switch (type) {
      case "video":
        return "bg-blue-50 text-blue-700 border-l-4 border-l-blue-500"
      case "article":
        return "bg-green-50 text-green-700 border-l-4 border-l-green-500"
      case "quiz":
        return "bg-orange-50 text-orange-700 border-l-4 border-l-orange-500"
      default:
        return "bg-purple-50 text-purple-700 border-l-4 border-l-purple-500"
    }
  }

  const getTypeLabel = (type: string) => {
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
          <p className="text-sm text-muted-foreground">Loading your goals...</p>
        ) : goalsList.length === 0 ? (
          <p className="text-sm text-muted-foreground">No goals yet. Create one from Goals page.</p>
        ) : (
          <div className="space-y-3">
            {goalsList.map((g) => (
              <Card key={g.id} className="p-4 hover:bg-muted/40 cursor-pointer border border-border" onClick={() => onSelectGoal && onSelectGoal(g)}>
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
        <p className="text-sm text-muted-foreground">Generating your study plan...</p>
      ) : error ? (
        <p className="text-sm text-red-600">{error}</p>
      ) : null}

      <div className="space-y-6">
        {studyPlan.map((day, dayIndex) => (
          <div key={dayIndex}>
            <div className="flex items-center gap-4 mb-4">
              <div className="flex-1">
                <h2 className="text-xl font-bold text-foreground">{day.day || `Day ${dayIndex + 1}`}</h2>
                <p className="text-sm text-muted-foreground">{day.date || ''}</p>
              </div>
              <div className="text-sm font-semibold text-primary">
                {day.modules.filter((m: any) => m.completed).length}/{day.modules.length} completed
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {day.modules.map((module: any) => (
                <Card
                  key={module.id}
                  className={`p-4 cursor-pointer transition-all hover:shadow-md ${getTypeColor(module.type)}`}
                  onClick={() => handleModuleClick(module)}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-xs font-semibold uppercase opacity-75">{getTypeLabel(module.type)}</span>
                        <span className="text-xs opacity-60">{module.duration}</span>
                      </div>
                      <h3 className="font-semibold text-foreground">{module.title}</h3>
                    </div>
                    <div className="flex-shrink-0">
                      {module.completed ? (
                        <CheckCircle2 size={24} className="opacity-75" />
                      ) : (
                        <Circle size={24} className="opacity-40" />
                      )}
                    </div>
                  </div>
                  <Button
                    className="w-full mt-3 bg-primary/20 text-primary hover:bg-primary/30 border border-current"
                    size="sm"
                  >
                    {module.completed ? "Review" : "Start"} <ChevronRight size={16} />
                  </Button>
                </Card>
              ))}
            </div>
          </div>
        ))}
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
