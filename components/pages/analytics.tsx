"use client"

import { Card } from "@/components/ui/card"
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts"
import { useEffect, useState } from "react"

export default function Analytics() {
  const [loading, setLoading] = useState(false)
  const [studyTimeData, setStudyTimeData] = useState<Array<{ day: string; hours: number }>>([])
  const [progressData, setProgressData] = useState<Array<{ week: string; progress: number }>>([])
  const [contentTypeData, setContentTypeData] = useState<Array<{ name: string; value: number }>>([])
  const [totalMinutesWeek, setTotalMinutesWeek] = useState(0)
  const [modulesCompleted, setModulesCompleted] = useState(0)
  const [modulesTotal, setModulesTotal] = useState(0)
  const [averageQuizScore, setAverageQuizScore] = useState(0)
  const [currentStreakDays, setCurrentStreakDays] = useState(0)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/analytics/summary`, { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        const data = await res.json().catch(() => ({}))
        if (!cancelled && res.ok) {
          setTotalMinutesWeek(Number(data?.totalStudyMinutesThisWeek || 0))
          setModulesCompleted(Number(data?.modulesCompleted || 0))
          setModulesTotal(Number(data?.modulesTotal || 0))
          setAverageQuizScore(Number(data?.averageQuizScore || 0))
          setCurrentStreakDays(Number(data?.currentStreakDays || 0))
          setStudyTimeData(Array.isArray(data?.studyTime) ? data.studyTime : [])
          setProgressData(Array.isArray(data?.progress) ? data.progress : [])
          setContentTypeData(Array.isArray(data?.contentType) ? data.contentType : [])
        }
      } catch {
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  const COLORS = ["#4f46e5", "#06b6d4", "#f97316", "#8b5cf6"]

  return (
    <div className="p-6 md:p-8 max-w-7xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Your Progress</h1>
        <p className="text-muted-foreground">Track your learning journey</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
        <Card className="p-6 bg-card border border-border">
          <p className="text-sm text-muted-foreground mb-1">Total Study Time</p>
          <p className="text-3xl font-bold text-primary">{Math.round((totalMinutesWeek/60) * 10) / 10} hrs</p>
          <p className="text-xs text-muted-foreground mt-2">This week</p>
        </Card>
        <Card className="p-6 bg-card border border-border">
          <p className="text-sm text-muted-foreground mb-1">Modules Completed</p>
          <p className="text-3xl font-bold text-secondary">{modulesCompleted}</p>
          <p className="text-xs text-muted-foreground mt-2">Out of {modulesTotal}</p>
        </Card>
        <Card className="p-6 bg-card border border-border">
          <p className="text-sm text-muted-foreground mb-1">Average Score</p>
          <p className="text-3xl font-bold text-accent">{averageQuizScore}%</p>
          <p className="text-xs text-muted-foreground mt-2">On quizzes</p>
        </Card>
        <Card className="p-6 bg-card border border-border">
          <p className="text-sm text-muted-foreground mb-1">Current Streak</p>
          <p className="text-3xl font-bold text-primary">{currentStreakDays} days</p>
          <p className="text-xs text-muted-foreground mt-2">Keep it up!</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        {/* Study Time Chart */}
        <Card className="p-6 bg-card border border-border">
          <h3 className="text-lg font-bold text-foreground mb-4">Study Time This Week</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={studyTimeData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
              <XAxis dataKey="day" stroke="var(--color-muted-foreground)" />
              <YAxis stroke="var(--color-muted-foreground)" />
              <Tooltip
                contentStyle={{
                  backgroundColor: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "8px",
                }}
              />
              <Bar dataKey="hours" fill="var(--color-primary)" radius={[8, 8, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>

        {/* Progress Chart */}
        <Card className="p-6 bg-card border border-border">
          <h3 className="text-lg font-bold text-foreground mb-4">Overall Progress</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={progressData}>
              <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
              <XAxis dataKey="week" stroke="var(--color-muted-foreground)" />
              <YAxis stroke="var(--color-muted-foreground)" />
              <Tooltip
                contentStyle={{
                  backgroundColor: "var(--color-card)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "8px",
                }}
              />
              <Line
                type="monotone"
                dataKey="progress"
                stroke="var(--color-secondary)"
                strokeWidth={2}
                dot={{ fill: "var(--color-secondary)", r: 5 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Card>
      </div>

      {/* Content Type Distribution */}
      <Card className="p-6 bg-card border border-border">
        <h3 className="text-lg font-bold text-foreground mb-4">Learning Content Distribution</h3>
        <div className="flex flex-col md:flex-row items-center justify-center gap-8">
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={contentTypeData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, value }) => `${name}: ${value}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {contentTypeData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
          <div className="space-y-3">
            {contentTypeData.map((item, index) => (
              <div key={index} className="flex items-center gap-3">
                <div className="w-4 h-4 rounded" style={{ backgroundColor: COLORS[index % COLORS.length] }} />
                <span className="text-sm text-foreground">
                  {item.name}: {item.value}%
                </span>
              </div>
            ))}
          </div>
        </div>
      </Card>
    </div>
  )
}
