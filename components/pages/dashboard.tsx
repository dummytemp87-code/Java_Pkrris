"use client"

import { Card } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { CheckCircle2, Clock, Zap, TrendingUp } from "lucide-react"

export default function Dashboard() {
  const dailyTasks = [
    { id: 1, title: "Complete Calculus Chapter 3", duration: "45 min", completed: true },
    { id: 2, title: "Review Biology Notes", duration: "30 min", completed: true },
    { id: 3, title: "Practice Physics Problems", duration: "60 min", completed: false },
    { id: 4, title: "Read History Article", duration: "25 min", completed: false },
  ]

  const goals = [
    { id: 1, title: "Master Calculus", progress: 65, daysLeft: 12 },
    { id: 2, title: "Biology Fundamentals", progress: 42, daysLeft: 18 },
    { id: 3, title: "Physics Concepts", progress: 78, daysLeft: 8 },
  ]

  const completedToday = dailyTasks.filter((t) => t.completed).length
  const totalTime = dailyTasks.reduce((acc, t) => {
    const minutes = Number.parseInt(t.duration)
    return acc + minutes
  }, 0)

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
              <p className="text-3xl font-bold text-primary">62%</p>
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
              {dailyTasks.map((task) => (
                <div
                  key={task.id}
                  className="flex items-center gap-4 p-4 rounded-lg bg-muted/30 hover:bg-muted/50 transition-colors"
                >
                  <input
                    type="checkbox"
                    defaultChecked={task.completed}
                    className="w-5 h-5 rounded border-2 border-primary cursor-pointer"
                  />
                  <div className="flex-1">
                    <p
                      className={`font-medium ${task.completed ? "line-through text-muted-foreground" : "text-foreground"}`}
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
                    <h3 className="font-medium text-foreground text-sm">{goal.title}</h3>
                    <span className="text-xs text-muted-foreground">{goal.daysLeft}d left</span>
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
