/**
 * Fetch wrapper that attaches the auth token and, on a 401/403 (Spring
 * Security returns 403 for any missing/expired JWT in this app, since
 * there's no custom entry point), clears the dead token and reloads --
 * without this, an expired token just produces silent empty states
 * forever with no way back to login.
 */
export async function apiFetch(url: string, options: RequestInit = {}): Promise<Response> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null
  const res = await fetch(url, {
    ...options,
    headers: {
      ...(options.headers || {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })
  if ((res.status === 401 || res.status === 403) && typeof window !== 'undefined' && token) {
    localStorage.removeItem('token')
    window.location.reload()
  }
  return res
}
