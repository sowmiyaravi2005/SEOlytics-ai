import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  BarChart3, FileSearch, GitCompare, History, LayoutDashboard, LogOut, Menu, Scale, Sparkles
} from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../context/useAuth.js';

const links = [
  { to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/audits/new', label: 'New audit', icon: FileSearch },
  { to: '/history', label: 'Audit history', icon: History },
  { to: '/compare', label: 'Compare audits', icon: GitCompare },
  { to: '/methodology', label: 'Scoring', icon: Scale }
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(true);

  return (
    <div className={`shell ${open ? '' : 'collapsed'}`}>
      <aside className="sidebar">
        <div className="brand">
          <Sparkles size={22} />
          {open && <div><strong>SEOlytics</strong><span>Technical auditor</span></div>}
        </div>
        <nav>
          {links.map((link) => (
            <NavLink key={link.to} to={link.to} end={link.end} className={({ isActive }) => isActive ? 'nav active' : 'nav'}>
              <link.icon size={18} />
              {open && <span>{link.label}</span>}
            </NavLink>
          ))}
        </nav>
        <button className="logout" onClick={() => { logout(); navigate('/login'); }}>
          <LogOut size={18} />
          {open && <span>Log out</span>}
        </button>
      </aside>
      <div className="main">
        <header className="topbar">
          <button className="icon-btn" onClick={() => setOpen((v) => !v)} aria-label="Toggle sidebar">
            <Menu size={18} />
          </button>
          <div className="topbar-title">
            <BarChart3 size={16} />
            Automated technical SEO audits
          </div>
          <div className="user-chip">
            <span>{user.fullName}</span>
            <small>{user.email}</small>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
