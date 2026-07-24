import type React from "react"
import type { Metadata } from "next"
import { Geist, Geist_Mono } from "next/font/google"
import { Analytics } from "@vercel/analytics/next"
import { ThemeProvider } from "@/components/theme-provider"
import { ToastProvider } from "@/components/toast-provider"
import "./globals.css"

const _geist = Geist({ subsets: ["latin"] })
const _geistMono = Geist_Mono({ subsets: ["latin"] })

export const metadata: Metadata = {
  title: "StudyHub — AI-Powered Study Assistant",
  description: "Your personalized learning companion",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`font-sans antialiased`}>
        <ThemeProvider attribute="class" defaultTheme="system" enableSystem={true}>
          <div aria-hidden className="fixed inset-0 -z-10 overflow-hidden bg-background">
            <div
              className="bg-blob bg-primary"
              style={{ width: '38rem', height: '38rem', top: '-10%', left: '-8%', animationDuration: '24s' }}
            />
            <div
              className="bg-blob bg-secondary"
              style={{ width: '32rem', height: '32rem', top: '20%', right: '-10%', animationDuration: '28s', animationDelay: '-6s' }}
            />
            <div
              className="bg-blob bg-accent"
              style={{ width: '34rem', height: '34rem', bottom: '-15%', left: '20%', animationDuration: '26s', animationDelay: '-12s' }}
            />
          </div>
          {children}
          <ToastProvider />
        </ThemeProvider>
        <Analytics />
      </body>
    </html>
  )
}
