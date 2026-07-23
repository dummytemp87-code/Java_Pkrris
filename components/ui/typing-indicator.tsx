import { cn } from '@/lib/utils'

// Drops into a chat thread as a transient "tutor is typing" bubble while
// waiting on a real AI reply -- styled to match the existing tutor message
// bubble (bg-muted, bordered, rounded-lg) so it reads as part of the
// conversation rather than a generic loading widget.
export function TypingIndicator({ className }: { className?: string }) {
  return (
    <div className={cn('flex justify-start', className)}>
      <div className="flex items-center gap-1.5 px-4 py-3 rounded-lg bg-muted border border-border">
        {[0, 1, 2].map((i) => (
          <span
            key={i}
            className="w-1.5 h-1.5 rounded-full bg-muted-foreground motion-safe:animate-[bounce-dot_1.2s_ease-in-out_infinite]"
            style={{ animationDelay: `${i * 0.15}s` }}
          />
        ))}
      </div>
    </div>
  )
}
