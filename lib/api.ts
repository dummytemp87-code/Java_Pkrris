const RETRY_ATTEMPTS = 2 // total attempts = this + 1
const RETRY_BASE_DELAY_MS = 700

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Fetch wrapper that attaches the auth token and, on a 401/403 (Spring
 * Security returns 403 for any missing/expired JWT in this app, since
 * there's no custom entry point), clears the dead token and reloads --
 * without this, an expired token just produces silent empty states
 * forever with no way back to login.
 *
 * Also retries transient failures (network errors, 5xx responses) with a
 * short backoff before giving up, so a flaky connection or a momentary
 * upstream hiccup doesn't surface as a visible error. 4xx responses are
 * never retried -- they won't succeed on a second attempt, and retrying
 * would just delay a legitimate error.
 */
export async function apiFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
  const headers = {
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }

  let lastError: unknown
  for (let attempt = 0; attempt <= RETRY_ATTEMPTS; attempt++) {
    try {
      const res = await fetch(url, { ...options, headers })
      if ((res.status === 401 || res.status === 403) && typeof window !== 'undefined' && token) {
        localStorage.removeItem('token')
        window.location.reload()
        return res
      }
      if (res.status >= 500 && attempt < RETRY_ATTEMPTS) {
        await sleep(RETRY_BASE_DELAY_MS * (attempt + 1))
        continue
      }
      return res
    } catch (err) {
      lastError = err
      if (attempt < RETRY_ATTEMPTS) {
        await sleep(RETRY_BASE_DELAY_MS * (attempt + 1))
        continue
      }
    }
  }
  throw lastError
}
