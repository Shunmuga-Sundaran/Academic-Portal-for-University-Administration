'use strict';
/* Small wrapper around fetch(): talks to the Java backend and turns errors into exceptions. */
const api = (() => {
  async function request(method, url, params = {}) {
    const options = { method, credentials: 'same-origin', headers: {} };
    const body = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) body.append(key, value);
    });
    if (method === 'GET' || method === 'DELETE') {
      const qs = body.toString();
      if (qs) url += (url.includes('?') ? '&' : '?') + qs;
    } else {
      options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
      options.body = body.toString();
    }
    const res = await fetch(url, options);
    let data = null;
    try { data = await res.json(); } catch (e) { /* empty body */ }
    if (!res.ok) {
      // session ended -> back to the login page (but not while logging in)
      if (
  res.status === 401 &&
  url.split('?')[0] !== '/api/login' &&
  url.split('?')[0] !== '/api/me'
) {
  window.location.href = '/index.html';
}
      const err = new Error((data && data.error) || 'Request failed (' + res.status + ')');
      err.status = res.status;
      throw err;
    }
    return data;
  }
  return {
    get: (url, params) => request('GET', url, params),
    post: (url, params) => request('POST', url, params),
    put: (url, params) => request('PUT', url, params),
    del: (url, params) => request('DELETE', url, params),
  };
})();
