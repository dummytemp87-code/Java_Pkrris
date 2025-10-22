"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Send, Lightbulb } from "lucide-react"

export default function AIChat() {
  const [messages, setMessages] = useState([
    {
      id: 1,
      role: "tutor",
      text: "Hello! I'm your AI tutor. I can help you understand any concept, solve problems, or clarify doubts. What would you like to learn about today?",
    },
  ])
  const [input, setInput] = useState("")

  const suggestedQuestions = [
    "Explain the chain rule with examples",
    "How do I solve this derivative problem?",
    "What's the difference between limits and continuity?",
    "Can you give me practice problems?",
  ]

  const sendMessage = () => {
    if (input.trim()) {
      setMessages([...messages, { id: messages.length + 1, role: "user", text: input }])
      setInput("")
      // Simulate response
      setTimeout(() => {
        setMessages((prev) => [
          ...prev,
          {
            id: prev.length + 1,
            role: "tutor",
            text: "That's a great question! Let me break this down for you step by step...",
          },
        ])
      }, 800)
    }
  }

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto h-screen flex flex-col">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground mb-2">AI Tutor</h1>
        <p className="text-muted-foreground">Ask anything about your studies</p>
      </div>

      <Card className="flex-1 p-6 bg-card border border-border flex flex-col mb-6">
        {/* Messages */}
        <div className="flex-1 overflow-y-auto mb-6 space-y-4">
          {messages.map((msg) => (
            <div key={msg.id} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-md px-4 py-3 rounded-lg ${
                  msg.role === "user"
                    ? "bg-primary text-primary-foreground"
                    : "bg-muted text-foreground border border-border"
                }`}
              >
                <p className="text-sm leading-relaxed">{msg.text}</p>
              </div>
            </div>
          ))}
        </div>

        {/* Input */}
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === "Enter" && sendMessage()}
            placeholder="Type your question..."
            className="flex-1 px-4 py-3 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          />
          <Button onClick={sendMessage} className="bg-primary text-primary-foreground hover:bg-primary/90 px-6">
            <Send size={20} />
          </Button>
        </div>
      </Card>

      {/* Suggested Questions */}
      <div>
        <div className="flex items-center gap-2 mb-3">
          <Lightbulb size={18} className="text-accent" />
          <h3 className="font-semibold text-foreground">Suggested Questions</h3>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {suggestedQuestions.map((question, index) => (
            <button
              key={index}
              onClick={() => {
                setInput(question)
              }}
              className="p-3 rounded-lg border border-border bg-card hover:bg-muted transition-colors text-left text-sm text-foreground hover:text-primary font-medium"
            >
              {question}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
