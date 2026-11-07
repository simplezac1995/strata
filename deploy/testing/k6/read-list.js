// Read-heavy baseline: authenticate once, then page through the user list.
import http from 'k6/http'
import { check, sleep } from 'k6'
import { Rate } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin'
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin123'

export const errors = new Rate('errors')

export const options = {
  scenarios: {
    read_list: { executor: 'constant-vus', vus: Number(__ENV.VUS || 20), duration: __ENV.DURATION || '2m' },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<300'],
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
  const response = http.get(`${BASE_URL}/api/v1/system/users?page=1&size=20`, {
    headers: { Authorization: `Bearer ${data.token}` },
  })
  const ok = check(response, { 'list 200': (r) => r.status === 200 })
  errors.add(!ok)
  sleep(0.2)
}
