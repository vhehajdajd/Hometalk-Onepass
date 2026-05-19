/*
    공통 fetch / csrf / 공통 util
 */

const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

/*
     JSON 요청용 CSRF 헤더
 */
function getCsrfHeaders() {

    const headers = {
        'Content-Type': 'application/json'
    };

    if (CSRF_HEADER && CSRF_TOKEN) {
        headers[CSRF_HEADER] = CSRF_TOKEN;
    }

    return headers;
}

/*
     multipart/form-data 요청용 헤더
 */
function getCsrfOnlyHeaders() {

    const headers = {};

    if (CSRF_HEADER && CSRF_TOKEN) {
        headers[CSRF_HEADER] = CSRF_TOKEN;
    }

    return headers;
}

/*
     공통 fetch wrapper
 */
async function apiFetch(url, options = {}) {

    return fetch(url, {
        ...options,
        headers: {
            ...getCsrfHeaders(),
            ...(options.headers || {})
        }
    });
}

/*
     FormData 전용 fetch wrapper
 */
async function formDataFetch(url, options = {}) {

    return fetch(url, {
        ...options,
        headers: {
            ...getCsrfOnlyHeaders(),
            ...(options.headers || {})
        }
    });
}