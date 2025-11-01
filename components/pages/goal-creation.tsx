'use client'

import type React from "react"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Upload, Plus, X } from "lucide-react"

interface Goal {
  id: number;
  title: string;
  progress: number;
  daysLeft: number;
}

interface GoalCreationProps {
  setGoals: React.Dispatch<React.SetStateAction<Goal[]>>;
  onNavigate: (page: string) => void;
}

export default function GoalCreation({ setGoals, onNavigate }: GoalCreationProps) {
  const [goalTitle, setGoalTitle] = useState("")
  const [description, setDescription] = useState("")
  const [deadline, setDeadline] = useState("")
  const [topics, setTopics] = useState<string[]>([])
  const [newTopic, setNewTopic] = useState("")
  const [syllabus, setSyllabus] = useState<File | null>(null)

  const addTopic = () => {
    if (newTopic.trim()) {
      setTopics([...topics, newTopic])
      setNewTopic("")
    }
  }

  const removeTopic = (index: number) => {
    setTopics(topics.filter((_, i) => i !== index))
  }

  const handleSyllabusUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) {
      const file = e.target.files[0];
      setSyllabus(file);
      setGoalTitle(file.name.replace(/\.[^/.]+$/, "")); // Pre-fill title
    }
  }

  const handleCreateGoal = async () => {
    const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    try {
      const res = await fetch(`${base}/api/goals`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          title: goalTitle,
          description,
          targetDate: deadline || null,
          topics,
        })
      })
      const data = await res.json();
      if (!res.ok) {
        alert(data?.error || 'Failed to create goal');
        return;
      }
      setGoals((prev) => [...prev, data]);
      onNavigate("dashboard");
    } catch (e) {
      alert('Failed to create goal');
    }
  }

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Create a New Goal</h1>
        <p className="text-muted-foreground">Define your learning objective and let AI break it down</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Form */}
        <div className="lg:col-span-2 space-y-6">
          {/* Goal Title */}
          <Card className="p-6 bg-card border border-border">
            <label className="block text-sm font-semibold text-foreground mb-2">Goal Title</label>
            <input
              type="text"
              value={goalTitle}
              onChange={(e) => setGoalTitle(e.target.value)}
              placeholder="e.g., Master Advanced Calculus"
              className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </Card>

          {/* Description */}
          <Card className="p-6 bg-card border border-border">
            <label className="block text-sm font-semibold text-foreground mb-2">Description</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What do you want to learn and why?"
              rows={4}
              className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-none"
            />
          </Card>

          {/* Deadline */}
          <Card className="p-6 bg-card border border-border">
            <label className="block text-sm font-semibold text-foreground mb-2">Target Deadline</label>
            <input
              type="date"
              value={deadline}
              onChange={(e) => setDeadline(e.target.value)}
              className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </Card>

          {/* Syllabus Upload */}
          <Card className="p-6 bg-card border border-border">
            <label className="block text-sm font-semibold text-foreground mb-4">Upload Syllabus (Optional)</label>
            <div className="border-2 border-dashed border-border rounded-lg p-8 text-center hover:border-primary transition-colors cursor-pointer">
              <input type="file" onChange={handleSyllabusUpload} className="hidden" id="syllabus-upload" />
              <label htmlFor="syllabus-upload" className="cursor-pointer">
                <Upload className="mx-auto mb-2 text-muted-foreground" size={32} />
                <p className="text-sm font-medium text-foreground">
                  {syllabus ? syllabus.name : "Click to upload or drag and drop"}
                </p>
                <p className="text-xs text-muted-foreground mt-1">PDF, DOC, or TXT files</p>
              </label>
            </div>
          </Card>
        </div>

        {/* Topics Sidebar */}
        <div>
          <Card className="p-6 bg-card border border-border sticky top-6">
            <h3 className="text-lg font-bold text-foreground mb-4">Topics to Cover</h3>

            <div className="space-y-3 mb-4">
              {topics.map((topic, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between gap-2 p-3 rounded-lg bg-primary/10 border border-primary/20"
                >
                  <span className="text-sm font-medium text-foreground">{topic}</span>
                  <button
                    onClick={() => removeTopic(index)}
                    className="text-muted-foreground hover:text-destructive transition-colors"
                  >
                    <X size={16} />
                  </button>
                </div>
              ))}
            </div>

            <div className="flex gap-2 mb-4">
              <input
                type="text"
                value={newTopic}
                onChange={(e) => setNewTopic(e.target.value)}
                onKeyPress={(e) => e.key === "Enter" && addTopic()}
                placeholder="Add a topic..."
                className="flex-1 px-3 py-2 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground text-sm focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <Button onClick={addTopic} className="bg-primary text-primary-foreground hover:bg-primary/90">
                <Plus size={16} />
              </Button>
            </div>

            <div className="p-4 rounded-lg bg-secondary/10 border border-secondary/20 mb-6">
              <p className="text-xs text-muted-foreground">
                💡 AI will analyze your syllabus and break down topics into focused learning modules
              </p>
            </div>

            <Button
              onClick={handleCreateGoal}
              className="w-full bg-primary text-primary-foreground hover:bg-primary/90 font-semibold"
            >
              Create Goal
            </Button>
          </Card>
        </div>
      </div>
    </div>
  )
}
