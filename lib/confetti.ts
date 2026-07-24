import confetti from "canvas-confetti"

/** A festive burst for big wins: goal completed, high quiz score. */
export function celebrateBig() {
  const duration = 1200
  const end = Date.now() + duration
  const colors = ["#4f46e5", "#f59e0b", "#10b981", "#ec4899"]

  ;(function frame() {
    confetti({
      particleCount: 3,
      angle: 60,
      spread: 60,
      origin: { x: 0, y: 0.7 },
      colors,
    })
    confetti({
      particleCount: 3,
      angle: 120,
      spread: 60,
      origin: { x: 1, y: 0.7 },
      colors,
    })
    if (Date.now() < end) requestAnimationFrame(frame)
  })()
}

/** A smaller, quieter burst for modest wins: passing a quiz, a completed module. */
export function celebrateSmall() {
  confetti({
    particleCount: 40,
    spread: 55,
    origin: { y: 0.7 },
    colors: ["#4f46e5", "#10b981"],
  })
}
