'use client'

import { useState, useEffect } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { CheckCircle2, MessageSquare, FileText, BookMarked, Save, Loader2, ArrowLeft } from "lucide-react"
import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"

interface LearningScreenProps {
  onNavigate: (page: string) => void;
  learningState: any;
  setLearningState: (state: any) => void;
  onProgressUpdated?: () => void | Promise<void>;
}

export default function LearningScreen({ onNavigate, learningState, setLearningState, onProgressUpdated }: LearningScreenProps) {
  const { isCompleted, notes, messages, inputMessage, chatLoading, selectedGoalTitle, selectedModule } = learningState;
  const [saveStatus, setSaveStatus] = useState("idle"); // idle, saving, saved
  const [articleLoading, setArticleLoading] = useState(false)
  const [articleError, setArticleError] = useState<string | null>(null)
  const [articleContent, setArticleContent] = useState<string>("")
  const [videoLoading, setVideoLoading] = useState(false)
  const [videoError, setVideoError] = useState<string | null>(null)
  const [video, setVideo] = useState<{ videoId: string; url: string; videoTitle?: string; channelTitle?: string } | null>(null)
  const [moduleProgress, setModuleProgress] = useState<{ percent: number; done: number; total: number }>({ percent: 0, done: 0, total: 0 })

  const setState = (newState: any) => {
    setLearningState({ ...learningState, ...newState });
  };

  const topic = selectedModule?.title || "Learning Module";

  useEffect(() => {
    if (!selectedGoalTitle || !selectedModule?.title) return;
    let cancelled = false;
    const run = async () => {
      setArticleLoading(true);
      setArticleError(null);
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
        const res = await fetch(`${base}/api/articles/content`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          body: JSON.stringify({
            goalTitle: selectedGoalTitle,
            moduleTitle: selectedModule.title,
            moduleType: selectedModule.type,
            moduleId: selectedModule.id,
          })
        })
        const data = await res.json();
        if (!res.ok) throw new Error(data?.error || 'Failed to load article');
        if (!cancelled) setArticleContent((data?.content ?? '').toString());
      } catch (e: any) {
        if (!cancelled) setArticleError(e?.message || 'Failed to load article');
      } finally {
        if (!cancelled) setArticleLoading(false);
      }
    };
    run();
    return () => { cancelled = true; };
  }, [selectedGoalTitle, selectedModule?.title])

  useEffect(() => {
    if (!selectedGoalTitle || !selectedModule?.title) return;
    let cancelled = false;
    const run = async () => {
      setVideoLoading(true);
      setVideoError(null);
      const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
      const language = typeof window !== 'undefined' ? (localStorage.getItem('language') || 'english') : 'english';
      let lastErr: any = null;
      for (let attempt = 1; attempt <= 2; attempt++) {
        try {
          const res = await fetch(`${base}/api/videos/content`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
            body: JSON.stringify({
              goalTitle: selectedGoalTitle,
              moduleTitle: selectedModule.title,
              moduleId: selectedModule.id,
              language,
            })
          })
          const data = await res.json();
          if (!res.ok) throw new Error(data?.error || 'Failed to load video');
          if (!cancelled) setVideo({ videoId: data.videoId, url: data.url, videoTitle: data.videoTitle, channelTitle: data.channelTitle });
          lastErr = null;
          break;
        } catch (e: any) {
          lastErr = e;
          await new Promise(r => setTimeout(r, 200 * attempt));
        }
      }
      if (!cancelled) {
        if (lastErr) setVideoError(lastErr?.message || 'Failed to load video');
        setVideoLoading(false);
      }
    };
    run();
    return () => { cancelled = true; };
  }, [selectedGoalTitle, selectedModule?.title, selectedModule?.id])

  // Fetch module/goal progress for sidebar
  useEffect(() => {
    const fetchProgress = async () => {
      if (!selectedGoalTitle) return;
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
        const res = await fetch(`${base}/api/study-plan/progress?goalTitle=${encodeURIComponent(selectedGoalTitle)}`, {
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          }
        })
        const data = await res.json();
        if (res.ok) {
          const done = Number(data?.completedModules ?? 0)
          const total = Number(data?.totalModules ?? 0)
          const percent = Number(data?.goalProgress ?? (total > 0 ? Math.round(done * 100 / total) : 0))
          setModuleProgress({ percent, done, total })
        }
      } catch {}
    }
    fetchProgress()
  }, [selectedGoalTitle])

  const toggleCompletion = async () => {
    if (!selectedGoalTitle || !selectedModule?.id) {
      setState({ isCompleted: !isCompleted })
      return
    }
    const newVal = !isCompleted
    setState({ isCompleted: newVal })
    try {
      const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
      const res = await fetch(`${base}/api/study-plan/complete`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({
          goalTitle: selectedGoalTitle,
          moduleId: selectedModule.id,
          moduleTitle: selectedModule.title,
          completed: newVal,
        })
      })
      const data = await res.json()
      if (res.ok) {
        const done = Number(data?.completedModules ?? 0)
        const total = Number(data?.totalModules ?? 0)
        const percent = Number(data?.goalProgress ?? (total > 0 ? Math.round(done * 100 / total) : 0))
        setModuleProgress({ percent, done, total })
        if (onProgressUpdated) await onProgressUpdated()
      }
    } catch {}
  }

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
        <div className="flex items-center gap-3">
          <Button variant="outline" onClick={() => onNavigate('study-plan')} className="flex items-center">
            <ArrowLeft size={16} className="mr-2" /> Back to Study Plan
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-foreground mb-1">{topic}</h1>
            <p className="text-muted-foreground">{selectedGoalTitle || 'Personalized Learning'}</p>
          </div>
        </div>
        <Button
          onClick={toggleCompletion}
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
                {videoLoading ? (
                  <p className="text-sm text-muted-foreground">Loading video...</p>
                ) : videoError ? (
                  <p className="text-sm text-red-600">{videoError}</p>
                ) : video && video.videoId ? (
                  <>
                    <div className="aspect-video rounded-lg overflow-hidden">
                      <iframe
                        className="w-full h-full"
                        src={`https://www.youtube.com/embed/${video.videoId}`}
                        title={video.videoTitle || 'YouTube video player'}
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        allowFullScreen
                      />
                    </div>
                    <div className="mt-2">
                      {video.videoTitle ? <p className="text-sm font-medium text-foreground">{video.videoTitle}</p> : null}
                      {video.channelTitle ? <p className="text-xs text-muted-foreground">{video.channelTitle}</p> : null}
                    </div>
                  </>
                ) : (
                  <p className="text-sm text-muted-foreground">No video available.</p>
                )}
              </Card>
            </TabsContent>

            {/* Article Tab */}
            <TabsContent value="article" className="mt-4">
              <Card className="p-6 bg-card border border-border">
                {articleLoading ? (
                  <p className="text-sm text-muted-foreground">Loading article...</p>
                ) : articleError ? (
                  <p className="text-sm text-red-600">{articleError}</p>
                ) : articleContent ? (
                  <div className="prose prose-sm max-w-none text-foreground">
                    <ReactMarkdown className="markdown" remarkPlugins={[remarkGfm]}>
                      {articleContent}
                    </ReactMarkdown>
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">Select a module to load content.</p>
                )}
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
                  <span className="text-sm font-semibold text-primary">{moduleProgress.percent}%</span>
                </div>
                <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                  <div className="h-full bg-primary rounded-full" style={{ width: `${moduleProgress.percent}%` }} />
                </div>
                <div className="mt-1 text-xs text-muted-foreground">{moduleProgress.done}/{moduleProgress.total} modules completed</div>
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
