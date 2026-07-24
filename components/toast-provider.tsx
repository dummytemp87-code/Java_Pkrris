"use client"

import { useTheme } from "next-themes"
import { Toaster as Sonner } from "sonner"

export function ToastProvider() {
  const { theme } = useTheme()
  return <Sonner theme={theme === "dark" ? "dark" : "light"} richColors position="top-right" closeButton />
}
