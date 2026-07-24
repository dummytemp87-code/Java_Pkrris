"use client"

import { useState } from "react"
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
  CreditCard,
  LogOut,
} from "lucide-react"

interface NavigationProps {
  currentPage: string
  onNavigate: (page: string) => void
  showLearn?: boolean
}

export default function Navigation({ currentPage, onNavigate, showLearn }: NavigationProps) {
  const [isOpen, setIsOpen] = useState(false)

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
        className={`nav-sidebar fixed md:relative w-64 h-[calc(100vh-1.5rem)] md:h-[calc(100vh-1.5rem)] m-3 glass rounded-2xl transition-transform duration-300 z-40 flex flex-col ${
          isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        }`}
      >
        <div className="p-6 pt-20 md:pt-6 border-b border-sidebar-border/30">
          <h1 className="text-2xl font-bold bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">StudyHub</h1>
          <p className="text-sm text-sidebar-foreground/60">Learn Smarter</p>
        </div>

        <nav className="p-4 space-y-2 flex-1 overflow-y-auto">
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
                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all ${
                  isActive
                    ? "bg-gradient-to-r from-primary to-secondary text-primary-foreground shadow-md shadow-primary/20"
                    : "text-sidebar-foreground hover:bg-sidebar-accent/20"
                }`}
              >
                <Icon size={20} />
                <span className="font-medium">{item.label}</span>
              </button>
            )
          })}
        </nav>

        <div className="p-4 border-t border-sidebar-border/30">
          <button
            onClick={() => {
              if (typeof window !== 'undefined') {
                localStorage.removeItem('token')
                window.location.reload()
              }
            }}
            className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-destructive hover:bg-destructive/10 transition-all"
          >
            <LogOut size={20} />
            <span className="font-medium">Logout</span>
          </button>
        </div>
      </aside>

      {/* Mobile overlay */}
      {isOpen && <div className="fixed inset-0 bg-black/50 z-30 md:hidden" onClick={() => setIsOpen(false)} />}
    </>
  )
}
