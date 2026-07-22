"use client"

import { useEffect, useState } from "react"
import { useTheme } from "next-themes"
import {
  LayoutDashboard,
  Target,
  Calendar,
  BookOpen,
  MessageSquare,
  BarChart3,
  Settings,
  Menu,
  X,
  Moon,
  Sun,
  CreditCard,
} from "lucide-react"

interface NavigationProps {
  currentPage: string
  onNavigate: (page: string) => void
  showLearn?: boolean
}

export default function Navigation({ currentPage, onNavigate, showLearn }: NavigationProps) {
  const [isOpen, setIsOpen] = useState(false)
  const { theme, setTheme } = useTheme()
  const [mounted, setMounted] = useState(false)

  useEffect(() => setMounted(true), [])
  const isDark = mounted && theme === "dark"

  const navItems = [
    { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
    { id: "goals", label: "Goals", icon: Target },
    { id: "study-plan", label: "Study Plan", icon: Calendar },
    // "Learn" is only a meaningful, distinct option once a module has actually
    // been started (it resumes it) -- until then it's functionally identical
    // to "Study Plan", which reads as a confusing duplicate to new users.
    ...(showLearn ? [{ id: "learning", label: "Learn", icon: BookOpen }] : []),
    { id: "chat", label: "AI Tutor", icon: MessageSquare },
    { id: "analytics", label: "Analytics", icon: BarChart3 },
    { id: "billing", label: "Billing", icon: CreditCard },
    { id: "settings", label: "Settings", icon: Settings },
  ]

  const toggleTheme = () => {
    setTheme(isDark ? "light" : "dark")
  }

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed top-4 left-4 z-50 md:hidden p-2 rounded-lg bg-primary text-primary-foreground"
      >
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      {/* Sidebar */}
      <aside
        className={`fixed md:relative w-64 h-screen bg-sidebar border-r border-sidebar-border transition-transform duration-300 z-40 ${
          isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        }`}
      >
        <div className="p-6 border-b border-sidebar-border">
          <h1 className="text-2xl font-bold text-sidebar-primary">StudyHub</h1>
          <p className="text-sm text-sidebar-foreground/60">Learn Smarter</p>
        </div>

        <nav className="p-4 space-y-2">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = currentPage === item.id
            return (
              <button
                key={item.id}
                onClick={() => {
                  onNavigate(item.id)
                  setIsOpen(false)
                }}
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  isActive
                    ? "bg-sidebar-primary text-sidebar-primary-foreground"
                    : "text-sidebar-foreground hover:bg-sidebar-accent/20"
                }`}
              >
                <Icon size={20} />
                <span className="font-medium">{item.label}</span>
              </button>
            )
          })}
        </nav>

        {/* Theme toggle */}
        <div className="absolute bottom-6 left-4 right-4">
          <button
            onClick={toggleTheme}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 rounded-lg bg-sidebar-accent/20 text-sidebar-foreground hover:bg-sidebar-accent/30 transition-colors"
          >
            {isDark ? <Sun size={20} /> : <Moon size={20} />}
            <span className="text-sm font-medium">{isDark ? "Light" : "Dark"}</span>
          </button>
        </div>
      </aside>

      {/* Mobile overlay */}
      {isOpen && <div className="fixed inset-0 bg-black/50 z-30 md:hidden" onClick={() => setIsOpen(false)} />}
    </>
  )
}
