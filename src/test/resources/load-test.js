import http from 'k6/http';
import { check, group, sleep } from 'k6';

// Configuration — 10 virtual users, 30 seconds
export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'],  // p95 response time < 500ms
        http_req_failed: ['rate<0.05'],     // error rate < 5%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

// Unique user per VU iteration
let counter = 0;

export default function () {
    const uniqueId = `${__VU}_${__ITER}_${Date.now()}`;

    group('Public Pages', function () {
        // GET /login — render login form
        let loginPage = http.get(`${BASE_URL}/login`);
        check(loginPage, {
            'login page returns 200': (r) => r.status === 200,
        });

        // GET /register — render registration form
        let registerPage = http.get(`${BASE_URL}/register`);
        check(registerPage, {
            'register page returns 200': (r) => r.status === 200,
        });

        // GET /actuator/health — health check
        let health = http.get(`${BASE_URL}/actuator/health`);
        check(health, {
            'health endpoint returns 200': (r) => r.status === 200,
        });
    });

    group('User Registration', function () {
        // POST /register — create new user
        let registerRes = http.post(`${BASE_URL}/register`, {
            username: `loadtest_${uniqueId}`,
            password: 'TestPass123!',
        });
        check(registerRes, {
            'registration succeeds (200 or 302)': (r) => r.status === 200 || r.status === 302,
        });
    });

    group('Authenticated Flow', function () {
        // POST /login — authenticate
        let loginRes = http.post(`${BASE_URL}/login`, {
            username: `loadtest_${uniqueId}`,
            password: 'TestPass123!',
        });
        check(loginRes, {
            'login succeeds (200 or 302)': (r) => r.status === 200 || r.status === 302,
        });

        // GET /dashboard — view account dashboard
        let dashboard = http.get(`${BASE_URL}/dashboard`);
        check(dashboard, {
            'dashboard accessible': (r) => r.status === 200 || r.status === 302,
        });

        // POST /deposit — deposit funds
        let depositRes = http.post(`${BASE_URL}/deposit`, {
            amount: '100.00',
        });
        check(depositRes, {
            'deposit succeeds': (r) => r.status === 200 || r.status === 302,
        });

        // POST /withdraw — withdraw funds
        let withdrawRes = http.post(`${BASE_URL}/withdraw`, {
            amount: '25.00',
        });
        check(withdrawRes, {
            'withdraw succeeds': (r) => r.status === 200 || r.status === 302,
        });

        // GET /transactions — transaction history
        let txns = http.get(`${BASE_URL}/transactions`);
        check(txns, {
            'transactions page accessible': (r) => r.status === 200 || r.status === 302,
        });
    });

    sleep(1);
}
