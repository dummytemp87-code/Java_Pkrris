"use client"

import { useEffect, useState } from "react"
import { toast } from "sonner"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Bell, Lock, User, Palette, X, Gift, Copy, Check } from "lucide-react"

const LANGUAGE_OPTIONS: { value: string; label: string }[] = [
  { value: "english", label: "English" },
  { value: "spanish", label: "Spanish" },
  { value: "french", label: "French" },
  { value: "german", label: "German" },
  { value: "hi", label: "Hindi" },
  { value: "ta", label: "Tamil" },
  { value: "te", label: "Telugu" },
  { value: "pt", label: "Portuguese" },
  { value: "it", label: "Italian" },
  { value: "zh", label: "Chinese" },
  { value: "ja", label: "Japanese" },
  { value: "ko", label: "Korean" },
  { value: "ar", label: "Arabic" },
  { value: "ru", label: "Russian" },
]
const languageLabel = (code: string) => LANGUAGE_OPTIONS.find((o) => o.value === code)?.label || code

export default function Settings() {
  type SettingsState = {
    emailNotifications: boolean;
    dailyReminders: boolean;
    weeklyReport: boolean;
    soundEnabled: boolean;
    theme: string;
    languages: string[];
  };

  const [settings, setSettings] = useState<SettingsState>({
    emailNotifications: true,
    dailyReminders: true,
    weeklyReport: false,
    soundEnabled: true,
    theme: "light",
    languages: ["english"],
  })
  const [languageToAdd, setLanguageToAdd] = useState(LANGUAGE_OPTIONS[0].value)

  useEffect(() => {
    const raw = typeof window !== 'undefined' ? localStorage.getItem('languagePreferences') : null
    if (raw) {
      try {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed) && parsed.length > 0) {
          setSettings((prev) => ({ ...prev, languages: parsed }))
          return
        }
      } catch {}
    }
    if (typeof window !== 'undefined') {
      localStorage.setItem('languagePreferences', JSON.stringify(["english"]))
    }
  }, [])

  useEffect(() => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('languagePreferences', JSON.stringify(settings.languages))
    }
    if (settings.languages.includes(languageToAdd)) {
      const next = LANGUAGE_OPTIONS.find((o) => !settings.languages.includes(o.value))
      if (next) setLanguageToAdd(next.value)
    }
  }, [settings.languages])

  const addLanguage = () => {
    if (settings.languages.includes(languageToAdd)) return
    setSettings((prev) => ({ ...prev, languages: [...prev.languages, languageToAdd] }))
  }
  const removeLanguage = (code: string) => {
    setSettings((prev) => ({ ...prev, languages: prev.languages.filter((l) => l !== code) }))
  }

  const [loadingSettings, setLoadingSettings] = useState(false)
  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle')

  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileName, setProfileName] = useState("")
  const [profileEmail, setProfileEmail] = useState("")
  const [referralCode, setReferralCode] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const [pwdCurrent, setPwdCurrent] = useState("")
  const [pwdNew, setPwdNew] = useState("")
  const [pwdStatus, setPwdStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle')

  useEffect(() => {
    const load = async () => {
      setLoadingSettings(true)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/settings`, { headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        if (res.ok) {
          const data = await res.json()
          const theme = data?.theme || 'light'
          const languages = Array.isArray(data?.languages) && data.languages.length > 0 ? data.languages : [data?.language || 'english']
          const soundEnabled = typeof data?.soundEnabled === 'boolean' ? data.soundEnabled : true
          setSettings((prev) => ({ ...prev, theme, languages, soundEnabled }))
          if (typeof window !== 'undefined') localStorage.setItem('languagePreferences', JSON.stringify(languages))
        }
      } catch (e: any) {
        toast.error(e?.message || 'Failed to load settings')
      } finally {
        setLoadingSettings(false)
      }
    }
    load()
  }, [])

  useEffect(() => {
    const loadProfile = async () => {
      setProfileLoading(true)
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/user/profile`, { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        const data = await res.json().catch(() => ({}))
        if (!res.ok) throw new Error(data?.error || 'Failed to load profile')
        setProfileName(data?.name || '')
        setProfileEmail(data?.email || '')
      } catch (e: any) {
        toast.error(e?.message || 'Failed to load profile')
      } finally {
        setProfileLoading(false)
      }
    }
    loadProfile()
  }, [])

  useEffect(() => {
    const loadReferralCode = async () => {
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/auth/me`, { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        const data = await res.json().catch(() => ({}))
        if (res.ok && data?.referralCode) setReferralCode(data.referralCode)
      } catch {
        // ignore -- referral box just won't render
      }
    }
    loadReferralCode()
  }, [])

  const referralLink = referralCode && typeof window !== 'undefined'
    ? `${window.location.origin}/?ref=${referralCode}`
    : null

  const copyReferralLink = async () => {
    if (!referralLink) return
    try {
      await navigator.clipboard.writeText(referralLink)
      setCopied(true)
      setTimeout(() => setCopied(false), 1800)
    } catch {
      // clipboard API unavailable -- silently ignore, link text is still selectable
    }
  }

  const handleSave = async () => {
    setSaveStatus('saving')
    try {
      const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
      const body = {
        theme: settings.theme,
        languages: settings.languages,
        soundEnabled: settings.soundEnabled,
      }
      const res = await fetch(`${base}/api/settings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify(body)
      })
      if (!res.ok) {
        const data = await res.json().catch(() => ({}))
        throw new Error(data?.error || 'Failed to save settings')
      }
      if (typeof window !== 'undefined') localStorage.setItem('languagePreferences', JSON.stringify(settings.languages))
      setSaveStatus('saved')
      toast.success('Settings saved')
      setTimeout(() => setSaveStatus('idle'), 1200)
    } catch (e: any) {
      toast.error(e?.message || 'Failed to save settings')
      setSaveStatus('error')
    }
  }

  type ToggleKey = 'emailNotifications' | 'dailyReminders' | 'weeklyReport' | 'soundEnabled';
  const handleToggle = (key: ToggleKey) => {
    setSettings((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  type SelectKey = 'theme';
  const handleChange = (key: SelectKey, value: string) => {
    setSettings((prev) => ({ ...prev, [key]: value }))
  }

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Settings</h1>
        <p className="text-muted-foreground">Customize your learning experience</p>
      </div>

      <div className="space-y-6">
        {/* Profile Settings */}
        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center gap-3 mb-6">
            <User className="text-primary" size={24} />
            <h2 className="text-xl font-bold text-foreground">Profile</h2>
          </div>
          {profileLoading ? (
            <div className="space-y-4">
              <div className="space-y-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-10 w-full" />
              </div>
              <div className="space-y-2">
                <Skeleton className="h-4 w-16" />
                <Skeleton className="h-10 w-full" />
              </div>
            </div>
          ) : (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">Full Name</label>
              <input
                type="text"
                value={profileName}
                onChange={(e) => setProfileName(e.target.value)}
                className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">Email</label>
              <input
                type="email"
                value={profileEmail}
                onChange={(e) => setProfileEmail(e.target.value)}
                className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              />
            </div>
            <Button
              className="bg-primary text-primary-foreground hover:bg-primary/90"
              disabled={profileSaving}
              onClick={async () => {
                setProfileSaving(true)
                try {
                  const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
                  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
                  const res = await fetch(`${base}/api/user/profile`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
                    body: JSON.stringify({ name: profileName, email: profileEmail })
                  })
                  const data = await res.json().catch(() => ({}))
                  if (!res.ok) throw new Error(data?.error || 'Failed to update profile')
                  if (data?.token) {
                    localStorage.setItem('token', data.token)
                  }
                  toast.success('Profile updated')
                } catch (e: any) {
                  toast.error(e?.message || 'Failed to update profile')
                } finally {
                  setProfileSaving(false)
                }
              }}
            >
              {profileSaving ? 'Updating...' : 'Update Profile'}
            </Button>
          </div>
          )}
        </Card>

        {/* Referral */}
        {referralLink && (
          <Card className="p-6 bg-card border border-border">
            <div className="flex items-center gap-3 mb-2">
              <Gift className="text-primary" size={24} />
              <h2 className="text-xl font-bold text-foreground">Refer Friends</h2>
            </div>
            <p className="text-sm text-muted-foreground mb-4">
              Share your link — when a friend you refer subscribes, you get a free month. They get an extended 7-day trial too.
            </p>
            <div className="flex gap-2">
              <input
                readOnly
                value={referralLink}
                onFocus={(e) => e.target.select()}
                className="flex-1 px-4 py-2 rounded-lg border border-input bg-muted/30 text-foreground text-sm"
              />
              <Button onClick={copyReferralLink} className="bg-primary text-primary-foreground hover:bg-primary/90 shrink-0">
                {copied ? <Check size={16} /> : <Copy size={16} />}
                <span className="ml-2">{copied ? "Copied" : "Copy"}</span>
              </Button>
            </div>
          </Card>
        )}

        {/* Notification Settings */}
        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center gap-3 mb-6">
            <Bell className="text-secondary" size={24} />
            <h2 className="text-xl font-bold text-foreground">Notifications</h2>
          </div>
          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 rounded-lg bg-muted/30">
              <div>
                <p className="font-medium text-foreground">Email Notifications</p>
                <p className="text-sm text-muted-foreground">Receive updates via email</p>
              </div>
              <input
                type="checkbox"
                checked={settings.emailNotifications}
                onChange={() => handleToggle("emailNotifications")}
                className="w-5 h-5 rounded border-2 border-primary cursor-pointer"
              />
            </div>
            <div className="flex items-center justify-between p-4 rounded-lg bg-muted/30">
              <div>
                <p className="font-medium text-foreground">Daily Reminders</p>
                <p className="text-sm text-muted-foreground">Get reminded to study daily</p>
              </div>
              <input
                type="checkbox"
                checked={settings.dailyReminders}
                onChange={() => handleToggle("dailyReminders")}
                className="w-5 h-5 rounded border-2 border-primary cursor-pointer"
              />
            </div>
            <div className="flex items-center justify-between p-4 rounded-lg bg-muted/30">
              <div>
                <p className="font-medium text-foreground">Weekly Report</p>
                <p className="text-sm text-muted-foreground">Receive weekly progress report</p>
              </div>
              <input
                type="checkbox"
                checked={settings.weeklyReport}
                onChange={() => handleToggle("weeklyReport")}
                className="w-5 h-5 rounded border-2 border-primary cursor-pointer"
              />
            </div>
          </div>
        </Card>

        {/* Appearance Settings */}
        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center gap-3 mb-6">
            <Palette className="text-accent" size={24} />
            <h2 className="text-xl font-bold text-foreground">Appearance</h2>
          </div>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">Theme</label>
              <select
                value={settings.theme}
                onChange={(e) => handleChange("theme", e.target.value)}
                className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="light">Light</option>
                <option value="dark">Dark</option>
                <option value="auto">Auto (System)</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">Language Preferences</label>
              <p className="text-xs text-muted-foreground mb-2">Videos are searched in your first preference; if none are found, the next one is tried.</p>
              <div className="flex flex-wrap gap-2 mb-3">
                {settings.languages.map((code, idx) => (
                  <div
                    key={code}
                    className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-primary/10 border border-primary/20 text-sm font-medium text-foreground"
                  >
                    <span className="text-xs text-muted-foreground">{idx + 1}.</span>
                    <span>{languageLabel(code)}</span>
                    {settings.languages.length > 1 && (
                      <button
                        onClick={() => removeLanguage(code)}
                        className="text-muted-foreground hover:text-destructive transition-colors"
                        aria-label={`Remove ${languageLabel(code)}`}
                      >
                        <X size={14} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
              {LANGUAGE_OPTIONS.some((o) => !settings.languages.includes(o.value)) && (
                <div className="flex gap-2">
                  <select
                    value={languageToAdd}
                    onChange={(e) => setLanguageToAdd(e.target.value)}
                    className="flex-1 px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  >
                    {LANGUAGE_OPTIONS.filter((o) => !settings.languages.includes(o.value)).map((o) => (
                      <option key={o.value} value={o.value}>{o.label}</option>
                    ))}
                  </select>
                  <Button onClick={addLanguage} className="bg-primary text-primary-foreground hover:bg-primary/90">
                    Add
                  </Button>
                </div>
              )}
            </div>
            <div className="flex items-center justify-between p-4 rounded-lg bg-muted/30">
              <div>
                <p className="font-medium text-foreground">Sound Effects</p>
                <p className="text-sm text-muted-foreground">Play sounds in the app</p>
              </div>
              <input
                type="checkbox"
                checked={settings.soundEnabled}
                onChange={() => setSettings((prev) => ({ ...prev, soundEnabled: !prev.soundEnabled }))}
                className="w-5 h-5 rounded border-2 border-primary cursor-pointer"
              />
            </div>
          </div>
        </Card>

        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-bold text-foreground">Save</h2>
              <p className="text-sm text-muted-foreground">Persist your preferences</p>
            </div>
            <Button className="bg-primary text-primary-foreground hover:bg-primary/90" onClick={handleSave} disabled={saveStatus==='saving'}>
              {saveStatus==='saving' ? 'Saving...' : 'Save Settings'}
            </Button>
          </div>
        </Card>

        {/* Security Settings */}
        <Card className="p-6 bg-card border border-border">
          <div className="flex items-center gap-3 mb-6">
            <Lock className="text-destructive" size={24} />
            <h2 className="text-xl font-bold text-foreground">Security</h2>
          </div>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">Current Password</label>
              <input
                type="password"
                value={pwdCurrent}
                onChange={(e) => setPwdCurrent(e.target.value)}
                className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="••••••••"
              />
            </div>
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">New Password</label>
              <input
                type="password"
                value={pwdNew}
                onChange={(e) => setPwdNew(e.target.value)}
                className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="••••••••"
              />
            </div>
            <Button
              className="w-full bg-secondary text-secondary-foreground hover:bg-secondary/90"
              disabled={pwdStatus === 'saving'}
              onClick={async () => {
                setPwdStatus('saving')
                try {
                  const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
                  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
                  const res = await fetch(`${base}/api/auth/change-password`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
                    body: JSON.stringify({ currentPassword: pwdCurrent, newPassword: pwdNew })
                  })
                  const data = await res.json().catch(() => ({}))
                  if (!res.ok) throw new Error(data?.error || 'Failed to change password')
                  if (data?.token) localStorage.setItem('token', data.token)
                  setPwdStatus('saved')
                  toast.success('Password changed')
                  setPwdCurrent("")
                  setPwdNew("")
                } catch (e: any) {
                  setPwdStatus('error')
                  toast.error(e?.message || 'Failed to change password')
                } finally {
                  setTimeout(() => setPwdStatus('idle'), 1500)
                }
              }}
            >
              {pwdStatus === 'saving' ? 'Changing...' : 'Change Password'}
            </Button>

            <Button
              className="w-full bg-muted text-foreground hover:bg-muted/80"
              onClick={async () => {
                try {
                  const base = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080'
                  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
                  const res = await fetch(`${base}/api/auth/logout-all`, { method: 'POST', headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
                  const data = await res.json().catch(() => ({}))
                  if (!res.ok) throw new Error(data?.error || 'Failed to sign out all devices')
                  if (data?.token) localStorage.setItem('token', data.token)
                  toast.success('All devices signed out (current session refreshed)')
                } catch (e: any) {
                  toast.error(e?.message || 'Failed to sign out all devices')
                }
              }}
            >
              Sign Out All Devices
            </Button>

            <Button
              className="w-full bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => {
                if (typeof window !== 'undefined') {
                  localStorage.removeItem('token')
                  window.location.reload()
                }
              }}
            >
              Sign Out
            </Button>
          </div>
        </Card>

        {/* Danger Zone */}
        <Card className="p-6 bg-destructive/10 border border-destructive/20">
          <h2 className="text-xl font-bold text-destructive mb-4">Danger Zone</h2>
          <p className="text-sm text-muted-foreground mb-4">
            These actions cannot be undone. Please proceed with caution.
          </p>
          <Button className="w-full bg-destructive text-destructive-foreground hover:bg-destructive/90">
            Delete Account
          </Button>
        </Card>
      </div>
    </div>
  )
}
