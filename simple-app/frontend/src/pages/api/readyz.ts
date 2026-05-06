import type { APIRoute } from 'astro'

export const GET: APIRoute = async ({ request }) => {
  try {
    const backendUrl = 'http://localhost:8080'
    const response = await fetch(`${backendUrl}/q/health/ready`)

    const healthStatus = {
      status: response.ok ? 'UP' : 'DOWN',
      service: 'frontend-astro',
      check: 'readiness',
      dependencies: {
        backend: response.ok ? 'UP' : 'DOWN'
      },
      timestamp: new Date().toISOString()
    }

    return new Response(JSON.stringify(healthStatus), {
      status: response.ok ? 200 : 503,
      headers: {
        'Content-Type': 'application/json'
      }
    })
  } catch (error) {
    const healthStatus = {
      status: 'DOWN',
      service: 'frontend-astro',
      check: 'readiness',
      dependencies: {
        backend: 'DOWN'
      },
      error: 'Backend not reachable',
      timestamp: new Date().toISOString()
    }

    return new Response(JSON.stringify(healthStatus), {
      status: 503,
      headers: {
        'Content-Type': 'application/json'
      }
    })
  }
}
