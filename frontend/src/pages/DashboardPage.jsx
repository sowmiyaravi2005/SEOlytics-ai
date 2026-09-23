import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { AlertTriangle, FileSearch, Globe, Layers } from 'lucide-react';
import { apiJson } from '../api';

const COLORS = ['#1d4ed8', '#0ea5e9', '#6366f1', '#94a3b8'];

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    apiJson('/api/dashboard').then(setData).catch((err) => setError(err.message));
  }, []);

  if (error) return <div className="banner error">{error}</div>;
  if (!data) return <div className="card glass loading-card">Loading dashboard…</div>;

  const donut = Object.entries(data.latestSeverityCounts || {}).map(([name, value]) => ({ name, value }));
  const empty = data.totalAudits === 0;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>SEO overview</h1>
          <p>Live metrics from your crawled websites and stored MySQL audit history.</p>
        </div>
        <Link className="btn primary" to="/audits/new">Start website audit</Link>
      </div>
      {empty && (
        <div className="empty glass">
          <FileSearch size={28} />
          <h3>No audits yet</h3>
          <p>Run your first crawl to populate health scores, issue charts, and history.</p>
        </div>
      )}
      <section className="stat-grid">
        <Stat icon={Globe} label="Websites audited" value={data.totalWebsites} />
        <Stat icon={Layers} label="Pages crawled" value={data.totalPagesCrawled} />
        <Stat icon={FileSearch} label="Overall SEO score" value={data.overallSeoScore ?? '—'} hint="/ 100" />
        <Stat icon={AlertTriangle} label="Latest issues" value={Object.values(data.latestSeverityCounts || {}).reduce((a, b) => a + b, 0)} />
      </section>
      <section className="grid-2">
        <article className="card glass">
          <h2>Issue distribution</h2>
          <p className="muted">From your most recent completed audit.</p>
          {donut.every((d) => d.value === 0) ? <p className="muted">No issue data yet.</p> : (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie data={donut} dataKey="value" nameKey="name" innerRadius={58} outerRadius={88} paddingAngle={3}>
                  {donut.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
          <div className="legend">
            {donut.map((d, i) => <span key={d.name}><i style={{ background: COLORS[i] }} />{d.name}: {d.value}</span>)}
          </div>
        </article>
        <article className="card glass">
          <h2>Crawl history</h2>
          <p className="muted">Score trend across recent audits.</p>
          {(data.crawlHistory || []).length === 0 ? <p className="muted">History appears after a crawl finishes.</p> : (
            <ResponsiveContainer width="100%" height={240}>
              <LineChart data={data.crawlHistory.map((p) => ({ ...p, label: (p.date || '').slice(0, 10) }))}>
                <XAxis dataKey="label" />
                <YAxis domain={[0, 100]} />
                <Tooltip />
                <Line type="monotone" dataKey="score" stroke="#2563eb" strokeWidth={2.4} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          )}
        </article>
      </section>
      <section className="grid-2">
        <article className="card glass">
          <h2>Healthy vs. issues</h2>
          <div className="split">
            <div><strong>{data.healthyPages}</strong><span>Healthy pages</span></div>
            <div><strong>{data.pagesWithIssues}</strong><span>Pages with issues</span></div>
          </div>
        </article>
        <article className="card glass">
          <h2>Top recurring issues</h2>
          {(data.topIssues || []).length === 0 ? <p className="muted">No recurring issues stored yet.</p> : (
            <ul className="issue-list">
              {data.topIssues.map((item) => (
                <li key={item.issueType}><span>{item.issueType.replaceAll('_', ' ')}</span><b>{item.count}</b></li>
              ))}
            </ul>
          )}
        </article>
      </section>
      <article className="card glass">
        <h2>Recent audit activity</h2>
        {(data.recentAudits || []).length === 0 ? <p className="muted">No recent crawls.</p> : (
          <table className="table">
            <thead><tr><th>Website</th><th>Status</th><th>Score</th><th>Pages</th><th>Issues</th></tr></thead>
            <tbody>
              {data.recentAudits.map((audit) => (
                <tr key={audit.id}>
                  <td><Link to={`/audits/${audit.id}`}>{audit.domain}</Link></td>
                  <td><span className={`pill ${audit.status.toLowerCase()}`}>{audit.status}</span></td>
                  <td>{audit.seoScore ?? '—'}</td>
                  <td>{audit.pagesCrawled}</td>
                  <td>{audit.issuesFound}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </article>
    </div>
  );
}

function Stat({ icon: Icon, label, value, hint }) {
  return (
    <article className="stat glass">
      <Icon size={18} />
      <span>{label}</span>
      <strong>{value}{hint && <small> {hint}</small>}</strong>
    </article>
  );
}
