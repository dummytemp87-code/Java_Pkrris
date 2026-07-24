"use client"

import { useEffect, useRef, useState } from "react"
import { toast } from "sonner"
import Script from "next/script"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { CheckCircle2, Loader2 } from "lucide-react"
import { celebrateBig } from "@/lib/confetti"

type BillingStatus = {
  plan: string
  status: string
  trialEndsAt?: string | null
  currentPeriodEnd?: string | null
}

declare global {
  interface Window {
    Razorpay: any
  }
}

const PLANS = [
  {
    id: "starter",
    name: "Starter",
    price: "₹199",
    features: ["Unlimited goals", "AI-generated study plans", "Up to 50 AI chat messages/day"],
  },
  {
    id: "pro",
    name: "Pro",
    price: "₹399",
    features: ["Everything in Starter", "Unlimited AI chat", "Syllabus upload (PDF/DOCX/image)", "Priority generation"],
  },
]

export default function Billing() {
  const [status, setStatus] = useState<BillingStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [subscribing, setSubscribing] = useState<string | null>(null)
  const [scriptReady, setScriptReady] = useState(false)
  const wasActive = useRef(false)
  const isFirstFetch = useRef(true)

  const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null

  const fetchStatus = async () => {
    try {
      const res = await fetch(`${base}/api/billing/status`, {
        headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      })
      const data = await res.json()
      if (res.ok) {
        const nowActive = data?.status === 'ACTIVE'
        if (nowActive && !wasActive.current && !isFirstFetch.current) {
          celebrateBig()
          toast.success("Subscription activated — you're all set!")
        }
        wasActive.current = nowActive
        isFirstFetch.current = false
        setStatus(data)
      }
    } catch {
      // ignore, keep last known status
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchStatus()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const daysLeft = (() => {
    if (!status?.trialEndsAt) return null
    const ms = new Date(status.trialEndsAt).getTime() - Date.now()
    return Math.max(0, Math.ceil(ms / (1000 * 60 * 60 * 24)))
  })()

  const handleSubscribe = async (planId: string) => {
    if (!scriptReady || !window.Razorpay) {
      toast.error("Payment widget is still loading, try again in a moment.")
      return
    }
    setSubscribing(planId)
    try {
      const res = await fetch(`${base}/api/billing/subscribe`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ plan: planId }),
      })
      const data = await res.json()
      if (!res.ok) {
        toast.error(data?.error || 'Failed to start checkout')
        return
      }
      const rzp = new window.Razorpay({
        key: data.keyId,
        subscription_id: data.subscriptionId,
        name: "StudyHub",
        description: `${planId === 'pro' ? 'Pro' : 'Starter'} plan`,
        handler: () => {
          // Informational only -- the webhook is the source of truth for activation.
          // Poll status a couple of times in case the webhook lands a moment after this.
          fetchStatus()
          setTimeout(fetchStatus, 3000)
          setTimeout(fetchStatus, 8000)
        },
        theme: { color: "#4f46e5" },
      })
      rzp.open()
    } catch (e) {
      toast.error('Failed to start checkout')
    } finally {
      setSubscribing(null)
    }
  }

  const isActive = status?.status === 'ACTIVE'
  const isTrialing = status?.status === 'TRIALING'

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto">
      <Script src="https://checkout.razorpay.com/v1/checkout.js" strategy="lazyOnload" onLoad={() => setScriptReady(true)} />

      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Billing</h1>
        <p className="text-muted-foreground">Manage your StudyHub subscription</p>
      </div>

      {loading ? (
        <Card className="p-6 mb-8">
          <Skeleton className="h-4 w-64" />
        </Card>
      ) : (
        <Card className="p-6 mb-8 animate-in fade-in-0 duration-200">
          {isActive ? (
            <p className="text-sm font-medium text-foreground">
              You're on the <span className="capitalize">{status?.plan?.toLowerCase()}</span> plan. Thanks for subscribing!
            </p>
          ) : isTrialing ? (
            <p className="text-sm font-medium text-foreground">
              {daysLeft !== null && daysLeft > 0
                ? `${daysLeft} day${daysLeft === 1 ? '' : 's'} left in your free trial.`
                : "Your free trial has ended."}{" "}
              <span className="text-muted-foreground font-normal">Choose a plan below to keep using AI features.</span>
            </p>
          ) : (
            <p className="text-sm font-medium text-foreground">Choose a plan to get started.</p>
          )}
        </Card>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {PLANS.map((plan) => (
          <Card
            key={plan.id}
            className="p-6 flex flex-col transition-all hover:shadow-md hover:-translate-y-0.5 animate-in fade-in-0 duration-200"
          >
            <h3 className="text-xl font-bold text-foreground mb-1">{plan.name}</h3>
            <p className="text-3xl font-bold text-foreground mb-4">
              {plan.price}
              <span className="text-sm font-normal text-muted-foreground">/month</span>
            </p>
            <ul className="space-y-2 mb-6 flex-1">
              {plan.features.map((f) => (
                <li key={f} className="flex items-start gap-2 text-sm text-muted-foreground">
                  <CheckCircle2 size={16} className="text-primary shrink-0 mt-0.5" />
                  {f}
                </li>
              ))}
            </ul>
            <Button
              onClick={() => handleSubscribe(plan.id)}
              disabled={subscribing !== null || (isActive && status?.plan?.toLowerCase() === plan.id)}
              className="w-full font-semibold"
            >
              {subscribing === plan.id ? (
                <Loader2 size={18} className="animate-spin" />
              ) : isActive && status?.plan?.toLowerCase() === plan.id ? (
                "Current plan"
              ) : (
                `Subscribe to ${plan.name}`
              )}
            </Button>
          </Card>
        ))}
      </div>
    </div>
  )
}
