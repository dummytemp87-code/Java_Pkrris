"use client";

import { useEffect, useState } from "react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

interface AuthProps {
  onAuthenticated: (auth: { token: string; name: string; email: string; role: string }) => void;
}

export default function Auth({ onAuthenticated }: AuthProps) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
  }, [mode]);

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
          mode === "login" ? { email, password } : { name, email, password }
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
        onAuthenticated({ token, name: data?.name, email: data?.email, role: data?.role });
      } else {
        setError("No token returned by server");
      }
    } catch (e: any) {
      setError(e?.message || "Request failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full h-screen flex items-center justify-center p-6">
      <Card className="w-full max-w-md p-6 bg-card border border-border">
        <h1 className="text-2xl font-bold mb-4 text-foreground">
          {mode === "login" ? "Login" : "Create an account"}
        </h1>
        {mode === "register" && (
          <div className="mb-3">
            <label className="block text-sm text-muted-foreground mb-1">Name</label>
            <input
              className="w-full px-3 py-2 rounded-md border border-input bg-background text-foreground"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your name"
            />
          </div>
        )}
        <div className="mb-3">
          <label className="block text-sm text-muted-foreground mb-1">Email</label>
          <input
            className="w-full px-3 py-2 rounded-md border border-input bg-background text-foreground"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
          />
        </div>
        <div className="mb-4">
          <label className="block text-sm text-muted-foreground mb-1">Password</label>
          <input
            type="password"
            className="w-full px-3 py-2 rounded-md border border-input bg-background text-foreground"
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
    </div>
  );
}
