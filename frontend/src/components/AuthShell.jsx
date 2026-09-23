import { Activity, FileText, Radar, ShieldCheck, Sparkles } from 'lucide-react';

export default function AuthShell({ title, subtitle, children }) {
  return (
    <div className="auth-screen split-auth">
      <section className="auth-hero">
        <div className="brand auth-brand">
          <Sparkles size={24} />
          <div><strong>SEOlytics</strong><span>Automated technical SEO auditor</span></div>
        </div>
        <h1>Find crawl, index, and on-page issues before they cost traffic.</h1>
        <div className="auth-highlights">
          <span><Radar size={18} /> Bounded crawls</span>
          <span><ShieldCheck size={18} /> SSRF-safe checks</span>
          <span><FileText size={18} /> Exportable reports</span>
        </div>
        <div className="auth-proof">
          <Activity size={20} />
          <div>
            <strong>Audit signal in minutes</strong>
            <p>Track health scores, technical issues, page metadata, and crawl progress from one workspace.</p>
          </div>
        </div>
      </section>
      <section>
        <div className="auth-card">
          <h2>{title}</h2>
          <p className="muted">{subtitle}</p>
          {children}
        </div>
      </section>
    </div>
  );
}
