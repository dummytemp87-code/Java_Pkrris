"use client"

import { useState } from "react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { TypingIndicator } from "@/components/ui/typing-indicator"
import { Send, Lightbulb, Loader2, Lock } from "lucide-react"
import { apiFetch } from "@/lib/api"

type Goal = { id: number; title: string; progress: number; daysLeft: number }

export default function AIChat({ goals = [], onNavigate }: { goals?: Goal[]; onNavigate?: (page: string) => void }) {
  const [messages, setMessages] = useState([
    {
      id: 1,
      role: "tutor",
      text: "Hello! I'm your AI tutor. I can help you understand any concept, solve problems, or clarify doubts. What would you like to learn about today?",
    },
  ])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(false)
  const [subscriptionRequired, setSubscriptionRequired] = useState(false)

  const suggestedQuestions = goals.length > 0
    ? [
        `Explain the basics of ${goals[0].title}`,
        `What should I focus on first for ${goals[0].title}?`,
        `Quiz me on ${goals[0].title}`,
        goals[1] ? `How does ${goals[0].title} relate to ${goals[1].title}?` : `Give me a real-world example of ${goals[0].title}`,
      ]
    : [
        "Explain this concept simply",
        "Give me a real-world example",
        "What are common mistakes to avoid?",
        "Can you quiz me on this?",
      ]

  const sendMessage = async () => {
    if (!input.trim() || loading) return
    const userMsg = { id: messages.length + 1, role: "user", text: input }
    setMessages([...messages, userMsg])
    setInput("")
    setLoading(true)
    try {
      const endpoint = process.env.NEXT_PUBLIC_BACKEND_URL
        ? `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/ai-chat`
        : "http://localhost:8080/api/ai-chat";
      const payloadMessages = [...messages, userMsg].map((m: any) => ({ role: m.role, text: m.text }))
      const res = await apiFetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messages: payloadMessages }),
      })
      if (res.status === 402) {
        setSubscriptionRequired(true)
        return
      }
      if (!res.ok) throw new Error("Failed to get AI response")
      const data = await res.json()
      const reply = (data?.reply ?? "I couldn't generate a response.").toString()
      const clean = reply.replace(/\$/g, "").replace(/\\[()\\[\\]]/g, "")
      setMessages((prev) => [
        ...prev,
        { id: prev.length + 1, role: "tutor", text: clean },
      ])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { id: prev.length + 1, role: "tutor", text: "Sorry, I had trouble answering that. Please try again." },
      ])
    } finally {
      setLoading(false)
    }
  }

  if (subscriptionRequired) {
    return (
      <div className="p-6 md:p-8 max-w-4xl mx-auto h-screen flex flex-col items-center justify-center text-center">
        <div className="w-14 h-14 rounded-full bg-muted flex items-center justify-center mb-4">
          <Lock size={24} className="text-muted-foreground" />
        </div>
        <h2 className="text-2xl font-bold text-foreground mb-2">Your trial has ended</h2>
        <p className="text-muted-foreground max-w-sm mb-6">
          Upgrade to a paid plan to keep chatting with your AI tutor and unlock unlimited AI features.
        </p>
        <Button onClick={() => onNavigate?.('billing')} className="px-6">
          View plans
        </Button>
      </div>
    )
  }

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto h-screen flex flex-col">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-foreground mb-2">AI Tutor</h1>
        <p className="text-muted-foreground">Ask anything about your studies</p>
      </div>

      <Card className="flex-1 p-6 flex flex-col mb-6">
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
                {msg.role === "tutor" ? (
                  <div className="text-sm leading-relaxed">
                    <ReactMarkdown className="markdown" remarkPlugins={[remarkGfm]}>
                      {msg.text}
                    </ReactMarkdown>
                  </div>
                ) : (
                  <p className="text-sm leading-relaxed whitespace-pre-wrap">{msg.text}</p>
                )}
              </div>
            </div>
          ))}
          {loading && <TypingIndicator />}
        </div>

        {/* Input */}
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === "Enter" && sendMessage()}
            placeholder="Type your question..."
            autoComplete="off"
            className="flex-1 px-4 py-3 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
            disabled={loading}
          />
          <Button onClick={sendMessage} disabled={loading} className="px-6">
            {loading ? <Loader2 size={20} className="animate-spin" /> : <Send size={20} />}
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
              className="p-3 rounded-lg glass hover:bg-accent/10 transition-colors text-left text-sm text-foreground hover:text-primary font-medium"
            >
              {question}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
