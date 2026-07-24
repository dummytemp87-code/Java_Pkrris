"use client"

import Link from "next/link"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Target, MessageSquare, Library, BarChart3, ArrowRight } from "lucide-react"

interface LandingProps {
  onGetStarted: () => void
  onLogin: () => void
}

export default function Landing({ onGetStarted, onLogin }: LandingProps) {
  const features = [
    {
      icon: Target,
      title: "Goals & Study Plans",
      description: "Set a learning goal and get an AI-generated, day-by-day plan sized to your timeline.",
    },
    {
      icon: MessageSquare,
      title: "AI Tutor Chat",
      description: "Ask questions and get clear, step-by-step explanations whenever you're stuck.",
    },
    {
      icon: Library,
      title: "Curated Learning Content",
      description: "Get a matched video and article for every module as you study.",
    },
    {
      icon: BarChart3,
      title: "Progress Analytics",
      description: "Track streaks, study time, and completion across every goal you set.",
    },
  ]

  return (
    <div className="min-h-screen">
      {/* Header */}
      <header className="flex items-center justify-between px-6 py-5 md:px-12">
        <h1 className="text-xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">StudyHub</h1>
        <Button variant="ghost" onClick={onLogin}>
          Log in
        </Button>
      </header>

      {/* Hero */}
      <section className="px-6 md:px-12 py-16 md:py-24 max-w-4xl mx-auto text-center">
        <h2 className="text-4xl md:text-6xl font-bold mb-4 text-balance bg-gradient-to-r from-primary via-secondary to-accent bg-clip-text text-transparent">
          Your AI-powered study companion
        </h2>
        <p className="text-lg text-muted-foreground mb-8 max-w-2xl mx-auto text-pretty">
          Set a learning goal, get a personalized study plan, and learn with an AI tutor —
          all in one place.
        </p>
        <div className="flex items-center justify-center gap-3">
          <Button size="lg" onClick={onGetStarted}>
            Get Started
            <ArrowRight size={18} />
          </Button>
          <Button size="lg" variant="outline" onClick={onLogin}>
            Log in
          </Button>
        </div>
        <p className="text-sm text-muted-foreground mt-4">
          Start with a free 3-day trial — no card required.
        </p>
      </section>

      {/* Features */}
      <section className="px-6 md:px-12 pb-20 max-w-5xl mx-auto">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {features.map((feature) => {
            const Icon = feature.icon
            return (
              <Card key={feature.title} className="p-6 hover:-translate-y-1 hover:shadow-xl transition-all">
                <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-primary to-secondary flex items-center justify-center mb-4 shadow-md shadow-primary/20">
                  <Icon className="text-primary-foreground" size={20} />
                </div>
                <h3 className="font-semibold text-foreground mb-2">{feature.title}</h3>
                <p className="text-sm text-muted-foreground text-pretty">{feature.description}</p>
              </Card>
            )
          })}
        </div>
      </section>

      {/* Footer */}
      <footer className="px-6 md:px-12 py-6 border-t border-border/50 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm text-muted-foreground">
        <span>StudyHub — Learn Smarter</span>
        <div className="flex items-center gap-4">
          <Link href="/terms" className="hover:text-foreground transition-colors">Terms</Link>
          <Link href="/privacy" className="hover:text-foreground transition-colors">Privacy</Link>
          <Link href="/refund" className="hover:text-foreground transition-colors">Refunds</Link>
        </div>
      </footer>
    </div>
  )
}
