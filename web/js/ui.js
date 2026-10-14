'use strict';
/* Shared UI helpers: escaping, tables, forms in popups, toasts. */

/** Escapes text before it is put into HTML (protects against XSS). */
function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, ch =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
}

function money(n) {
  return '₹' + Number(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function badge(text) {
  return `<span class="badge badge-${esc(String(text).toLowerCase())}">${esc(text)}</span>`;
}

function btn(label, action, id, extraClass = '') {
  return `<button class="btn btn-sm ${extraClass}" data-act="${esc(action)}" data-id="${esc(id)}">${esc(label)}</button>`;
}

function toast(message, type = 'ok') {
  let box = document.getElementById('toasts');
  if (!box) { box = document.createElement('div'); box.id = 'toasts'; document.body.appendChild(box); }
  const t = document.createElement('div');
  t.className = 'toast toast-' + type;
  t.textContent = message;
  box.appendChild(t);
  setTimeout(() => t.remove(), 3500);
}

/**
 * Builds an HTML table. columns: [{label, key}] or [{label, render: row => htmlString}].
 * Values read with "key" are escaped automatically; "render" functions must call esc() themselves.
 */
function table(columns, rows, emptyText = 'No records found.') {
  if (!rows || rows.length === 0) return `<div class="empty">${esc(emptyText)}</div>`;
  const head = columns.map(c => `<th>${esc(c.label)}</th>`).join('');
  const body = rows.map(r =>
    '<tr>' + columns.map(c => `<td>${c.render ? c.render(r) : esc(r[c.key])}</td>`).join('') + '</tr>').join('');
  return `<div class="table-wrap"><table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table></div>`;
}

/** One click handler for every button with data-act inside a container (survives re-rendering). */
function onActions(container, handlers) {
  container.onclick = async (e) => {
    const b = e.target.closest('[data-act]');
    if (!b || !container.contains(b)) return;
    const fn = handlers[b.dataset.act];
    if (!fn) return;
    try { await fn(b.dataset.id, b); } catch (err) { toast(err.message, 'error'); }
  };
}

function closeModal() { document.querySelectorAll('.modal-overlay').forEach(m => m.remove()); }

function fieldHtml(f, values) {
  const val = values[f.name] ?? f.value ?? '';
  const required = f.required === false ? '' : 'required';
  let input;
  if (f.type === 'select') {
    input = `<select name="${esc(f.name)}" ${required}>` + (f.options || []).map(o =>
      `<option value="${esc(o.value)}" ${String(o.value) === String(val) ? 'selected' : ''}>${esc(o.label)}</option>`).join('') + '</select>';
  } else {
    const attrs = [
      f.pattern ? `pattern="${esc(f.pattern)}"` : '',
      f.min !== undefined ? `min="${esc(f.min)}"` : '',
      f.max !== undefined ? `max="${esc(f.max)}"` : '',
      f.step ? `step="${esc(f.step)}"` : '',
      f.minlength ? `minlength="${esc(f.minlength)}"` : '',
      f.readonly ? 'readonly' : '',
    ].join(' ');
    input = `<input type="${esc(f.type || 'text')}" name="${esc(f.name)}" value="${esc(val)}" ${required} ${attrs} placeholder="${esc(f.placeholder || '')}">`;
  }
  return `<label class="field"><span>${esc(f.label)}</span>${input}${f.help ? `<small>${esc(f.help)}</small>` : ''}</label>`;
}

/** Popup form. onSubmit(data) must return a success message (or throw an Error to show it in the popup). */
function openForm({ title, fields, values = {}, submitLabel = 'Save', onSubmit }) {
  closeModal();
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `<div class="modal"><h3>${esc(title)}</h3><form>
      ${fields.map(f => fieldHtml(f, values)).join('')}
      <div class="form-error" hidden></div>
      <div class="modal-actions"><button type="button" class="btn btn-ghost" data-close>Cancel</button>
      <button type="submit" class="btn btn-primary">${esc(submitLabel)}</button></div></form></div>`;
  document.body.appendChild(overlay);
  const form = overlay.querySelector('form');
  const errBox = overlay.querySelector('.form-error');
  overlay.addEventListener('click', e => {
    if (e.target === overlay || e.target.hasAttribute('data-close')) overlay.remove();
  });
  const first = form.querySelector('input:not([readonly]), select');
  if (first) first.focus();
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!form.reportValidity()) return;          // browser-side validation (required, email, pattern...)
    const data = Object.fromEntries(new FormData(form).entries());
    const submit = form.querySelector('[type=submit]');
    submit.disabled = true; errBox.hidden = true;
    try {
      const message = await onSubmit(data);
      overlay.remove();
      if (message) toast(message);
    } catch (err) {
      errBox.textContent = err.message; errBox.hidden = false; submit.disabled = false;
    }
  });
}

/** Read-only popup with any HTML inside (used for result lists, ledgers...). */
function openInfo(title, html, wide = false) {
  closeModal();
  const overlay = document.createElement('div');
  overlay.className = 'modal-overlay';
  overlay.innerHTML = `<div class="modal ${wide ? 'wide' : ''}"><h3>${esc(title)}</h3>${html}
    <div class="modal-actions"><button class="btn" data-close>Close</button></div></div>`;
  document.body.appendChild(overlay);
  overlay.addEventListener('click', e => {
    if (e.target === overlay || e.target.hasAttribute('data-close')) overlay.remove();
  });
}

const PAYMENT_MODES = [
  { value: 'UPI', label: 'UPI' }, { value: 'CARD', label: 'Card' },
  { value: 'CASH', label: 'Cash' }, { value: 'BANK_TRANSFER', label: 'Bank transfer' },
];

const feeColumns = (showStudent) => [
  ...(showStudent ? [{ label: 'Student', render: r => esc(r.rollNo + ' – ' + r.studentName) }] : []),
  { key: 'description', label: 'Description' },
  { label: 'Amount', render: r => `<span class="num">${money(r.amount)}</span>` },
  { label: 'Paid', render: r => `<span class="num">${money(r.paid)}</span>` },
  { label: 'Balance', render: r => `<span class="num">${money(r.balance)}</span>` },
  { key: 'dueDate', label: 'Due date' },
  { label: 'Status', render: r => badge(r.status) },
];

const paymentColumns = [
  { key: 'description', label: 'Fee' },
  { label: 'Amount', render: r => `<span class="num">${money(r.amount)}</span>` },
  { key: 'mode', label: 'Mode' },
  { label: 'Paid on', render: r => esc(String(r.paidOn).replace('T', ' ').slice(0, 19)) },
];

const resultColumns = (showStudent) => [
  ...(showStudent ? [{ key: 'rollNo', label: 'Roll no' }, { key: 'studentName', label: 'Student' }] : []),
  { key: 'courseCode', label: 'Course' },
  { key: 'examName', label: 'Exam' },
  { label: 'Marks', render: r => esc(Number(r.marks).toFixed(2) + ' / ' + r.maxMarks) },
  { label: '%', render: r => esc(r.percentage.toFixed(1)) },
  { label: 'Grade', render: r => badge(r.grade) },
];
