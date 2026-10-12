import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../../services/api';
import { BackTitle, State } from '../../components/UI';

export default function SubjectDetail({ attendance = false }) {
  const { id } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api(attendance ? `/api/student/attendance/${id}` : `/api/student/subjects/${id}/syllabus`).then(setData).catch(e => setError(e.message)); }, [id, attendance]);
  if (error) return <State error>{error}</State>;
  if (!data) return <State>Loading…</State>;
  return <><BackTitle>{attendance ? 'Attendance History' : 'Syllabus'}</BackTitle>{attendance ? <div className="gold-table compact"><div className="table-head"><b>Date</b><b>Time</b><b>Status</b></div>{data.map(x => <div className="table-row" key={x.sessionId}><span>{x.sessionDate}</span><span>{x.startTime}–{x.endTime}</span><span>{x.status}</span></div>)}</div> : <section className="panel"><h2>Subject Syllabus</h2><p>{data.content || 'No syllabus text is available.'}</p></section>}</>;
}
