"use client";

import { useEffect, useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

interface AuthProps {
  onAuthenticated: (auth: { token: string; name: string; email: string; role: string }) => void;
  onBack?: () => void;
}

type PendingAuth = { token: string; name: string; email: string; role: string };

export default function Auth({ onAuthenticated, onBack }: AuthProps) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [referralCode, setReferralCode] = useState<string | null>(null);

  // After a fresh registration, hold off on onAuthenticated() and show an
  // inline OTP step instead -- soft-gated, "Skip for now" proceeds exactly
  // like verification never happened (the account works either way, this
  // just gives new users the chance to verify right away instead of having
  // to find it later in Settings).
  const [step, setStep] = useState<"form" | "verify">("form");
  const [pendingAuth, setPendingAuth] = useState<PendingAuth | null>(null);
  const [otpCode, setOtpCode] = useState("");
  const [otpStatus, setOtpStatus] = useState<"idle" | "sending" | "verifying">("idle");
  const [otpError, setOtpError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
  }, [mode]);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const ref = new URLSearchParams(window.location.search).get('ref');
    if (ref) {
      setReferralCode(ref.toUpperCase());
      setMode('register');
    }
  }, []);

  const submit = async () => {
    if (!email || !password || (mode === "register" && !name)) return;
    setLoading(true);
    setError(null);
    try {
      const endpointBase = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
      const url = mode === "login" ? `${endpointBase}/api/auth/login` : `${endpointBase}/api/auth/register`;
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(
          mode === "login"
            ? { email, password }
            : { name, email, password, ...(referralCode ? { referralCode } : {}) }
        ),
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data?.error || "Authentication failed");
        return;
      }
      const token = data?.token as string;
      if (token) {
        localStorage.setItem("token", token);
        const auth: PendingAuth = { token, name: data?.name, email: data?.email, role: data?.role };
        if (mode === "register") {
          setPendingAuth(auth);
          setStep("verify");
          sendOtp(token);
        } else {
          onAuthenticated(auth);
        }
      } else {
        setError("No token returned by server");
      }
    } catch (e: any) {
      setError(e?.message || "Request failed");
    } finally {
      setLoading(false);
    }
  };

  const sendOtp = async (token: string) => {
    setOtpStatus("sending");
    setOtpError(null);
    try {
      const endpointBase = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
      const res = await fetch(`${endpointBase}/api/auth/send-verification-otp`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data?.error || "Failed to send verification code");
    } catch (e: any) {
      setOtpError(e?.message || "Failed to send verification code");
    } finally {
      setOtpStatus("idle");
    }
  };

  const verifyOtp = async () => {
    if (!pendingAuth || otpCode.trim().length !== 6) return;
    setOtpStatus("verifying");
    setOtpError(null);
    try {
      const endpointBase = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
      const res = await fetch(`${endpointBase}/api/auth/verify-email`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${pendingAuth.token}` },
        body: JSON.stringify({ otp: otpCode.trim() }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data?.error || "Invalid code");
      onAuthenticated(pendingAuth);
    } catch (e: any) {
      setOtpError(e?.message || "Invalid code");
    } finally {
      setOtpStatus("idle");
    }
  };

  return (
    <div className="w-full h-screen flex flex-col items-center justify-center p-6">
      {onBack ? (
        <button
          onClick={onBack}
          className="mb-6 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          ← Back to StudyHub
        </button>
      ) : null}
      {step === "verify" && pendingAuth ? (
        <Card className="w-full max-w-md p-6 glass-strong">
          <h1 className="text-xl font-bold mb-1 bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">StudyHub</h1>
          <p className="text-sm text-muted-foreground mb-4">Verify your email</p>
          <p className="text-sm text-muted-foreground mb-4">
            We sent a 6-digit code to <span className="text-foreground">{pendingAuth.email}</span>. Enter it below, or skip and verify later from Settings.
          </p>
          <div className="mb-4">
            <input
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
              placeholder="6-digit code"
              className="w-full px-3 py-2 rounded-md border border-white/20 dark:border-white/10 bg-white/5 backdrop-blur-sm text-foreground text-center text-lg tracking-widest focus:outline-none focus:ring-2 focus:ring-primary/50"
            />
          </div>
          {otpError ? (
            <p className="text-sm text-red-600 mb-3">{otpError}</p>
          ) : null}
          <Button disabled={otpStatus === "verifying" || otpCode.length !== 6} onClick={verifyOtp} className="w-full mb-2">
            {otpStatus === "verifying" ? "Verifying..." : "Verify Email"}
          </Button>
          <Button variant="secondary" disabled={otpStatus === "sending"} onClick={() => sendOtp(pendingAuth.token)} className="w-full mb-3">
            {otpStatus === "sending" ? "Sending..." : "Resend Code"}
          </Button>
          <button
            className="text-sm text-muted-foreground hover:text-foreground hover:underline w-full text-center"
            onClick={() => onAuthenticated(pendingAuth)}
          >
            Skip for now
          </button>
        </Card>
      ) : (
      <Card className="w-full max-w-md p-6 glass-strong">
        <h1 className="text-xl font-bold mb-1 bg-gradient-to-r from-primary to-secondary bg-clip-text text-transparent">StudyHub</h1>
        <p className="text-sm text-muted-foreground mb-4">
          {mode === "login" ? "Welcome back" : "Create your account"}
        </p>
        {mode === "register" && referralCode ? (
          <p className="text-xs text-primary bg-primary/10 border border-primary/20 rounded-md px-3 py-2 mb-3">
            Referred by a friend — you'll get an extended 7-day trial.
          </p>
        ) : mode === "register" ? (
          <p className="text-xs text-muted-foreground bg-secondary/10 border border-secondary/20 rounded-md px-3 py-2 mb-3">
            Free 3-day trial — no card required.
          </p>
        ) : null}
        {mode === "register" && (
          <div className="mb-3">
            <label className="block text-sm text-muted-foreground mb-1">Name</label>
            <input
              className="w-full px-3 py-2 rounded-md border border-white/20 dark:border-white/10 bg-white/5 backdrop-blur-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your name"
            />
          </div>
        )}
        <div className="mb-3">
          <label className="block text-sm text-muted-foreground mb-1">Email</label>
          <input
            className="w-full px-3 py-2 rounded-md border border-white/20 dark:border-white/10 bg-white/5 backdrop-blur-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </div>
        <div className="mb-4">
          <label className="block text-sm text-muted-foreground mb-1">Password</label>
          <input
            type="password"
            className="w-full px-3 py-2 rounded-md border border-white/20 dark:border-white/10 bg-white/5 backdrop-blur-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/50"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
          />
        </div>
        {error ? (
          <p className="text-sm text-red-600 mb-3">{error}</p>
        ) : null}
        <Button disabled={loading} onClick={submit} className="w-full mb-3">
          {loading ? "Please wait..." : mode === "login" ? "Login" : "Register"}
        </Button>
        <button
          className="text-sm text-primary hover:underline"
          onClick={() => setMode(mode === "login" ? "register" : "login")}
        >
          {mode === "login" ? "Create an account" : "Have an account? Login"}
        </button>
      </Card>
      )}
    </div>
  );
}
