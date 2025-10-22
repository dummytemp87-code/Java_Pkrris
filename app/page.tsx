"use client"

import { useState } from "react"
import Navigation from "@/components/navigation"
import Dashboard from "@/components/pages/dashboard"
import GoalCreation from "@/components/pages/goal-creation"
import StudyPlan from "@/components/pages/study-plan"
import LearningScreen from "@/components/pages/learning-screen"
import AIChat from "@/components/pages/ai-chat"
import ResourceLibrary from "@/components/pages/resource-library"
import Analytics from "@/components/pages/analytics"
import Settings from "@/components/pages/settings"

export default function Home() {
  const [currentPage, setCurrentPage] = useState("dashboard")

  const renderPage = () => {
    switch (currentPage) {
      case "dashboard":
        return <Dashboard />
      case "goals":
        return <GoalCreation />
      case "study-plan":
        return <StudyPlan />
      case "learning":
        return <LearningScreen />
      case "chat":
        return <AIChat />
      case "resources":
        return <ResourceLibrary />
      case "analytics":
        return <Analytics />
      case "settings":
        return <Settings />
      default:
        return <Dashboard />
    }
  }

  return (
    <div className="flex h-screen bg-background">
      <Navigation currentPage={currentPage} onNavigate={setCurrentPage} />
      <main className="flex-1 overflow-auto">{renderPage()}</main>
    </div>
  )
}
