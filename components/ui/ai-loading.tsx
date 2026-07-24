'use client'

import { useEffect, useState } from 'react'
import { Sparkles, type LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface AILoadingProps {
  messages: string[]
  icon?: LucideIcon
  className?: string
  compact?: boolean
}

// Cycles through short, specific status lines instead of a bare spinner --
// for waits backed by a real multi-second AI call (plan generation, syllabus
// analysis, quiz generation), telling people what's actually happening reads
// as intentional instead of frozen. Stops on the last message rather than
// looping if the call runs long.
export function AILoading({ messages, icon: Icon = Sparkles, className, compact = false }: AILoadingProps) {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    setIndex(0)
    if (messages.length <= 1) return
    const id = setInterval(() => {
      setIndex((i) => Math.min(i + 1, messages.length - 1))
    }, 2200)
    return () => clearInterval(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [messages.join('|')])

  if (compact) {
    return (
      <div className={cn('flex items-center justify-center gap-3', className)}>
        <div className="flex items-center justify-center w-8 h-8 rounded-full bg-gradient-to-br from-primary to-secondary shrink-0 motion-safe:animate-pulse">
          <Icon size={14} className="text-primary-foreground" />
        </div>
        <p key={index} className="text-sm font-medium text-foreground animate-in fade-in-0 duration-300">
          {messages[index]}
        </p>
      </div>
    )
  }

  return (
    <div className={cn('flex flex-col items-center justify-center text-center gap-4 py-16', className)}>
      <div className="flex items-center justify-center w-14 h-14 rounded-full bg-gradient-to-br from-primary to-secondary glow-primary motion-safe:animate-pulse">
        <Icon size={24} className="text-primary-foreground" />
      </div>
      <p key={index} className="text-sm font-medium text-foreground animate-in fade-in-0 duration-300">
        {messages[index]}
      </p>
      <div className="w-40 h-1 rounded-full bg-muted overflow-hidden">
        <div className="h-full w-1/3 rounded-full bg-gradient-to-r from-primary to-secondary motion-safe:animate-[shimmer_1.6s_ease-in-out_infinite]" />
      </div>
    </div>
  )
}
