import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/useAuth.js';
import AppLayout from './layout/AppLayout.jsx';
import LoginPage from './pages/LoginPage.jsx';
import RegisterPage from './pages/RegisterPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import NewAuditPage from './pages/NewAuditPage.jsx';
import AuditDetailPage from './pages/AuditDetailPage.jsx';
import HistoryPage from './pages/HistoryPage.jsx';
import ComparePage from './pages/ComparePage.jsx';
import MethodologyPage from './pages/MethodologyPage.jsx';

function Protected({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="boot">Loading SEOlytics…</div>;
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

function Guest({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="boot">Loading SEOlytics…</div>;
  if (user) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Guest><LoginPage /></Guest>} />
      <Route path="/register" element={<Guest><RegisterPage /></Guest>} />
      <Route path="/" element={<Protected><AppLayout /></Protected>}>
        <Route index element={<DashboardPage />} />
        <Route path="audits/new" element={<NewAuditPage />} />
        <Route path="audits/:id" element={<AuditDetailPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="compare" element={<ComparePage />} />
        <Route path="methodology" element={<MethodologyPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
