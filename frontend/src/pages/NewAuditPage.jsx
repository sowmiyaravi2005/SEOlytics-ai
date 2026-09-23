import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiJson } from '../api';

export default function NewAuditPage() {
  const navigate = useNavigate();
  const [url, setUrl] = useState('https://');
  const [maxPages, setMaxPages] = useState(20);
  const [maxDepth, setMaxDepth] = useState(2);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const audit = await apiJson('/api/audits', {
        method: 'POST',
        body: JSON.stringify({ url, maxPages: Number(maxPages), maxDepth: Number(maxDepth) })
      });
      navigate(`/audits/${audit.id}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page narrow">
      <h1>New website audit</h1>
      <p>SEOlytics crawls public pages only, respects robots.txt, and never scans localhost or private networks.</p>
      <form className="card glass form" onSubmit={onSubmit}>
        {error && <div className="banner error">{error}</div>}
        <label>Website URL
          <input value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://example.com" required />
        </label>
        <div className="form-row">
          <label>Max pages
            <input type="number" min="1" max="80" value={maxPages} onChange={(e) => setMaxPages(e.target.value)} />
          </label>
          <label>Max depth
            <input type="number" min="0" max="5" value={maxDepth} onChange={(e) => setMaxDepth(e.target.value)} />
          </label>
        </div>
        <p className="muted">Default delay is applied between requests. JavaScript rendering via Selenium is optional and disabled unless configured on the server.</p>
        <button className="btn primary" disabled={busy}>{busy ? 'Starting crawl…' : 'Start crawl'}</button>
      </form>
    </div>
  );
}
