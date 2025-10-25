'use client'

import { Card } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { CheckCircle2, Clock, Zap, TrendingUp, X } from "lucide-react"
import { useState, useMemo } from "react"

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
}: { 
  onNavigate: (page: string) => void; 
  goals: Goal[];
  onDeleteGoal: (id: number) => void;
  onSelectGoal?: (goal: Goal) => void;
}) {
  const [dailyTasks, setDailyTasks] = useState([
    { id: 1, title: "Complete Calculus Chapter 3", duration: "45 min", completed: true },
    { id: 2, title: "Review Biology Notes", duration: "30 min", completed: true },
    { id: 3, title: "Practice Physics Problems", duration: "60 min", completed: false },
    { id: 4, title: "Read History Article", duration: "25 min", completed: false },
  ])

  const sortedTasks = useMemo(() => {
    return [...dailyTasks].sort((a, b) =>
      a.completed === b.completed ? 0 : a.completed ? 1 : -1
    )
  }, [dailyTasks])

  const completedToday = dailyTasks.filter((t) => t.completed).length
  const totalTime = dailyTasks.reduce((acc, t) => {
    const minutes = Number.parseInt(t.duration)
    return acc + minutes
  }, 0)

  const overallProgress = Math.round(
    goals.reduce((acc, goal) => acc + goal.progress, 0) / goals.length
  )

  const handleTaskCompletion = (id: number) => {
    setDailyTasks(
      dailyTasks.map((task) =>
        task.id === id ? { ...task, completed: !task.completed } : task
      )
    )
  }

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
              <p className="text-3xl font-bold text-primary">
                {completedToday}/{dailyTasks.length}
              </p>
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
              <p className="text-3xl font-bold text-accent">7 days</p>
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
            <div className="space-y-3">
              {sortedTasks.map((task) => (
                <div
                  key={task.id}
                  className="flex items-center gap-4 p-4 rounded-lg bg-muted/30 hover:bg-muted/50 transition-colors cursor-pointer"
                  onClick={() => onNavigate("learning")}
                >
                  <input
                    type="checkbox"
                    checked={task.completed}
                    onChange={() => handleTaskCompletion(task.id)}
                    onClick={(e) => e.stopPropagation()}
                    className="w-5 h-5 rounded border-2 border-primary cursor-pointer"
                  />
                  <div className="flex-1">
                    <p
                      className={`font-medium ${
                        task.completed ? "line-through text-muted-foreground" : "text-foreground"
                      }`}
                    >
                      {task.title}
                    </p>
                  </div>
                  <span className="text-sm text-muted-foreground">{task.duration}</span>
                </div>
              ))}
            </div>
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
