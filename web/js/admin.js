'use strict';
/* Administrator portal: departments, students, faculty, courses, enrollments, exams, fees, accounts. */

initPage({
  role: 'ADMIN',
  title: 'Administrator',
  sections: [
    { id: 'dashboard', label: 'Dashboard', render: dashboard },
    { id: 'departments', label: 'Departments', render: departments },
    { id: 'students', label: 'Students', render: students },
    { id: 'faculty', label: 'Faculty', render: faculty },
    { id: 'courses', label: 'Courses', render: courses },
    { id: 'enrollments', label: 'Enrollments', render: enrollments },
    { id: 'exams', label: 'Exams & Results', render: exams },
    { id: 'fees', label: 'Fees', render: fees },
    { id: 'accounts', label: 'User accounts', render: accounts },
  ],
});

/* ---------- small helpers ---------- */
const tile = (label, value, cls = '') => `<div class="stat ${cls}"><span>${esc(label)}</span><strong>${esc(value)}</strong></div>`;
const toOptions = (list, valueKey, label) =>
  list.map(x => ({ value: x[valueKey], label: typeof label === 'function' ? label(x) : x[label] }));
const deptOptions = async () => toOptions(await api.get('/api/admin/departments'), 'deptId', 'name');
const studentOptions = async () =>
  toOptions(await api.get('/api/admin/students'), 'studentId', s => `${s.rollNo} – ${s.name}`);
const facultyOptions = async (withNone) => {
  const list = toOptions(await api.get('/api/admin/faculty'), 'facultyId', f => `${f.name} (${f.designation})`);
  return withNone ? [{ value: '', label: '— Unassigned —' }, ...list] : list;
};
function debounce(fn, ms) { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; }

/** Title + toolbar buttons + an empty card where the table goes. Returns the card element. */
function shell(c, title, toolbarHtml = '') {
  c.innerHTML = `<div class="toolbar"><h2>${esc(title)}</h2><div class="row">${toolbarHtml}</div></div><div class="tbl card"></div>`;
  return c.querySelector('.tbl');
}
const addButton = (label) => `<button class="btn btn-primary" data-act="add">${esc(label)}</button>`;

/* ---------- dashboard ---------- */
async function dashboard(c) {
  const s = await api.get('/api/admin/stats');
  c.innerHTML = `<h2>Dashboard</h2><h3>University</h3>
    <div class="stats">${tile('Students', s.students)}${tile('Faculty', s.faculty)}${tile('Courses', s.courses)}${tile('Exams scheduled', s.exams)}</div>
    <h3>Fee collection</h3>
    <div class="stats">${tile('Total billed', money(s.billed))}${tile('Collected', money(s.collected), 'good')}${tile('Outstanding', money(s.outstanding), 'warn')}</div>`;
}

/* ---------- departments ---------- */
async function departments(c) {
  const box = shell(c, 'Departments', addButton('+ Add department'));
  const load = async () => {
    box.innerHTML = table([{ key: 'deptId', label: 'ID' }, { key: 'name', label: 'Department' }],
      await api.get('/api/admin/departments'));
  };
  onActions(c, {
    add: () => openForm({
      title: 'Add department', fields: [{ name: 'name', label: 'Department name' }],
      onSubmit: async d => { const r = await api.post('/api/admin/departments', d); await load(); return r.message; },
    }),
  });
  await load();
}

/* ---------- students ---------- */
const studentFields = (depts, isNew) => [
  ...(isNew ? [{ name: 'rollNo', label: 'Roll number', placeholder: 'RA2111003010001' }] : []),
  { name: 'name', label: 'Full name' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'phone', label: 'Phone (10 digits)', type: 'tel', pattern: '\\d{10}' },
  { name: 'deptId', label: 'Department', type: 'select', options: depts },
  { name: 'semester', label: 'Semester (1-8)', type: 'number', min: 1, max: 8 },
  ...(isNew ? [{ name: 'password', label: 'Temporary password', type: 'password', minlength: 6,
    help: 'The student logs in with the roll number (lower-case) and this password.' }] : []),
];

async function students(c) {
  const box = shell(c, 'Students',
    '<input id="q" type="search" placeholder="Search name or roll no…">' + addButton('+ Add student'));
  let rows = [];
  const load = async () => {
    const q = c.querySelector('#q').value.trim();
    rows = await api.get('/api/admin/students', q ? { q } : {});
    box.innerHTML = table([
      { key: 'rollNo', label: 'Roll no' }, { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' },
      { key: 'phone', label: 'Phone' }, { key: 'deptName', label: 'Department' }, { key: 'semester', label: 'Sem' },
      { label: 'Actions', render: r => btn('Edit', 'edit', r.studentId) + btn('Delete', 'delete', r.studentId, 'btn-danger') },
    ], rows);
  };
  c.querySelector('#q').addEventListener('input', debounce(() => load().catch(e => toast(e.message, 'error')), 250));
  onActions(c, {
    add: async () => openForm({
      title: 'Add student', fields: studentFields(await deptOptions(), true),
      onSubmit: async d => { const r = await api.post('/api/admin/students', d); await load(); return r.message; },
    }),
    edit: async id => {
      const s = rows.find(x => String(x.studentId) === id);
      openForm({
        title: 'Edit student – ' + s.rollNo, fields: studentFields(await deptOptions(), false), values: s,
        onSubmit: async d => { const r = await api.put('/api/admin/students', { ...d, id }); await load(); return r.message; },
      });
    },
    delete: async id => {
      const s = rows.find(x => String(x.studentId) === id);
      if (!confirm(`Delete ${s.name}? Their enrollments, marks and fees are deleted too.`)) return;
      toast((await api.del('/api/admin/students', { id })).message);
      await load();
    },
  });
  await load();
}

/* ---------- faculty ---------- */
const facultyFields = (depts, isNew) => [
  { name: 'name', label: 'Full name' },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'deptId', label: 'Department', type: 'select', options: depts },
  { name: 'designation', label: 'Designation', placeholder: 'Assistant Professor' },
  ...(isNew ? [
    { name: 'username', label: 'Login username', pattern: '[a-z0-9._\\-]{3,30}', help: '3-30 lower-case letters, digits, dot, dash or underscore.' },
    { name: 'password', label: 'Temporary password', type: 'password', minlength: 6 },
  ] : []),
];

async function faculty(c) {
  const box = shell(c, 'Faculty', addButton('+ Add faculty'));
  let rows = [];
  const load = async () => {
    rows = await api.get('/api/admin/faculty');
    box.innerHTML = table([
      { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' }, { key: 'deptName', label: 'Department' },
      { key: 'designation', label: 'Designation' },
      { label: 'Actions', render: r => btn('Edit', 'edit', r.facultyId) + btn('Delete', 'delete', r.facultyId, 'btn-danger') },
    ], rows);
  };
  onActions(c, {
    add: async () => openForm({
      title: 'Add faculty', fields: facultyFields(await deptOptions(), true),
      onSubmit: async d => { const r = await api.post('/api/admin/faculty', d); await load(); return r.message; },
    }),
    edit: async id => {
      const f = rows.find(x => String(x.facultyId) === id);
      openForm({
        title: 'Edit faculty – ' + f.name, fields: facultyFields(await deptOptions(), false), values: f,
        onSubmit: async d => { const r = await api.put('/api/admin/faculty', { ...d, id }); await load(); return r.message; },
      });
    },
    delete: async id => {
      const f = rows.find(x => String(x.facultyId) === id);
      if (!confirm(`Delete ${f.name}? Their courses will become unassigned.`)) return;
      toast((await api.del('/api/admin/faculty', { id })).message);
      await load();
    },
  });
  await load();
}

/* ---------- courses ---------- */
const courseFields = (depts, facultyOpts, isNew) => [
  ...(isNew ? [{ name: 'courseCode', label: 'Course code', placeholder: '21CSE456T' }] : []),
  { name: 'courseName', label: 'Course name' },
  { name: 'credits', label: 'Credits (1-6)', type: 'number', min: 1, max: 6 },
  { name: 'deptId', label: 'Department', type: 'select', options: depts },
  { name: 'facultyId', label: 'Faculty', type: 'select', options: facultyOpts, required: false },
];

async function courses(c) {
  const box = shell(c, 'Courses', addButton('+ Add course'));
  let rows = [];
  const load = async () => {
    rows = await api.get('/api/admin/courses');
    box.innerHTML = table([
      { key: 'courseCode', label: 'Code' }, { key: 'courseName', label: 'Course' }, { key: 'credits', label: 'Credits' },
      { key: 'deptName', label: 'Department' },
      { label: 'Faculty', render: r => esc(r.facultyName || '— unassigned —') },
      { label: 'Actions', render: r => btn('Edit', 'edit', r.courseId) + btn('Delete', 'delete', r.courseId, 'btn-danger') },
    ], rows);
  };
  onActions(c, {
    add: async () => openForm({
      title: 'Add course', fields: courseFields(await deptOptions(), await facultyOptions(true), true),
      onSubmit: async d => { const r = await api.post('/api/admin/courses', d); await load(); return r.message; },
    }),
    edit: async id => {
      const course = rows.find(x => String(x.courseId) === id);
      openForm({
        title: 'Edit course – ' + course.courseCode,
        fields: courseFields(await deptOptions(), await facultyOptions(true), false), values: course,
        onSubmit: async d => { const r = await api.put('/api/admin/courses', { ...d, id }); await load(); return r.message; },
      });
    },
    delete: async id => {
      const course = rows.find(x => String(x.courseId) === id);
      if (!confirm(`Delete ${course.courseCode}? Its enrollments, exams and marks are deleted too.`)) return;
      toast((await api.del('/api/admin/courses', { id })).message);
      await load();
    },
  });
  await load();
}

/* ---------- enrollments ---------- */
async function enrollments(c) {
  const list = await api.get('/api/admin/courses');
  if (list.length === 0) { c.innerHTML = '<h2>Enrollments</h2><div class="card empty">Add a course first.</div>'; return; }
  const select = '<select id="course">' + list.map(x =>
    `<option value="${esc(x.courseId)}">${esc(x.courseCode)} – ${esc(x.courseName)}</option>`).join('') + '</select>';
  const box = shell(c, 'Enrollments', select + addButton('+ Enroll student'));
  const sel = c.querySelector('#course');
  const load = async () => {
    const rows = await api.get('/api/admin/enrollments', { courseId: sel.value });
    box.innerHTML = table([
      { key: 'rollNo', label: 'Roll no' }, { key: 'name', label: 'Student' }, { key: 'deptName', label: 'Department' },
      { key: 'semester', label: 'Sem' }, { label: 'Action', render: r => btn('Remove', 'remove', r.studentId, 'btn-danger') },
    ], rows, 'No students are enrolled in this course yet.');
  };
  sel.addEventListener('change', () => load().catch(e => toast(e.message, 'error')));
  onActions(c, {
    add: async () => openForm({
      title: 'Enroll student', submitLabel: 'Enroll',
      fields: [{ name: 'studentId', label: 'Student', type: 'select', options: await studentOptions() }],
      onSubmit: async d => {
        const r = await api.post('/api/admin/enrollments', { studentId: d.studentId, courseId: sel.value });
        await load(); return r.message;
      },
    }),
    remove: async id => {
      if (!confirm('Remove this student from the course?')) return;
      toast((await api.del('/api/admin/enrollments', { studentId: id, courseId: sel.value })).message);
      await load();
    },
  });
  await load();
}

/* ---------- exams & results ---------- */
async function exams(c) {
  const box = shell(c, 'Exams & Results', addButton('+ Schedule exam'));
  let rows = [];
  const load = async () => {
    rows = await api.get('/api/admin/exams');
    box.innerHTML = table([
      { key: 'courseCode', label: 'Course' }, { key: 'examName', label: 'Exam' }, { key: 'examDate', label: 'Date' },
      { key: 'maxMarks', label: 'Max marks' },
      { label: 'Actions', render: r => btn('View results', 'results', r.examId) + btn('Delete', 'delete', r.examId, 'btn-danger') },
    ], rows);
  };
  onActions(c, {
    add: async () => {
      const list = await api.get('/api/admin/courses');
      openForm({
        title: 'Schedule exam',
        fields: [
          { name: 'courseId', label: 'Course', type: 'select', options: toOptions(list, 'courseId', x => `${x.courseCode} – ${x.courseName}`) },
          { name: 'examName', label: 'Exam name', placeholder: 'CIA-1 / End Semester' },
          { name: 'examDate', label: 'Exam date', type: 'date' },
          { name: 'maxMarks', label: 'Maximum marks (1-500)', type: 'number', min: 1, max: 500 },
        ],
        onSubmit: async d => { const r = await api.post('/api/admin/exams', d); await load(); return r.message; },
      });
    },
    results: async id => {
      const e = rows.find(x => String(x.examId) === id);
      const list = await api.get('/api/admin/results', { examId: id });
      openInfo(`Results – ${e.courseCode} ${e.examName}`, table(resultColumns(true), list, 'No marks have been entered yet.'), true);
    },
    delete: async id => {
      if (!confirm('Delete this exam and all its marks?')) return;
      toast((await api.del('/api/admin/exams', { id })).message);
      await load();
    },
  });
  await load();
}

/* ---------- fees ---------- */
const feeFormFields = [
  { name: 'description', label: 'Description', placeholder: 'Tuition Fee Sem 5' },
  { name: 'amount', label: 'Amount (₹)', type: 'number', min: 0.01, step: '0.01' },
  { name: 'dueDate', label: 'Due date', type: 'date' },
];

async function fees(c) {
  c.innerHTML = `<div class="toolbar"><h2>Fee management</h2><div class="row">
      <button class="btn btn-primary" data-act="addFee">+ Add fee</button>
      <button class="btn" data-act="billAll">Bill all students</button>
      <button class="btn" data-act="ledger">Student ledger</button></div></div>
    <div class="stats" id="sum"></div><h3>Outstanding dues</h3><div class="tbl card"></div>`;
  const box = c.querySelector('.tbl');
  let rows = [];
  const load = async () => {
    const [s, list] = await Promise.all([api.get('/api/admin/fees/summary'), api.get('/api/admin/fees/outstanding')]);
    rows = list;
    c.querySelector('#sum').innerHTML = tile('Total billed', money(s.billed)) + tile('Collected', money(s.collected), 'good') +
      tile('Outstanding', money(s.outstanding), 'warn');
    box.innerHTML = table([...feeColumns(true), { label: 'Action', render: r => btn('Record payment', 'pay', r.feeId) }],
      rows, 'No outstanding dues.');
  };
  onActions(c, {
    addFee: async () => openForm({
      title: 'Add fee for a student',
      fields: [{ name: 'studentId', label: 'Student', type: 'select', options: await studentOptions() }, ...feeFormFields],
      onSubmit: async d => { const r = await api.post('/api/admin/fees', d); await load(); return r.message; },
    }),
    billAll: () => openForm({
      title: 'Bill ALL students', submitLabel: 'Bill everyone', fields: feeFormFields,
      onSubmit: async d => {
        if (!confirm(`Bill ${money(d.amount)} to every student?`)) throw new Error('Cancelled.');
        const r = await api.post('/api/admin/fees/all', d); await load(); return r.message;
      },
    }),
    ledger: async () => openForm({
      title: 'Student ledger', submitLabel: 'Show',
      fields: [{ name: 'studentId', label: 'Student', type: 'select', options: await studentOptions() }],
      onSubmit: async d => {
        const data = await api.get('/api/admin/fees/ledger', { studentId: d.studentId });
        openInfo('Student ledger',
          '<h3>Fees</h3>' + table(feeColumns(false), data.fees) + '<h3>Payments</h3>' + table(paymentColumns, data.payments, 'No payments yet.'), true);
      },
    }),
    pay: id => {
      const fee = rows.find(x => String(x.feeId) === id);
      openForm({
        title: `Record payment – ${fee.rollNo} (${fee.description})`, submitLabel: 'Record payment',
        fields: [
          { name: 'amount', label: `Amount received (balance ${money(fee.balance)})`, type: 'number', min: 0.01, step: '0.01', value: fee.balance },
          { name: 'mode', label: 'Payment mode', type: 'select', options: PAYMENT_MODES },
        ],
        onSubmit: async d => { const r = await api.post('/api/admin/payments', { ...d, feeId: id }); await load(); return r.message; },
      });
    },
  });
  await load();
}

/* ---------- user accounts ---------- */
async function accounts(c) {
  const box = shell(c, 'User accounts');
  let rows = [];
  const load = async () => {
    rows = await api.get('/api/admin/users');
    box.innerHTML = table([
      { key: 'username', label: 'Username' }, { label: 'Role', render: r => badge(r.role) },
      { label: 'Status', render: r => badge(r.active ? 'Active' : 'Inactive') },
      { label: 'Actions', render: r => btn('Reset password', 'reset', r.userId) +
          btn(r.active ? 'Disable' : 'Enable', 'toggle', r.userId, r.active ? 'btn-danger' : '') },
    ], rows);
  };
  onActions(c, {
    reset: id => {
      const u = rows.find(x => String(x.userId) === id);
      openForm({
        title: 'Reset password – ' + u.username,
        fields: [{ name: 'password', label: 'New password', type: 'password', minlength: 6 }],
        onSubmit: async d => (await api.post('/api/admin/users/reset', { userId: id, password: d.password })).message,
      });
    },
    toggle: async id => {
      const u = rows.find(x => String(x.userId) === id);
      if (!confirm(`${u.active ? 'Disable' : 'Enable'} the account "${u.username}"?`)) return;
      toast((await api.post('/api/admin/users/toggle', { userId: id })).message);
      await load();
    },
  });
  await load();
}
