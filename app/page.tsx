'use client'

import { useState } from 'react'
import Navigation from '@/components/navigation'
import Dashboard from '@/components/pages/dashboard'
import GoalCreation from '@/components/pages/goal-creation'
import StudyPlan from '@/components/pages/study-plan'
import LearningScreen from '@/components/pages/learning-screen'
import AIChat from '@/components/pages/ai-chat'
import ResourceLibrary from '@/components/pages/resource-library'
import Analytics from '@/components/pages/analytics'
import Settings from '@/components/pages/settings'

export default function Home() {
  const [currentPage, setCurrentPage] = useState('dashboard')
  const [goals, setGoals] = useState([
    { id: 1, title: "Master Calculus", progress: 65, daysLeft: 12 },
    { id: 2, title: "Biology Fundamentals", progress: 42, daysLeft: 18 },
    { id: 3, title: "Physics Concepts", progress: 78, daysLeft: 8 },
  ])

  const [learningScreenState, setLearningScreenState] = useState({
    isCompleted: false,
    notes: "",
    messages: [
      { id: 1, role: "tutor", text: "Hi! I'm your AI tutor. What would you like to learn about derivatives?" },
    ],
    inputMessage: ""
});

  const handleDeleteGoal = (id: number) => {
    setGoals(goals.filter(goal => goal.id !== id));
  };

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <Dashboard onNavigate={setCurrentPage} goals={goals} onDeleteGoal={handleDeleteGoal} />
      case 'goals':
        return <GoalCreation setGoals={setGoals} onNavigate={setCurrentPage} />
      case 'study-plan':
        return <StudyPlan onNavigate={setCurrentPage} />
      case 'learning':
        return <LearningScreen 
                  onNavigate={setCurrentPage} 
                  learningState={learningScreenState} 
                  setLearningState={setLearningScreenState} 
                />
      case 'chat':
        return <AIChat />
      case 'resources':
        return <ResourceLibrary />
      case 'analytics':
        return <Analytics />
      case 'settings':
        return <Settings />
      default:
        return <Dashboard onNavigate={setCurrentPage} goals={goals} onDeleteGoal={handleDeleteGoal} />
    }
  }

  return (
    <div className='flex h-screen bg-background'>
      <Navigation currentPage={currentPage} onNavigate={setCurrentPage} />
      <main className='flex-1 overflow-auto'>{renderPage()}</main>
    </div>
  )
}
