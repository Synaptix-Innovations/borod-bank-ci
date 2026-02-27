import http from 'k6/http';
import { check, group, sleep } from 'k6';

// Configuration — 10 virtual users, 30 seconds
export const options = {
    vus: 10,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<2000'],  // p95 response time < 2000ms
        http_req_failed: ['rate<0.05'],     // error rate < 5%
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

// Extract CSRF token from HTML response
function getCsrf(response) {
    const match = response.body.match(/name="_csrf"\s+value="([^"]+)"/);
    return match ? match[1] : '';
}

export default function () {
    const uniqueId = `${__VU}_${__ITER}_${Date.now()}`;
    const username = `k6_${uniqueId}`;
    const password = 'TestPass123!';

    group('Public Pages', function () {
        let health = http.get(`${BASE_URL}/actuator/health`);
        check(health, {
            'health endpoint returns 200': (r) => r.status === 200,
        });

        let loginPage = http.get(`${BASE_URL}/login`);
        check(loginPage, {
            'login page returns 200': (r) => r.status === 200,
        });

        let registerPage = http.get(`${BASE_URL}/register`);
        check(registerPage, {
            'register page returns 200': (r) => r.status === 200,
        });
    });

    group('User Registration', function () {
        // Get CSRF token from register page
        let regPage = http.get(`${BASE_URL}/register`);
        let csrf = getCsrf(regPage);

        let registerRes = http.post(`${BASE_URL}/register`, {
            username: username,
            password: password,
            _csrf: csrf,
        });
        check(registerRes, {
            'registration succeeds': (r) => r.status === 200 || r.status === 302,
        });
    });

    group('Authenticated Flow', function () {
        // Get CSRF token from login page
        let loginPage = http.get(`${BASE_URL}/login`);
        let csrf = getCsrf(loginPage);

        let loginRes = http.post(`${BASE_URL}/login`, {
            username: username,
            password: password,
            _csrf: csrf,
        });
        check(loginRes, {
            'login succeeds': (r) => r.status === 200 || r.status === 302,
        });

        // Authenticated requests (cookies are kept per VU)
        let dashboard = http.get(`${BASE_URL}/dashboard`);
        check(dashboard, {
            'dashboard accessible': (r) => r.status === 200,
        });

        let txns = http.get(`${BASE_URL}/transactions`);
        check(txns, {
            'transactions page accessible': (r) => r.status === 200,
        });
    });

    sleep(1);
}
