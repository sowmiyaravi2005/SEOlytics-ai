import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiBlob, apiJson } from '../api';

const SEVERITIES = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const TYPES = [
  '', 'MISSING_TITLE', 'EMPTY_TITLE', 'DUPLICATE_TITLE', 'MISSING_META_DESCRIPTION', 'DUPLICATE_META_DESCRIPTION',
  'BROKEN_INTERNAL_LINK', 'BROKEN_EXTERNAL_LINK', 'HTTP_CLIENT_ERROR', 'HTTP_SERVER_ERROR', 'REDIRECT_CHAIN',
  'MISSING_IMAGE_ALT', 'MISSING_CANONICAL', 'MISSING_H1', 'MULTIPLE_H1', 'MISSING_VIEWPORT', 'INSECURE_HTTP',
  'MISSING_SITEMAP', 'INVALID_SITEMAP', 'ROBOTS_TXT_INACCESSIBLE', 'GA4_TAG_NOT_DETECTED'
];

export default function AuditDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [detail, setDetail] = useState(null);
  const [progress, setProgress] = useState(null);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [severity, setSeverity] = useState('');
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const [openIssue, setOpenIssue] = useState(null);
  const [guideIssue, setGuideIssue] = useState(null);
  const [selectedPage, setSelectedPage] = useState(null);
  const [copied, setCopied] = useState('');

  const running = progress && (progress.status === 'PENDING' || progress.status === 'RUNNING');

  useEffect(() => {
    let timer;
    async function tick() {
      try {
        const p = await apiJson(`/api/audits/${id}/progress`);
        setProgress(p);
        if (p.status === 'PENDING' || p.status === 'RUNNING') {
          timer = setTimeout(tick, 1500);
        } else {
          const d = await apiJson(`/api/audits/${id}`);
          setDetail(d);
        }
      } catch (err) {
        setError(err.message);
      }
    }
    tick();
    return () => clearTimeout(timer);
  }, [id]);

  async function applyFilters(e) {
    e?.preventDefault();
    const params = new URLSearchParams();
    if (query) params.set('query', query);
    if (severity) params.set('severity', severity);
    if (type) params.set('type', type);
    if (status) params.set('status', status);
    try {
      setError('');
      const queryString = params.toString();
      const d = await apiJson(`/api/audits/${id}${queryString ? `?${queryString}` : ''}`);
      setDetail(d);
    } catch (err) {
      setError(err.message);
    }
  }

  async function refreshDetail() {
    try {
      setError('');
      const d = await apiJson(`/api/audits/${id}`);
      setDetail(d);
      return d;
    } catch (err) {
      setError(err.message);
      return null;
    }
  }

  async function updateStatus(issue, statusValue) {
    try {
      const updated = await apiJson(`/api/issues/${issue.id}/status`, {
        method: 'POST',
        body: JSON.stringify({ status: statusValue })
      });
      setDetail((current) => ({
        ...current,
        issues: current.issues.map((item) => item.id === updated.id ? updated : item)
      }));
      if (guideIssue?.id === updated.id) setGuideIssue(updated);
    } catch (err) {
      setError(err.message);
    }
  }

  async function recrawl() {
    try {
      const next = await apiJson(`/api/audits/${id}/recrawl`, { method: 'POST', body: JSON.stringify({}) });
      navigate(`/audits/${next.id}`);
    } catch (err) {
      setError(err.message);
    }
  }

  async function copyExample(issue) {
    if (!issue.exampleSolution) return;
    try {
      await navigator.clipboard.writeText(issue.exampleSolution);
      setCopied(String(issue.id));
      setTimeout(() => setCopied(''), 1800);
    } catch (err) {
      setError('Unable to copy the example fix to the clipboard.');
    }
  }

  async function download(kind) {
    try {
      const { blob, filename } = await apiBlob(`/api/audits/${id}/report.${kind}`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err.message);
    }
  }

  const summary = detail?.summary || progress;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Audit results</h1>
          <p>{summary?.url || 'Loading crawl…'}</p>
          {detail?.summary?.createdAt && <p>Crawl date: {String(detail.summary.createdAt).replace('T', ' ').slice(0, 19)} UTC</p>}
        </div>
        <div className="actions">
          <button className="btn" onClick={refreshDetail} disabled={!detail}>Refresh</button>
          <button className="btn primary" onClick={recrawl} disabled={!detail}>Re-Crawl Website</button>
          <button className="btn" onClick={() => download('html')} disabled={!detail}>HTML report</button>
          <button className="btn" onClick={() => download('pdf')} disabled={!detail}>PDF report</button>
        </div>
      </div>
      {error && <div className="banner error">{error}</div>}
      {running && (
        <article className="card glass">
          <h2>Crawl in progress</h2>
          <div className="progress"><span style={{ width: `${progress.progressPercent || 5}%` }} /></div>
          <p>{progress.progressMessage} · {progress.pagesCrawled || 0}/{progress.maxPages} pages</p>
        </article>
      )}
      {detail && (
        <>
          <section className="stat-grid">
            <article className="stat glass"><span>SEO score</span><strong>{detail.summary.seoScore ?? '—'}</strong></article>
            <article className="stat glass"><span>Pages crawled</span><strong>{detail.summary.pagesCrawled}</strong></article>
            <article className="stat glass"><span>Issues</span><strong>{detail.summary.issuesFound}</strong></article>
            <article className="stat glass"><span>Status</span><strong>{detail.summary.status}</strong></article>
          </section>
          {detail.summary.errorMessage && <div className="banner error">{detail.summary.errorMessage}</div>}
          <article className="card glass">
            <p className="muted">{detail.summary.scoringBreakdown}</p>
            <p className="muted">robots.txt: {String(detail.summary.robotsTxtAccessible)} · sitemap: {String(detail.summary.sitemapFound)} · GA4 snippet detected: {String(detail.summary.ga4TagDetected)}. Tag detection is not proof that analytics collection works.</p>
            <p className="muted">Workflow: detect, explain, suggest, user fixes, re-crawl, then verify. SEOlytics does not modify external websites by default.</p>
          </article>
          <form className="filters glass" onSubmit={applyFilters}>
            <input placeholder="Search URL or issue" value={query} onChange={(e) => setQuery(e.target.value)} />
            <select value={severity} onChange={(e) => setSeverity(e.target.value)}>{SEVERITIES.map((s) => <option key={s} value={s}>{s || 'All severities'}</option>)}</select>
            <select value={type} onChange={(e) => setType(e.target.value)}>{TYPES.map((s) => <option key={s} value={s}>{s || 'All types'}</option>)}</select>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="">All HTTP statuses</option>
              <option value="200">200</option>
              <option value="301">301</option>
              <option value="404">404</option>
              <option value="500">500</option>
            </select>
            <button className="btn">Filter</button>
          </form>
          <article className="card glass">
            <h2>Issues by severity</h2>
            <div className="legend">
              {Object.entries(detail.issuesBySeverity || {}).map(([k, v]) => <span key={k} className={`pill ${k.toLowerCase()}`}>{k}: {v}</span>)}
            </div>
            <table className="table">
              <thead><tr><th>Severity</th><th>Type</th><th>URL</th><th>Issue</th><th>Status</th><th>Fix</th></tr></thead>
              <tbody>
                {detail.issues.map((issue) => (
                  <tr key={issue.id} onClick={() => setOpenIssue(openIssue === issue.id ? null : issue.id)}>
                    <td><span className={`pill ${issue.severity.toLowerCase()}`}>{issue.severity}</span></td>
                    <td>{issue.issueType.replaceAll('_', ' ')}</td>
                    <td className="url">{issue.pageUrl}</td>
                    <td>
                      <strong>{issue.title}</strong>
                      {openIssue === issue.id && (
                        <div className="expand">
                          <p>{issue.description}</p>
                          <p><b>Fix:</b> {issue.recommendation}</p>
                        </div>
                      )}
                    </td>
                    <td><span className={`pill ${String(issue.status || 'OPEN').toLowerCase()}`}>{issue.status || 'OPEN'}</span></td>
                    <td><button className="btn small" type="button" onClick={(event) => { event.stopPropagation(); setGuideIssue(issue); }}>{'How to Fix'}</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {detail.issues.length === 0 && <p className="muted">No issues match the current filters.</p>}
          </article>
          <article className="card glass fix-center">
            <div className="section-head">
              <div>
                <h2>SEO Fix Center</h2>
                <p className="muted">Actionable guidance from the crawl results. Suggested fixes are not applied automatically.</p>
              </div>
              <button className="btn primary" onClick={recrawl}>Re-Crawl Website</button>
            </div>
            <div className="fix-grid">
              <div className="fix-list">
                {groupIssues(detail.issues).map((group) => (
                  <button key={group.key} className={`fix-item ${group.severity.toLowerCase()} ${guideIssue?.id === group.first.id ? 'active' : ''}`} onClick={() => setGuideIssue(group.first)}>
                    <span className={`pill ${group.severity.toLowerCase()}`}>{group.severity}</span>
                    <strong>{group.title}</strong>
                    <small>{group.count} affected page{group.count === 1 ? '' : 's'}</small>
                  </button>
                ))}
                {detail.issues.length === 0 && <p className="muted">No open guidance for the current filters.</p>}
              </div>
              <div className="fix-guide">
                {guideIssue ? (
                  <>
                    <div className="guide-title">
                      <div>
                        <span className={`pill ${guideIssue.severity.toLowerCase()}`}>{guideIssue.severity}</span>
                        <h3>{guideIssue.title}</h3>
                      </div>
                      <select value={guideIssue.status || 'OPEN'} onChange={(e) => updateStatus(guideIssue, e.target.value)}>
                        <option value="OPEN">Open</option>
                        <option value="FIXED">Mark fixed by user</option>
                        <option value="IGNORED">Ignored</option>
                        <option value="NEEDS_REVIEW">Needs Review</option>
                      </select>
                    </div>
                    <GuideBlock title="Problem" text={guideIssue.explanation || guideIssue.description} />
                    <GuideBlock title="Why It Matters" text={guideIssue.whyItMatters} />
                    <div className="guide-block">
                      <h4>Affected Pages</h4>
                      <ol className="affected-list">
                        {affectedPages(detail.issues, guideIssue, detail.pages).map((item) => (
                          <li key={`${item.pageId || 'site'}-${item.url}`}>
                            <button type="button" className="link-button" onClick={() => item.page && setSelectedPage(item.page)}>{item.url}</button>
                          </li>
                        ))}
                      </ol>
                    </div>
                    <GuideBlock title="Detected Value" text={guideIssue.detectedValue || guideIssue.description} pre />
                    <GuideBlock title="Expected Value" text={guideIssue.expectedValue} />
                    <GuideBlock title="How to Fix" text={guideIssue.howToFix || guideIssue.recommendation} />
                    {guideIssue.exampleSolution && (
                      <div className="guide-block">
                        <div className="section-head compact">
                          <h4>Example</h4>
                          <button className="btn" onClick={() => copyExample(guideIssue)}>{copied === String(guideIssue.id) ? 'Copied' : 'Copy Code'}</button>
                        </div>
                        <pre>{guideIssue.exampleSolution}</pre>
                      </div>
                    )}
                    <GuideBlock title="Verify" text={guideIssue.verificationMethod} />
                    <p className="muted">Status meaning: suggested fix is guidance, user-applied fix means you marked it, automatically verified fix means a later crawl marks it fixed.</p>
                  </>
                ) : (
                  <p className="muted">Choose an issue to open its fix guide.</p>
                )}
              </div>
            </div>
          </article>
          <article className="card glass">
            <h2>Crawled pages</h2>
            <table className="table">
              <thead><tr><th>URL</th><th>Status</th><th>Title</th><th>H1</th><th>HTTPS</th></tr></thead>
              <tbody>
                {detail.pages.map((page) => (
                  <tr key={page.id} onClick={() => setSelectedPage(page)}>
                    <td className="url">{page.url}</td>
                    <td>{page.statusCode}</td>
                    <td>{page.title || '—'}</td>
                    <td>{page.h1Count}</td>
                    <td>{page.https ? 'Yes' : 'No'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </article>
          {selectedPage && (
            <article className="card glass">
              <h2>Page SEO details</h2>
              <p className="url">{selectedPage.url}</p>
              <dl className="meta-grid">
                <div><dt>Status</dt><dd>{selectedPage.statusCode}</dd></div>
                <div><dt>Redirect hops</dt><dd>{selectedPage.redirectHops}</dd></div>
                <div><dt>Title</dt><dd>{selectedPage.title || '—'}</dd></div>
                <div><dt>Meta description</dt><dd>{selectedPage.metaDescription || '—'}</dd></div>
                <div><dt>Canonical</dt><dd>{selectedPage.canonical || '—'}</dd></div>
                <div><dt>H1 text</dt><dd>{selectedPage.h1Text || '—'}</dd></div>
                <div><dt>Images missing alt</dt><dd>{selectedPage.imagesMissingAlt}</dd></div>
                <div><dt>Viewport</dt><dd>{String(selectedPage.hasViewport)}</dd></div>
                <div><dt>GA4 snippet</dt><dd>{String(selectedPage.ga4TagDetected)}</dd></div>
              </dl>
              <h3>Issues on this page</h3>
              <ul className="plain">
                {detail.issues.filter((issue) => issue.pageId === selectedPage.id).map((issue) => (
                  <li key={issue.id}><strong>{issue.title}</strong> — {issue.recommendation}</li>
                ))}
              </ul>
              {detail.issues.filter((issue) => issue.pageId === selectedPage.id).length === 0 && (
                <p className="muted">No filtered issues are attached to this page.</p>
              )}
            </article>
          )}
        </>
      )}
    </div>
  );
}

function groupIssues(issues) {
  const groups = new Map();
  for (const issue of issues) {
    const key = issue.issueType;
    const group = groups.get(key) || {
      key,
      title: issue.title,
      severity: issue.severity,
      count: 0,
      first: issue
    };
    group.count += 1;
    groups.set(key, group);
  }
  return [...groups.values()].sort((a, b) => severityRank(a.severity) - severityRank(b.severity));
}

function severityRank(severity) {
  return { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 }[severity] ?? 4;
}

function affectedPages(issues, selected, pages) {
  const pageById = new Map((pages || []).map((page) => [page.id, page]));
  return issues
    .filter((issue) => issue.issueType === selected.issueType)
    .map((issue) => ({ url: issue.pageUrl, pageId: issue.pageId }))
    .filter((item) => Boolean(item.url))
    .map((item) => ({ ...item, page: pageById.get(item.pageId) || null }));
}

function GuideBlock({ title, text, pre }) {
  if (!text) return null;
  return (
    <div className="guide-block">
      <h4>{title}</h4>
      {pre ? <pre>{text}</pre> : <p>{text}</p>}
    </div>
  );
}
