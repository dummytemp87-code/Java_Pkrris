import Link from "next/link"

export const metadata = { title: "Privacy Policy — StudyHub" }

export default function PrivacyPage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-3xl mx-auto px-6 py-12 md:py-16">
        <Link href="/" className="text-sm text-primary hover:underline">← Back to StudyHub</Link>
        <h1 className="text-3xl font-bold text-foreground mt-6 mb-2">Privacy Policy</h1>
        <p className="text-sm text-muted-foreground mb-10">Last updated: July 2026</p>

        <div className="space-y-8 text-foreground">
          <section>
            <h2 className="text-lg font-semibold mb-2">What we collect</h2>
            <ul className="list-disc pl-5 text-muted-foreground leading-relaxed space-y-1">
              <li><span className="text-foreground font-medium">Account info</span>: name, email, and a hashed password.</li>
              <li><span className="text-foreground font-medium">Study content</span>: your goals, uploaded syllabuses (PDF/DOCX/image), chat messages with the AI tutor, and quiz results.</li>
              <li><span className="text-foreground font-medium">Usage data</span>: which features you use, so we can show accurate progress/streak stats.</li>
              <li><span className="text-foreground font-medium">Payment info</span>: if you subscribe, Razorpay processes your payment directly — we never see or store your card details, only your subscription status.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">How we use it</h2>
            <p className="text-muted-foreground leading-relaxed">
              To run the product: generating your study plans and tutor responses, tracking your progress, and
              managing your subscription. We don't sell your data, and we don't use your study content to train
              AI models.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">Third parties we share data with</h2>
            <ul className="list-disc pl-5 text-muted-foreground leading-relaxed space-y-1">
              <li><span className="text-foreground font-medium">Google Gemini</span> — receives the text/images you submit (goals, syllabuses, chat messages) to generate study plans, tutor replies, and quizzes.</li>
              <li><span className="text-foreground font-medium">YouTube Data API</span> — used to find relevant lesson videos for your study plan; no personal data is sent beyond the search query.</li>
              <li><span className="text-foreground font-medium">Razorpay</span> — handles subscription payments; receives what's needed to process a transaction (per their own privacy policy).</li>
              <li><span className="text-foreground font-medium">Supabase</span> — hosts our database (all the data above), located per Supabase's infrastructure.</li>
            </ul>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">Your controls</h2>
            <p className="text-muted-foreground leading-relaxed">
              You can update your profile, sign out of all devices, or delete your account from Settings.
              Deleting your account removes your goals, study plans, quiz history, and chat history from our
              database.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">Security</h2>
            <p className="text-muted-foreground leading-relaxed">
              Passwords are hashed (never stored in plain text), all traffic is encrypted (HTTPS), and access to
              AI features requires authentication. No system is perfectly secure, but we don't store payment
              card details ourselves — that's handled entirely by Razorpay.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">Contact</h2>
            <p className="text-muted-foreground leading-relaxed">
              Questions about your data: support@studyhub.app
            </p>
          </section>
        </div>
      </div>
    </div>
  )
}
