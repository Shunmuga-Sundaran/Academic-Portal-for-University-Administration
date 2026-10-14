'use strict';
const HOME = { ADMIN: '/admin.html', FACULTY: '/faculty.html', STUDENT: '/student.html' };

// Already logged in? Skip the form.
api.get('/api/me').then(me => { window.location.href = HOME[me.role]; }).catch(() => { /* not logged in */ });

document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const err = document.getElementById('err');
  const button = e.target.querySelector('button');
  err.hidden = true; button.disabled = true;
  try {
    const me = await api.post('/api/login', {
      username: e.target.elements['username'].value.trim(),
      password: e.target.elements['password'].value,
    });
    window.location.href = HOME[me.role];
  } catch (ex) {
    err.textContent = ex.message; err.hidden = false; button.disabled = false;
  }
});
