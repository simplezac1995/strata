// Mixed workload: list users, read roles, read operation logs. Adjust paths to your enabled modules.
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Counter, Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin123'

export const errors = new Rate('errors')
export const requests = new Counter('business_requests')

export const options = {
  scenarios: {
    mixed: { executor: 'constant-vus', vus: Number(__ENV.VUS || 10), duration: __ENV.DURATION || '2m' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    errors: ['rate<0.01'],
  },
}

export function setup() {
  const response = http.post(`${BASE_URL}/api/v1/system/auth/login`, JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
  })
  check(response, { 'login 200': (r) => r.status === 200 })
  return { token: response.json('data.accessToken') }
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` }
  const paths = [
    '/api/v1/system/users?page=1&size=20',
    '/api/v1/system/roles?page=1&size=20',
    '/api/v1/system/operation-logs?page=1&size=20',
  ]
  const path = paths[Math.floor(Math.random() * paths.length)]
  const response = http.get(`${BASE_URL}${path}`, { headers })
  const ok = check(response, { 'request 200': (r) => r.status === 200 })
  errors.add(!ok)
  requests.add(1)
  sleep(0.3)
}
