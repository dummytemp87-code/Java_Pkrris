import Link from "next/link"

export const metadata = { title: "Refund Policy — StudyHub" }

export default function RefundPage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-3xl mx-auto px-6 py-12 md:py-16">
        <Link href="/" className="text-sm text-primary hover:underline">← Back to StudyHub</Link>
        <h1 className="text-3xl font-bold text-foreground mt-6 mb-2">Refund Policy</h1>
        <p className="text-sm text-muted-foreground mb-10">Last updated: July 2026</p>

        <div className="space-y-8 text-foreground">
          <section>
            <h2 className="text-lg font-semibold mb-2">Free trial</h2>
            <p className="text-muted-foreground leading-relaxed">
              Every new account gets a 3-day free trial (7 days if you signed up via a referral link) with full
              access to all features. You are never charged during the trial — no card is required to start
              one. You only pay if you actively choose a paid plan.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">Cancelling a subscription</h2>
            <p className="text-muted-foreground leading-relaxed">
              You can cancel your subscription anytime from the Billing page. Cancellation stops future billing
              — you keep full access until the end of the period you already paid for, and you won't be charged
              again after that.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">Refunds</h2>
            <p className="text-muted-foreground leading-relaxed">
              Because every plan starts with a full-featured free trial, we generally don't offer refunds for
              partial or unused billing periods once a charge has gone through — the trial is the intended way
              to evaluate the product before paying. If you were charged in error (e.g. a duplicate charge, or a
              charge after you'd already cancelled), contact us and we'll make it right.
            </p>
          </section>

          <section>
            <h2 className="text-lg font-semibold mb-2">How to request one</h2>
            <p className="text-muted-foreground leading-relaxed">
              Email support@studyhub.app with your account email and the issue. We aim to respond within 2
              business days. Approved refunds are issued back to your original Razorpay payment method.
            </p>
          </section>
        </div>
      </div>
    </div>
  )
}
