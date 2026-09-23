import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiJson } from '../api';

export default function HistoryPage() {
  const [audits, setAudits] = useState([]);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  async function load() {
    try {
      setAudits(await apiJson('/api/audits'));
    } catch (err) {
      setError(err.message);
    }
  }

  useEffect(() => { load(); }, []);

  async function remove(id) {
    if (!confirm('Delete this audit and its crawled data?')) return;
    try {
      setError('');
      await apiJson(`/api/audits/${id}`, { method: 'DELETE' });
      setNotice('Audit deleted.');
      load();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="page">
      <h1>Audit history</h1>
      <p>Each user only sees their own crawls. Delete an audit to remove stored pages and issues.</p>
      {error && <div className="banner error">{error}</div>}
      {notice && <div className="banner success">{notice}</div>}
      {audits.length === 0 ? (
        <div className="empty glass"><h3>No history yet</h3><p>Start an audit to build a timeline for comparison.</p></div>
      ) : (
        <article className="card glass">
          <table className="table">
            <thead><tr><th>Website</th><th>Date</th><th>Status</th><th>Score</th><th></th></tr></thead>
            <tbody>
              {audits.map((audit) => (
                <tr key={audit.id}>
                  <td><Link to={`/audits/${audit.id}`}>{audit.domain}</Link></td>
                  <td>{(audit.createdAt || '').replace('T', ' ').slice(0, 19)}</td>
                  <td><span className={`pill ${audit.status.toLowerCase()}`}>{audit.status}</span></td>
                  <td>{audit.seoScore ?? '—'}</td>
                  <td><button className="btn danger" onClick={() => remove(audit.id)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </article>
      )}
    </div>
  );
}
