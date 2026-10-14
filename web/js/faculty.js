'use strict';
/* Faculty portal: own courses, exams, marks entry, results. */

initPage({
  role: 'FACULTY',
  title: 'Faculty Portal',
  sections: [
    { id: 'courses', label: 'My courses', render: myCourses },
    { id: 'exams', label: 'Exams', render: myExams },
    { id: 'marks', label: 'Enter marks', render: marks },
    { id: 'results', label: 'Results', render: results },
  ],
});

const examLabel = e => `${e.courseCode} – ${e.examName} (${e.examDate})`;
const examSelect = list => '<select id="exam">' +
  list.map(e => `<option value="${esc(e.examId)}">${esc(examLabel(e))}</option>`).join('') + '</select>';

/* ---------- my courses ---------- */
async function myCourses(c) {
  const rows = await api.get('/api/faculty/courses');
  c.innerHTML = '<div class="toolbar"><h2>My courses</h2></div><div class="tbl card"></div>';
  c.querySelector('.tbl').innerHTML = table([
    { key: 'courseCode', label: 'Code' }, { key: 'courseName', label: 'Course' }, { key: 'credits', label: 'Credits' },
    { key: 'deptName', label: 'Department' },
    { label: 'Action', render: r => btn('Class roster', 'roster', r.courseId) },
  ], rows, 'No courses are assigned to you yet. Please contact the administrator.');
  onActions(c, {
    roster: async id => {
      const course = rows.find(x => String(x.courseId) === id);
      const students = await api.get('/api/faculty/roster', { courseId: id });
      openInfo(`Class roster – ${course.courseCode} ${course.courseName}`, table([
        { key: 'rollNo', label: 'Roll no' }, { key: 'name', label: 'Name' }, { key: 'email', label: 'Email' },
        { key: 'semester', label: 'Sem' }], students, 'No students are enrolled yet.'), true);
    },
  });
}

/* ---------- exams ---------- */
async function myExams(c) {
  c.innerHTML = '<div class="toolbar"><h2>My exams</h2><div class="row"><button class="btn btn-primary" data-act="add">+ Schedule exam</button></div></div><div class="tbl card"></div>';
  const box = c.querySelector('.tbl');
  const load = async () => {
    box.innerHTML = table([
      { key: 'courseCode', label: 'Course' }, { key: 'examName', label: 'Exam' }, { key: 'examDate', label: 'Date' },
      { key: 'maxMarks', label: 'Max marks' }], await api.get('/api/faculty/exams'), 'You have not scheduled any exams yet.');
  };
  onActions(c, {
    add: async () => {
      const courses = await api.get('/api/faculty/courses');
      if (courses.length === 0) { toast('No courses are assigned to you yet.', 'error'); return; }
      openForm({
        title: 'Schedule exam',
        fields: [
          { name: 'courseId', label: 'Course', type: 'select', options: courses.map(x => ({ value: x.courseId, label: `${x.courseCode} – ${x.courseName}` })) },
          { name: 'examName', label: 'Exam name', placeholder: 'CIA-1 / End Semester' },
          { name: 'examDate', label: 'Exam date', type: 'date' },
          { name: 'maxMarks', label: 'Maximum marks (1-500)', type: 'number', min: 1, max: 500 },
        ],
        onSubmit: async d => { const r = await api.post('/api/faculty/exams', d); await load(); return r.message; },
      });
    },
  });
  await load();
}

/* ---------- enter marks ---------- */
async function marks(c) {
  const list = await api.get('/api/faculty/exams');
  if (list.length === 0) { c.innerHTML = '<h2>Enter marks</h2><div class="card empty">Schedule an exam first (Exams page).</div>'; return; }
  c.innerHTML = `<div class="toolbar"><h2>Enter marks</h2><div class="row">${examSelect(list)}</div></div><div class="tbl card"></div>`;
  const sel = c.querySelector('#exam');
  const box = c.querySelector('.tbl');
  const load = async () => {
    const sheet = await api.get('/api/faculty/marks', { examId: sel.value });
    const max = sheet.exam.maxMarks;
    if (sheet.rows.length === 0) { box.innerHTML = '<div class="empty">No students are enrolled in this course.</div>'; return; }
    box.innerHTML = `<p class="hint">Maximum marks: <strong>${esc(max)}</strong>. Leave a box empty to skip that student.</p>` +
      table([
        { key: 'rollNo', label: 'Roll no' }, { key: 'name', label: 'Student' },
        { label: 'Marks', render: r => `<input class="marks-input" type="number" min="0" max="${esc(max)}" step="0.01" data-student="${esc(r.studentId)}" value="${esc(r.marks ?? '')}">` },
      ], sheet.rows) +
      '<div class="modal-actions"><button class="btn btn-primary" data-act="save">Save marks</button></div>';
  };
  sel.addEventListener('change', () => load().catch(e => toast(e.message, 'error')));
  onActions(c, {
    save: async () => {
      const entries = [...box.querySelectorAll('input[data-student]')]
        .filter(i => i.value.trim() !== '')
        .map(i => `${i.dataset.student}:${i.value.trim()}`);
      if (entries.length === 0) { toast('Enter marks for at least one student.', 'error'); return; }
      const bad = [...box.querySelectorAll('input[data-student]')].find(i => !i.checkValidity());
      if (bad) { bad.reportValidity(); return; }
      toast((await api.post('/api/faculty/marks', { examId: sel.value, entries: entries.join(',') })).message);
      await load();
    },
  });
  await load();
}

/* ---------- results ---------- */
async function results(c) {
  const list = await api.get('/api/faculty/exams');
  if (list.length === 0) { c.innerHTML = '<h2>Results</h2><div class="card empty">You have not scheduled any exams yet.</div>'; return; }
  c.innerHTML = `<div class="toolbar"><h2>Results</h2><div class="row">${examSelect(list)}</div></div><div class="stats" id="stats"></div><div class="tbl card"></div>`;
  const sel = c.querySelector('#exam');
  const load = async () => {
    const d = await api.get('/api/faculty/results', { examId: sel.value });
    c.querySelector('#stats').innerHTML =
      `<div class="stat"><span>Class average</span><strong>${d.average === null ? '–' : esc(d.average + '%')}</strong></div>` +
      `<div class="stat good"><span>Passed (40% or more)</span><strong>${esc(d.passed)} of ${esc(d.total)}</strong></div>`;
    c.querySelector('.tbl').innerHTML = table(resultColumns(true), d.results, 'No marks have been entered for this exam yet.');
  };
  sel.addEventListener('change', () => load().catch(e => toast(e.message, 'error')));
  await load();
}
