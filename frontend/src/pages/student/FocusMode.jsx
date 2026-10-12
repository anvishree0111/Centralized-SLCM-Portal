import { useEffect, useState } from 'react';
import { api } from '../../services/api';
import { Section, State } from '../../components/UI';

export default function FocusMode() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api('/api/student/focus-mode').then(setData).catch(e => setError(e.message)); }, []);
  async function toggle(event) {
    const enabled = event.target.checked;
    try {
      await api('/api/me/preferences', { method: 'PUT', body: JSON.stringify({ focusModeEnabled: enabled }) });
      setData(current => ({ ...current, enabled }));
    } catch (e) { setError(e.message); event.target.checked = !enabled; }
  }
  if (error) return <State error>{error}</State>;
  if (!data) return <State>Loading focus mode…</State>;
  return <>
    <div className="focus-header"><span>Focus Mode</span><label className="switch"><input type="checkbox" checked={data.enabled} onChange={toggle} /><i /></label></div>
    <div className="focus-active">⚡ Focus Mode {data.enabled ? 'Activated' : 'Available'}</div>
    <Section title="Upcoming Examination"><div className="exam-table"><b>Date</b><b>Course Name</b><b>Syllabus</b>{data.exams?.map(e => <div className="contents" key={e.id}><span>{e.examDate}</span><span>{e.subjectName}</span><a href={`/subjects/${e.subjectId}/syllabus`}>View Syllabus</a></div>)}</div></Section>
    <div className="alerts">{data.dueAssignments?.map(a => <div key={a.id}>⌛ {a.title} Assignment Due</div>)}{data.attendanceAlerts?.map(a => <div key={a.subjectId}>⚠️ Low Attendance in {a.subjectName}</div>)}</div>
  </>;
}
