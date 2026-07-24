"use client"

import { useEffect, useRef, useState } from "react"
import { useTheme } from "next-themes"
import { toast } from "sonner"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Bell, Lock, User, Palette, X, Gift, Copy, Check, Mail } from "lucide-react"

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

  const { theme: activeTheme, setTheme: setActiveTheme } = useTheme()

  const [settings, setSettings] = useState<SettingsState>({
    emailNotifications: true,
    dailyReminders: true,
    weeklyReport: false,
    soundEnabled: true,
    theme: "system",
    languages: ["english"],
  })

  // The live theme (from next-themes, already correctly restored from its own
  // localStorage before this component even mounts) is the one source of
  // truth for what's on screen. Mirror it into settings.theme one-way, purely
  // so the dropdown displays it and the autosave payload includes it --
  // settings.theme must never be pushed back onto setActiveTheme, or loading
  // a stale/default value from the backend would visibly flip the user's
  // current theme out from under them the moment they open this page.
  useEffect(() => {
    if (!activeTheme) return
    setSettings((prev) => (prev.theme === activeTheme ? prev : { ...prev, theme: activeTheme }))
  }, [activeTheme])
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
  const [settingsLoaded, setSettingsLoaded] = useState(false)
  const [autoSaveStatus, setAutoSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle')

  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileName, setProfileName] = useState("")
  const [profileEmail, setProfileEmail] = useState("")
  const [referralCode, setReferralCode] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const [emailVerified, setEmailVerified] = useState<boolean | null>(null)
  const [otpSent, setOtpSent] = useState(false)
  const [otpCode, setOtpCode] = useState("")
  const [otpStatus, setOtpStatus] = useState<'idle' | 'sending' | 'verifying'>('idle')

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
          // theme is deliberately NOT read here -- the mirror effect above
          // owns it from the live next-themes state, never from this fetch.
          const languages = Array.isArray(data?.languages) && data.languages.length > 0 ? data.languages : [data?.language || 'english']
          const soundEnabled = typeof data?.soundEnabled === 'boolean' ? data.soundEnabled : true
          const emailNotifications = typeof data?.emailNotifications === 'boolean' ? data.emailNotifications : true
          const dailyReminders = typeof data?.dailyReminders === 'boolean' ? data.dailyReminders : true
          const weeklyReport = typeof data?.weeklyReport === 'boolean' ? data.weeklyReport : false
          setSettings((prev) => ({ ...prev, languages, soundEnabled, emailNotifications, dailyReminders, weeklyReport }))
          if (typeof window !== 'undefined') localStorage.setItem('languagePreferences', JSON.stringify(languages))
        }
      } catch (e: any) {
        toast.error(e?.message || 'Failed to load settings')
      } finally {
        setLoadingSettings(false)
        setSettingsLoaded(true)
      }
    }
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    const loadMe = async () => {
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const res = await fetch(`${base}/api/auth/me`, { headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) } })
        const data = await res.json().catch(() => ({}))
        if (res.ok) {
          if (data?.referralCode) setReferralCode(data.referralCode)
          setEmailVerified(!!data?.emailVerified)
        }
      } catch {
        // ignore -- referral box just won't render, verification card shows loading state
      }
    }
    loadMe()
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

  // Auto-save: any change to settings persists on its own, debounced, once the
  // initial GET has completed (so we never overwrite the saved preferences
  // with the in-component defaults before they're loaded).
  const saveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    if (!settingsLoaded) return
    if (saveTimer.current) clearTimeout(saveTimer.current)
    saveTimer.current = setTimeout(async () => {
      setAutoSaveStatus('saving')
      try {
        const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
        const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
        const body = {
          theme: settings.theme,
          languages: settings.languages,
          soundEnabled: settings.soundEnabled,
          emailNotifications: settings.emailNotifications,
          dailyReminders: settings.dailyReminders,
          weeklyReport: settings.weeklyReport,
        }
        const res = await fetch(`${base}/api/settings`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
          body: JSON.stringify(body)
        })
        if (!res.ok) throw new Error('Failed to save settings')
        setAutoSaveStatus('saved')
        setTimeout(() => setAutoSaveStatus((s) => (s === 'saved' ? 'idle' : s)), 1500)
      } catch (e: any) {
        setAutoSaveStatus('error')
        toast.error(e?.message || 'Failed to save settings')
      }
    }, 600)
    return () => { if (saveTimer.current) clearTimeout(saveTimer.current) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settings, settingsLoaded])

  type ToggleKey = 'emailNotifications' | 'dailyReminders' | 'weeklyReport' | 'soundEnabled';
  const handleToggle = (key: ToggleKey) => {
    setSettings((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const handleThemeChange = (value: string) => {
    // Applies immediately; the mirror effect above then copies it into
    // settings.theme, which is what actually triggers the debounced save.
    setActiveTheme(value)
  }

  const sendVerificationOtp = async () => {
    setOtpStatus('sending')
    try {
      const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
      const res = await fetch(`${base}/api/auth/send-verification-otp`, {
        method: 'POST',
        headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(data?.error || 'Failed to send verification code')
      setOtpSent(true)
      toast.success('Verification code sent -- check your email')
    } catch (e: any) {
      toast.error(e?.message || 'Failed to send verification code')
    } finally {
      setOtpStatus('idle')
    }
  }

  const verifyEmailOtp = async () => {
    if (otpCode.trim().length !== 6) return
    setOtpStatus('verifying')
    try {
      const base = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080"
      const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
      const res = await fetch(`${base}/api/auth/verify-email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: JSON.stringify({ otp: otpCode.trim() }),
      })
      const data = await res.json().catch(() => ({}))
      if (!res.ok) throw new Error(data?.error || 'Invalid code')
      setEmailVerified(true)
      setOtpSent(false)
      setOtpCode("")
      toast.success('Email verified')
    } catch (e: any) {
      toast.error(e?.message || 'Invalid code')
    } finally {
      setOtpStatus('idle')
    }
  }

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto">
      <div className="mb-8 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-foreground mb-2">Settings</h1>
          <p className="text-muted-foreground">Customize your learning experience</p>
        </div>
        {settingsLoaded && (
          <p className="text-xs text-muted-foreground shrink-0 pt-2">
            {autoSaveStatus === 'saving' ? 'Saving...' : autoSaveStatus === 'saved' ? 'All changes saved' : autoSaveStatus === 'error' ? 'Failed to save' : ''}
          </p>
        )}
      </div>

      <div className="space-y-6">
        {/* Profile Settings */}
        <Card className="p-6">
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

        {/* Email Verification */}
        <Card className="p-6">
          <div className="flex items-center gap-3 mb-4">
            <Mail className="text-primary" size={24} />
            <h2 className="text-xl font-bold text-foreground">Email Verification</h2>
          </div>
          {emailVerified === null ? (
            <Skeleton className="h-10 w-full" />
          ) : emailVerified ? (
            <p className="text-sm text-foreground flex items-center gap-2">
              <span className="flex items-center justify-center w-5 h-5 rounded-full bg-primary/15 text-primary"><Check size={12} /></span>
              Your email is verified.
            </p>
          ) : (
            <div className="space-y-3">
              <p className="text-sm text-muted-foreground">
                Verify {profileEmail || "your email"} so you can receive your daily progress update and other account emails.
              </p>
              {!otpSent ? (
                <Button onClick={sendVerificationOtp} disabled={otpStatus === 'sending'}>
                  {otpStatus === 'sending' ? 'Sending...' : 'Send Verification Code'}
                </Button>
              ) : (
                <div className="flex flex-wrap gap-2">
                  <input
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    value={otpCode}
                    onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                    placeholder="6-digit code"
                    className="w-40 px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary tracking-widest"
                  />
                  <Button onClick={verifyEmailOtp} disabled={otpStatus === 'verifying' || otpCode.length !== 6}>
                    {otpStatus === 'verifying' ? 'Verifying...' : 'Verify'}
                  </Button>
                  <Button variant="secondary" onClick={sendVerificationOtp} disabled={otpStatus === 'sending'}>
                    Resend
                  </Button>
                </div>
              )}
            </div>
          )}
        </Card>

        {/* Referral */}
        {referralLink && (
          <Card className="p-6">
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
              <Button onClick={copyReferralLink} className="shrink-0">
                {copied ? <Check size={16} /> : <Copy size={16} />}
                <span className="ml-2">{copied ? "Copied" : "Copy"}</span>
              </Button>
            </div>
          </Card>
        )}

        {/* Notification Settings */}
        <Card className="p-6">
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
                <p className="text-sm text-muted-foreground">Get a daily email with your progress and what's next</p>
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
        <Card className="p-6">
          <div className="flex items-center gap-3 mb-6">
            <Palette className="text-accent" size={24} />
            <h2 className="text-xl font-bold text-foreground">Appearance</h2>
          </div>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-semibold text-foreground mb-2">Theme</label>
              <select
                value={settings.theme}
                onChange={(e) => handleThemeChange(e.target.value)}
                className="w-full px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
              >
                <option value="light">Light</option>
                <option value="dark">Dark</option>
                <option value="system">Auto (System)</option>
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
                  <Button onClick={addLanguage}>
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

        {/* Security Settings */}
        <Card className="p-6">
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
              variant="secondary"
              className="w-full"
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
      </div>
    </div>
  )
}
