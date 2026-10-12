import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Layout from './layouts/Layout';
import Login from './pages/Login';
import Dashboard from './pages/student/Dashboard';
import MarksAnalysis from './pages/student/MarksAnalysis';
import GrowthReport from './pages/student/GrowthReport';
import Documents from './pages/student/Documents';
import Payments from './pages/student/Payments';
import FocusMode from './pages/student/FocusMode';
import AdminHome from './pages/admin/AdminHome';
import StudentResources from './pages/student/StudentResources';
import SubjectDetail from './pages/student/SubjectDetail';

function Protected({ children, role }) { const { user, loading } = useAuth(); if (loading) return <div className="center-state">Loading portal…</div>; if (!user) return <Navigate to="/login" replace />; if (role && user.role !== role) return <Navigate to="/" replace />; return children; }
export default function App() { return <Routes><Route path="/login" element={<Login />} /><Route path="/" element={<Protected role="STUDENT"><Layout /></Protected>}><Route index element={<Dashboard />} /><Route path="subjects" element={<StudentResources kind="subjects" />} /><Route path="subjects/:id/syllabus" element={<SubjectDetail />} /><Route path="semesters" element={<StudentResources kind="semesters" />} /><Route path="schedule" element={<StudentResources kind="schedule" />} /><Route path="attendance" element={<StudentResources kind="attendance" />} /><Route path="attendance/:id" element={<SubjectDetail attendance />} /><Route path="assignments" element={<StudentResources kind="assignments" />} /><Route path="marks" element={<MarksAnalysis />} /><Route path="reports" element={<GrowthReport />} /><Route path="documents" element={<Documents />} /><Route path="payments" element={<Payments />} /><Route path="focus" element={<FocusMode />} /></Route><Route path="/admin" element={<Protected role="ADMIN"><Layout /></Protected>}><Route index element={<AdminHome />} /></Route><Route path="*" element={<Navigate to="/" replace />} /></Routes>; }
