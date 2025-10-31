'use client'

import { useState, useEffect } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { CheckCircle2, MessageSquare, FileText, BookMarked, Save, Loader2 } from "lucide-react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"

interface LearningScreenProps {
  onNavigate: (page: string) => void;
  learningState: any;
  setLearningState: (state: any) => void;
}

export default function LearningScreen({ onNavigate, learningState, setLearningState }: LearningScreenProps) {
  const { isCompleted, notes, messages, inputMessage, chatLoading } = learningState;
  const [saveStatus, setSaveStatus] = useState("idle"); // idle, saving, saved

  const setState = (newState: any) => {
    setLearningState({ ...learningState, ...newState });
  };

  const topic = "Power Rule & Chain Rule";

  const sendMessage = async () => {
    if (inputMessage.trim()) {
      const newMessages = [...messages, { id: messages.length + 1, role: "user", text: inputMessage }];
      setState({ messages: newMessages, inputMessage: "", chatLoading: true });

      try {
        const endpoint = process.env.NEXT_PUBLIC_BACKEND_URL
          ? `${process.env.NEXT_PUBLIC_BACKEND_URL}/api/ai-chat`
          : "http://localhost:8080/api/ai-chat";
        const payloadMessages = newMessages.map((m: any) => ({ role: m.role, text: m.text }));
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
        const res = await fetch(endpoint, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify({
            messages: payloadMessages,
            systemPrompt: `You are a friendly, student-focused AI tutor. Write answers in clean Markdown (headings, lists, bold) with step-by-step clarity. Do NOT use LaTeX or $...$ or \\(...\\) or \\[...\\]. Use plain-text math: exponents with ^ (x^2), fractions as a/b, and inline code for short expressions (like x^2 + 1). Stay strictly on the topic: ${topic}.`,
          }),
        });
        if (!res.ok) throw new Error("Failed to get AI response");
        const data = await res.json();
        const reply = (data?.reply ?? "I couldn't generate a response.").toString();
        const clean = reply.replace(/\$/g, "").replace(/\\[()\\[\\]]/g, "");
        setState({
          messages: [
            ...newMessages,
            {
              id: newMessages.length + 1,
              role: "tutor",
              text: clean,
            },
          ],
          chatLoading: false,
        });
      } catch (e) {
        setState({
          messages: [
            ...newMessages,
            {
              id: newMessages.length + 1,
              role: "tutor",
              text: "Sorry, I had trouble answering that. Please try again.",
            },
          ],
          chatLoading: false,
        });
      }
    }
  }

  const handleSaveNotes = async () => {
    setSaveStatus("saving");
    await new Promise(resolve => setTimeout(resolve, 1500));
    setSaveStatus("saved");
    setTimeout(() => setSaveStatus("idle"), 3000);
  };

  const handleDownloadNotes = () => {
    const blob = new Blob([notes], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'notes.txt';
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="p-6 md:p-8 max-w-7xl mx-auto">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-1">{topic}</h1>
          <p className="text-muted-foreground">Calculus Fundamentals</p>
        </div>
        <Button
          onClick={() => setState({ isCompleted: !isCompleted })}
          className={`${isCompleted ? "bg-green-600 hover:bg-green-700" : "bg-primary hover:bg-primary/90"} text-white`}
        >
          <CheckCircle2 size={20} className="mr-2" />
          {isCompleted ? "Completed" : "Mark Complete"}
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <Tabs defaultValue="video" className="w-full">
            <TabsList className="grid w-full grid-cols-4 bg-muted">
              <TabsTrigger value="video">Video</TabsTrigger>
              <TabsTrigger value="article">Article</TabsTrigger>
              <TabsTrigger value="notes">Notes</TabsTrigger>
              <TabsTrigger value="chat">Chat</TabsTrigger>
            </TabsList>

            {/* Video Tab */}
            <TabsContent value="video" className="mt-4">
              <Card className="p-4 bg-card border border-border">
                <div className="aspect-video bg-muted rounded-lg">
                  {/* Placeholder for video player */}
                </div>
              </Card>
            </TabsContent>

            {/* Article Tab */}
            <TabsContent value="article" className="mt-4">
              <Card className="p-6 bg-card border border-border">
                <div className="prose prose-sm max-w-none text-foreground">
                  <h3 className="font-semibold mb-4">The Power Rule Explained</h3>
                  <p className="text-sm leading-relaxed mb-4">
                    The power rule is one of the most fundamental rules in calculus. It states that if f(x) = x^n, then
                    f'(x) = n·x^(n-1).
                  </p>
                  <p className="text-sm leading-relaxed mb-4">
                    This rule applies to any real number n, making it incredibly versatile for differentiating
                    polynomial functions.
                  </p>
                  <div className="bg-primary/10 border border-primary/20 rounded-lg p-4 my-4">
                    <p className="text-sm font-mono">d/dx(x^n) = n·x^(n-1)</p>
                  </div>
                  <p className="text-sm leading-relaxed">
                    Let's look at some examples to solidify your understanding...
                  </p>
                </div>
              </Card>
            </TabsContent>

            {/* Notes Tab */}
            <TabsContent value="notes" className="mt-4">
              <Card className="p-6 bg-card border border-border">
                <textarea
                  value={notes}
                  onChange={(e) => setState({ notes: e.target.value })}
                  placeholder="Take notes here... Your notes will be saved automatically"
                  className="w-full h-64 px-4 py-3 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary resize-none"
                />
                <div className="mt-4 flex items-center gap-4">
                  <Button 
                    onClick={handleSaveNotes} 
                    disabled={saveStatus === 'saving'}
                    className="bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                  >
                    <Save size={16} className="mr-2" />
                    {saveStatus === 'saving' ? 'Saving...' : saveStatus === 'saved' ? 'Saved!' : 'Save Notes'}
                  </Button>
                  {saveStatus === 'saved' && (
                    <Button onClick={handleSaveNotes} variant="outline">
                      Save Again
                    </Button>
                  )}
                </div>
              </Card>
            </TabsContent>

            {/* Chat Tab */}
            <TabsContent value="chat" className="mt-4">
              <Card className="p-6 bg-card border border-border flex flex-col h-96">
                <div className="flex-1 overflow-y-auto mb-4 space-y-4">
                  {messages.map((msg: any) => (
                    <div key={msg.id} className={`flex ${msg.role === "user" ? "justify-end" : "justify-start"}`}>
                      <div
                        className={`max-w-xs px-4 py-2 rounded-lg ${
                          msg.role === "user" ? "bg-primary text-primary-foreground" : "bg-muted text-foreground"
                        }`}
                      >
                        {msg.role === "tutor" ? (
                          <div className="text-sm leading-relaxed">
                            <ReactMarkdown className="markdown" remarkPlugins={[remarkGfm]}>
                              {msg.text}
                            </ReactMarkdown>
                          </div>
                        ) : (
                          <p className="text-sm whitespace-pre-wrap">{msg.text}</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={inputMessage}
                    onChange={(e) => setState({ inputMessage: e.target.value })}
                    onKeyPress={(e) => e.key === "Enter" && !chatLoading && sendMessage()}
                    placeholder="Ask your tutor..."
                    className="flex-1 px-4 py-2 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    disabled={!!chatLoading}
                  />
                  <Button onClick={sendMessage} disabled={!!chatLoading} className="bg-primary text-primary-foreground hover:bg-primary/90">
                    {chatLoading ? <Loader2 size={16} className="mr-2 animate-spin" /> : null}
                    Send
                  </Button>
                </div>
              </Card>
            </TabsContent>
          </Tabs>
        </div>

        {/* Sidebar */}
        <div className="space-y-4">
          {/* Learning Resources */}
          <Card className="p-4 bg-card border border-border">
            <h3 className="font-semibold text-foreground mb-3 flex items-center gap-2">
              <BookMarked size={18} />
              Resources
            </h3>
            <div className="space-y-2">
              <div className="p-3 rounded-lg bg-blue-50 border-l-4 border-l-blue-500 cursor-pointer hover:bg-blue-100 transition-colors">
                <p className="text-sm font-medium text-blue-900">Video Lecture</p>
                <p className="text-xs text-blue-700">15 min</p>
              </div>
              <div className="p-3 rounded-lg bg-green-50 border-l-4 border-l-green-500 cursor-pointer hover:bg-green-100 transition-colors">
                <p className="text-sm font-medium text-green-900">Reading Material</p>
                <p className="text-xs text-green-700">8 pages</p>
              </div>
              <div className="p-3 rounded-lg bg-orange-50 border-l-4 border-l-orange-500 cursor-pointer hover:bg-orange-100 transition-colors">
                <p className="text-sm font-medium text-orange-900">Practice Problems</p>
                <p className="text-xs text-orange-700">10 questions</p>
              </div>
            </div>
          </Card>

          {/* Progress */}
          <Card className="p-4 bg-card border border-border">
            <h3 className="font-semibold text-foreground mb-3">Module Progress</h3>
            <div className="space-y-3">
              <div>
                <div className="flex justify-between items-center mb-1">
                  <span className="text-sm text-foreground">Completion</span>
                  <span className="text-sm font-semibold text-primary">75%</span>
                </div>
                <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                  <div className="h-full w-3/4 bg-primary rounded-full" />
                </div>
              </div>
            </div>
          </Card>

          {/* Quick Actions */}
          <Card className="p-4 bg-card border border-border">
            <h3 className="font-semibold text-foreground mb-3">Quick Actions</h3>
            <div className="space-y-2">
              <Button onClick={handleDownloadNotes} className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90 justify-start">
                <FileText size={16} className="mr-2" />
                Download Notes
              </Button>
              <Button onClick={() => onNavigate('chat')} className="w-full bg-accent text-accent-foreground hover:bg-accent/90 justify-start">
                <MessageSquare size={16} className="mr-2" />
                Ask Question
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
