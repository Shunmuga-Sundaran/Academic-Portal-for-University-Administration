'use strict';
/* Student portal: profile, courses, exam schedule, results, fees. */

initPage({
  role: 'STUDENT',
  title: 'Student Portal',
  sections: [
    { id: 'profile', label: 'My profile', render: profile },
    { id: 'courses', label: 'Courses', render: courses },
    { id: 'exams', label: 'Exam schedule', render: exams },
    { id: 'results', label: 'My results', render: results },
    { id: 'fees', label: 'Fees', render: fees },
  ],
});

/* ---------- profile ---------- */
async function profile(c, me) {
  const s = me.details;
  const row = (label, value) => `<dt>${esc(label)}</dt><dd>${esc(value)}</dd>`;
  c.innerHTML = `<h2>My profile</h2><div class="card"><dl class="profile-grid">
    ${row('Name', s.name)}${row('Roll number', s.rollNo)}${row('Email', s.email)}${row('Phone', s.phone)}
    ${row('Department', s.deptName)}${row('Semester', s.semester)}${row('Username', me.username)}</dl></div>`;
}

/* ---------- courses (browse, enroll, drop) ---------- */
async function courses(c) {
  c.innerHTML = '<div class="toolbar"><h2>Courses</h2></div><div class="tbl card"></div>';
  const box = c.querySelector('.tbl');
  const load = async () => {
    box.innerHTML = table([
      { key: 'courseCode', label: 'Code' }, { key: 'courseName', label: 'Course' }, { key: 'credits', label: 'Credits' },
      { key: 'deptName', label: 'Department' }, { label: 'Faculty', render: r => esc(r.facultyName || '—') },
      { label: 'Status', render: r => r.enrolled ? badge('Enrolled') : '' },
      { label: 'Action', render: r => r.enrolled ? btn('Drop', 'drop', r.courseId, 'btn-danger') : btn('Enroll', 'enroll', r.courseId) },
    ], await api.get('/api/student/courses'));
  };
  onActions(c, {
    enroll: async id => { toast((await api.post('/api/student/enroll', { courseId: id })).message); await load(); },
    drop: async id => {
      if (!confirm('Drop this course?')) return;
      toast((await api.post('/api/student/drop', { courseId: id })).message);
      await load();
    },
  });
  await load();
}

/* ---------- exam schedule ---------- */
async function exams(c) {
  const rows = await api.get('/api/student/exams');
  c.innerHTML = '<div class="toolbar"><h2>Exam schedule</h2></div><div class="tbl card"></div>';
  c.querySelector('.tbl').innerHTML = table([
    { key: 'examDate', label: 'Date' }, { key: 'courseCode', label: 'Course' }, { key: 'examName', label: 'Exam' },
    { key: 'maxMarks', label: 'Max marks' },
  ], rows, 'No exams are scheduled for your courses yet.');
}

/* ---------- results ---------- */
async function results(c) {
  const d = await api.get('/api/student/results');
  c.innerHTML = '<div class="toolbar"><h2>My results</h2></div><div class="stats" id="stats"></div><div class="tbl card"></div>';
  if (d.overall !== null) {
    c.querySelector('#stats').innerHTML =
      `<div class="stat"><span>Overall percentage</span><strong>${esc(d.overall)}%</strong></div>` +
      `<div class="stat good"><span>Overall grade</span><strong>${esc(d.grade)}</strong></div>`;
  }
  c.querySelector('.tbl').innerHTML = table(resultColumns(false), d.results, 'No results have been published yet.');
}

/* ---------- fees ---------- */
async function fees(c) {
  c.innerHTML = '<div class="toolbar"><h2>Fees & payments</h2></div><div class="stats" id="stats"></div><h3>My fees</h3><div class="tbl card" id="fees"></div><h3>Payment history</h3><div class="tbl card" id="pays"></div>';
  let rows = [];
  const load = async () => {
    const d = await api.get('/api/student/fees');
    rows = d.fees;
    c.querySelector('#stats').innerHTML = `<div class="stat warn"><span>Total outstanding</span><strong>${esc(money(d.outstanding))}</strong></div>`;
    c.querySelector('#fees').innerHTML = table([...feeColumns(false),
      { label: 'Action', render: r => Number(r.balance) > 0 ? btn('Pay', 'pay', r.feeId) : '' }], rows, 'No fees have been assigned to you.');
    c.querySelector('#pays').innerHTML = table(paymentColumns, d.payments, 'No payments yet.');
  };
  onActions(c, {
    pay: id => {
      const fee = rows.find(x => String(x.feeId) === id);
      openForm({
        title: `Pay – ${fee.description}`, submitLabel: 'Pay now',
        fields: [
          { name: 'amount', label: `Amount (balance ${money(fee.balance)})`, type: 'number', min: 0.01, max: fee.balance, step: '0.01', value: fee.balance },
          { name: 'mode', label: 'Payment mode', type: 'select', options: PAYMENT_MODES },
        ],
        onSubmit: async d => { const r = await api.post('/api/student/pay', { ...d, feeId: id }); await load(); return r.message; },
      });
    },
  });
  await load();
}
