'use client'

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { CheckCircle2, Circle, ChevronRight } from "lucide-react"
import TaskDetailsModal from "./task-details-modal"

interface Module {
  id: number;
  title: string;
  duration: string;
  completed: boolean;
  type: string;
  description: string;
}

export default function StudyPlan({ onNavigate }: { onNavigate: (page: string) => void }) {
  const [selectedModule, setSelectedModule] = useState<Module | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const studyPlan = [
    {
      day: "Monday",
      date: "Jan 15",
      modules: [
        { id: 1, title: "Limits & Continuity", duration: "45 min", completed: true, type: "video", description: "An introduction to the fundamental concepts of limits and continuity, including definitions and examples." },
        { id: 2, title: "Derivatives Basics", duration: "60 min", completed: true, type: "article", description: "Learn the basics of derivatives, including the power rule and how to find the derivative of simple functions." },
      ],
    },
    {
      day: "Tuesday",
      date: "Jan 16",
      modules: [
        { id: 3, title: "Power Rule & Chain Rule", duration: "50 min", completed: false, type: "video", description: "A deep dive into the power rule and chain rule, with examples of how to apply them to more complex functions." },
        { id: 4, title: "Practice Problems Set 1", duration: "40 min", completed: false, type: "quiz", description: "A set of practice problems to test your understanding of derivatives and the power and chain rules." },
      ],
    },
    {
      day: "Wednesday",
      date: "Jan 17",
      modules: [
        { id: 5, title: "Integration Fundamentals", duration: "55 min", completed: false, type: "video", description: "An introduction to integration, including the concept of the antiderivative and the basic rules of integration." },
        { id: 6, title: "Integration Techniques", duration: "45 min", completed: false, type: "article", description: "Learn various integration techniques, including substitution and integration by parts." },
      ],
    },
    {
      day: "Thursday",
      date: "Jan 18",
      modules: [
        { id: 7, title: "Applications of Calculus", duration: "60 min", completed: false, type: "video", description: "Explore the real-world applications of calculus, including optimization problems and related rates." },
        { id: 8, title: "Real-world Problems", duration: "50 min", completed: false, type: "quiz", description: "A set of real-world problems to test your ability to apply calculus concepts to practical scenarios." },
      ],
    },
  ]

  const handleModuleClick = (module: Module) => {
    setSelectedModule(module)
    setIsModalOpen(true)
  }

  const handleStartLearning = () => {
    if (selectedModule) {
      setIsModalOpen(false)
      onNavigate("learning")
    }
  }

  const getTypeColor = (type: string) => {
    switch (type) {
      case "video":
        return "bg-blue-50 text-blue-700 border-l-4 border-l-blue-500"
      case "article":
        return "bg-green-50 text-green-700 border-l-4 border-l-green-500"
      case "quiz":
        return "bg-orange-50 text-orange-700 border-l-4 border-l-orange-500"
      default:
        return "bg-purple-50 text-purple-700 border-l-4 border-l-purple-500"
    }
  }

  const getTypeLabel = (type: string) => {
    return type.charAt(0).toUpperCase() + type.slice(1)
  }

  return (
    <div className="p-6 md:p-8 max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Your Study Plan</h1>
        <p className="text-muted-foreground">Master Calculus - 4 weeks program</p>
      </div>

      <div className="space-y-6">
        {studyPlan.map((day, dayIndex) => (
          <div key={dayIndex}>
            <div className="flex items-center gap-4 mb-4">
              <div className="flex-1">
                <h2 className="text-xl font-bold text-foreground">{day.day}</h2>
                <p className="text-sm text-muted-foreground">{day.date}</p>
              </div>
              <div className="text-sm font-semibold text-primary">
                {day.modules.filter((m) => m.completed).length}/{day.modules.length} completed
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {day.modules.map((module) => (
                <Card
                  key={module.id}
                  className={`p-4 cursor-pointer transition-all hover:shadow-md ${getTypeColor(module.type)}`}
                  onClick={() => handleModuleClick(module)}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="text-xs font-semibold uppercase opacity-75">{getTypeLabel(module.type)}</span>
                        <span className="text-xs opacity-60">{module.duration}</span>
                      </div>
                      <h3 className="font-semibold text-foreground">{module.title}</h3>
                    </div>
                    <div className="flex-shrink-0">
                      {module.completed ? (
                        <CheckCircle2 size={24} className="opacity-75" />
                      ) : (
                        <Circle size={24} className="opacity-40" />
                      )}
                    </div>
                  </div>
                  <Button
                    className="w-full mt-3 bg-primary/20 text-primary hover:bg-primary/30 border border-current"
                    size="sm"
                  >
                    {module.completed ? "Review" : "Start"} <ChevronRight size={16} />
                  </Button>
                </Card>
              ))}
            </div>
          </div>
        ))}
      </div>

      <TaskDetailsModal
        module={selectedModule}
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onStart={handleStartLearning}
      />
    </div>
  )
}
