'use strict';
/* Builds the page shell (sidebar, top bar), guards the role, and shows the selected section. */

async function initPage({ role, title, sections }) {
  let me;
  try { me = await api.get('/api/me'); } catch (e) { return; }      // 401 -> api.js redirects to login
  if (me.role !== role) { window.location.href = '/index.html'; return; } // wrong portal -> back to login

  document.title = title + ' – Academic Portal';
  const app = document.getElementById('app');
  app.innerHTML = `
    <aside class="sidebar">
      <div class="brand">Academic Portal<small>${esc(title)}</small></div>
      <nav class="nav">${sections.map(s => `<a href="#${esc(s.id)}" data-id="${esc(s.id)}">${esc(s.label)}</a>`).join('')}</nav>
    </aside>
    <div class="main">
      <header class="topbar">
        <strong>${esc(title)}</strong>
        <div class="who"><span>${esc(me.name)}</span>
          <button class="btn btn-sm" id="pwBtn">Change password</button>
          <button class="btn btn-sm" id="logoutBtn">Logout</button></div>
      </header>
      <main class="content" id="content"></main>
    </div>`;

  const content = document.getElementById('content');

  async function show() {
    const id = window.location.hash.slice(1) || sections[0].id;
    const section = sections.find(s => s.id === id) || sections[0];
    app.querySelectorAll('.nav a').forEach(a => a.classList.toggle('active', a.dataset.id === section.id));
    content.onclick = null;
    content.innerHTML = '<div class="loading">Loading…</div>';
    try { await section.render(content, me); }
    catch (err) { content.innerHTML = `<div class="error-card">${esc(err.message)}</div>`; }
  }

  document.getElementById('logoutBtn').onclick = async () => {
    try { await api.post('/api/logout'); } finally { window.location.href = '/index.html'; }
  };
  document.getElementById('pwBtn').onclick = () => openForm({
    title: 'Change password',
    fields: [
      { name: 'oldPassword', label: 'Current password', type: 'password' },
      { name: 'newPassword', label: 'New password', type: 'password', minlength: 6, help: 'At least 6 characters' },
    ],
    onSubmit: async d => (await api.post('/api/password', d)).message,
  });
  window.addEventListener('hashchange', show);
  show();
}
