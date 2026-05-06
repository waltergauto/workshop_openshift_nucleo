import type { APIRoute } from 'astro'

export const GET: APIRoute = ({ request }) => {
  const url = new URL(request.url)
  const checkType = url.searchParams.get('type') || 'liveness'

  const healthStatus = {
    status: 'UP',
    service: 'frontend-astro',
    check: checkType,
    timestamp: new Date().toISOString()
  }

  return new Response(JSON.stringify(healthStatus), {
    status: 200,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}
