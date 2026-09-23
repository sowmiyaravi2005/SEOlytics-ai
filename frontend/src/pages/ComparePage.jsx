import { useEffect, useState } from 'react';
import { apiJson } from '../api';

export default function ComparePage() {
  const [audits, setAudits] = useState([]);
  const [first, setFirst] = useState('');
  const [second, setSecond] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    apiJson('/api/audits').then(setAudits).catch((err) => setError(err.message));
  }, []);

  async function compare(e) {
    e.preventDefault();
    setError('');
    try {
      setResult(await apiJson(`/api/audits/${first}/compare/${second}`));
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="page">
      <h1>Compare audits</h1>
      <p>Select two crawls of the same website to verify fixes and compare SEO health before and after re-crawling.</p>
      <form className="filters glass" onSubmit={compare}>
        <select value={first} onChange={(e) => setFirst(e.target.value)} required>
          <option value="">First audit</option>
          {audits.map((a) => <option key={a.id} value={a.id}>{a.domain} · {a.id} · {a.status}</option>)}
        </select>
        <select value={second} onChange={(e) => setSecond(e.target.value)} required>
          <option value="">Second audit</option>
          {audits.map((a) => <option key={a.id} value={a.id}>{a.domain} · {a.id} · {a.status}</option>)}
        </select>
        <button className="btn primary">Compare</button>
      </form>
      {error && <div className="banner error">{error}</div>}
      {result && (
        <article className="card glass">
          <h2>SEO Health</h2>
          <div className="split">
            <div><strong>{result.first.seoScore ?? '—'}/100</strong><span>Before · {result.first.url}</span></div>
            <div><strong>{result.second.seoScore ?? '—'}/100</strong><span>After · {result.second.url}</span></div>
          </div>
          <p className="muted">SEOlytics scores are audit health indicators, not official Google ranking scores.</p>
          <div className="comparison-grid">
            <div><strong>{result.issuesFixed}</strong><span>Issues Fixed</span></div>
            <div><strong>{result.remainingIssues}</strong><span>Remaining</span></div>
            <div><strong>{result.newIssues}</strong><span>New Issues</span></div>
            <div><strong>{result.pagesImproved}</strong><span>Pages Improved</span></div>
          </div>
          <div className="legend">
            {Object.entries(result.secondBySeverity || {}).map(([name, value]) => (
              <span key={name} className={`pill ${name.toLowerCase()}`}>{name}: {value}</span>
            ))}
          </div>
          <table className="table">
            <thead><tr><th>Issue type</th><th>First</th><th>Second</th><th>Trend</th></tr></thead>
            <tbody>
              {result.deltas.map((d) => (
                <tr key={d.issueType}>
                  <td>{d.issueType.replaceAll('_', ' ')}</td>
                  <td>{d.firstCount}</td>
                  <td>{d.secondCount}</td>
                  <td className={d.trend}>{d.trend}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </article>
      )}
    </div>
  );
}
