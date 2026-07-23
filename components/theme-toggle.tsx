"use client"

import { useEffect, useState } from "react"
import { useTheme } from "next-themes"
import { Moon, Sun } from "lucide-react"

export default function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => setMounted(true), [])
  const isDark = mounted && theme === "dark"

  return (
    <button
      type="button"
      role="switch"
      aria-checked={isDark}
      aria-label={isDark ? "Switch to light theme" : "Switch to dark theme"}
      onClick={() => setTheme(isDark ? "light" : "dark")}
      className="fixed top-4 right-4 z-50 glass flex items-center h-8 w-14 rounded-full p-1 transition-colors"
    >
      <span
        className={`flex items-center justify-center h-6 w-6 rounded-full bg-gradient-to-br from-primary to-secondary shadow-md transition-transform duration-300 ${
          isDark ? "translate-x-6" : "translate-x-0"
        }`}
      >
        {isDark ? <Moon size={13} className="text-primary-foreground" /> : <Sun size={13} className="text-primary-foreground" />}
      </span>
    </button>
  )
}
