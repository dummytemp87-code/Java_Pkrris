'use client'

import { useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { AILoading } from '@/components/ui/ai-loading'
import { celebrateBig, celebrateSmall } from '@/lib/confetti'
import { apiFetch } from '@/lib/api'

interface Module {
  id: number
  title: string
  duration: string
  completed: boolean
  type: string
  description: string
}

export default function QuizPage({ onNavigate, goalTitle, module, onProgressUpdated }: { onNavigate: (page: string) => void, goalTitle: string, module: Module, onProgressUpdated?: () => void | Promise<void> }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [quiz, setQuiz] = useState<any | null>(null)
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<{ score: number, total: number, percent: number } | null>(null)
  const [subscriptionRequired, setSubscriptionRequired] = useState(false)

  const base = useMemo(() => process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080', [])

  useEffect(() => {
    if (!goalTitle || !module?.title) return
    let cancelled = false
    const run = async () => {
      setLoading(true)
      setError(null)
      setSubscriptionRequired(false)
      try {
        const res = await apiFetch(`${base}/api/quiz/generate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ goalTitle, moduleTitle: module.title, moduleId: module.id })
        })
        if (res.status === 402) {
          if (!cancelled) setSubscriptionRequired(true)
          return
        }
        const data = await res.json()
        if (!res.ok) throw new Error(data?.error || 'Failed to load quiz')
        const q = data?.quiz || (typeof data?.quizText === 'string' ? JSON.parse(data.quizText) : null)
        if (!cancelled) setQuiz(q)
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Failed to load quiz')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    run()
    return () => { cancelled = true }
  }, [goalTitle, module?.title, module?.id])

  const setAnswer = (qid: number, idx: number) => {
    setAnswers(prev => ({ ...prev, [qid]: idx }))
  }

  const handleSubmit = async () => {
    if (!quiz?.questions?.length) return
    setSubmitting(true)
    setError(null)
    try {
      const body = {
        goalTitle,
        moduleTitle: module.title,
        moduleId: module.id,
        answers: Object.entries(answers).map(([qid, idx]) => ({ questionId: Number(qid), selectedIndex: Number(idx) }))
      }
      const res = await apiFetch(`${base}/api/quiz/submit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
      const data = await res.json()
      if (!res.ok) throw new Error(data?.error || 'Failed to submit quiz')
      const percent = data.percent ?? 0
      setResult({ score: data.score ?? 0, total: data.total ?? 0, percent })
      if (percent >= 80) {
        celebrateBig()
        toast.success(`${percent}% — great work!`)
      } else if (percent >= 50) {
        celebrateSmall()
        toast.success(`Quiz complete — ${percent}%`)
      } else {
        toast(`Quiz complete — ${percent}%. Review and try again anytime.`)
      }
      if (onProgressUpdated) await onProgressUpdated()
    } catch (e: any) {
      setError(e?.message || 'Failed to submit quiz')
    } finally {
      setSubmitting(false)
    }
  }

  const allAnswered = quiz?.questions?.every((q: any) => typeof answers[q.id] === 'number')

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="min-w-0">
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground mb-1">{module?.title || 'Quiz'}</h1>
          <p className="text-muted-foreground">{goalTitle}</p>
        </div>
        <Button variant="outline" onClick={() => onNavigate('study-plan')} className="shrink-0 w-full sm:w-auto">Back to Plan</Button>
      </div>

      {loading ? (
        <AILoading messages={["Reading the module…", "Writing questions…", "Almost ready…"]} />
      ) : subscriptionRequired ? (
        <Card className="p-6">
          <p className="text-sm font-medium text-foreground mb-3">Your trial has ended. Upgrade to generate new quizzes.</p>
          <Button onClick={() => onNavigate('billing')}>
            View plans
          </Button>
        </Card>
      ) : error ? (
        <p className="text-sm text-red-600">{error}</p>
      ) : quiz ? (
        <div className="space-y-4">
          <Card className="p-4">
            <h2 className="text-xl font-semibold text-foreground mb-2">{quiz.title || 'Module Quiz'}</h2>
            <div className="space-y-4">
              {Array.isArray(quiz.questions) && quiz.questions.map((q: any) => (
                <div key={q.id} className="p-3 rounded-lg border border-border">
                  <p className="font-medium text-foreground mb-2">{q.question}</p>
                  <div className="space-y-2">
                    {Array.isArray(q.options) && q.options.map((opt: string, idx: number) => (
                      <label key={idx} className="flex items-center gap-2 text-sm cursor-pointer">
                        <input
                          type="radio"
                          name={`q-${q.id}`}
                          checked={answers[q.id] === idx}
                          onChange={() => setAnswer(q.id, idx)}
                        />
                        <span>{opt}</span>
                      </label>
                    ))}
                  </div>
                  {result && typeof q.correctIndex === 'number' ? (
                    <div className="mt-2 text-xs text-muted-foreground">
                      <p>Correct answer: {q.options?.[q.correctIndex]}</p>
                      {q.explanation ? <p className="mt-1">Explanation: {q.explanation}</p> : null}
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          </Card>
          {!result ? (
            <Button onClick={handleSubmit} disabled={!allAnswered || submitting}>
              {submitting ? 'Grading…' : 'Submit Quiz'}
            </Button>
          ) : (
            <Card className="p-4">
              <p className="text-foreground">Score: <span className="font-semibold">{result.score}/{result.total}</span> ({result.percent}%)</p>
              <div className="mt-3 flex gap-2">
                <Button onClick={() => onNavigate('study-plan')}>Back to Study Plan</Button>
              </div>
            </Card>
          )}
        </div>
      ) : null}
    </div>
  )
}
