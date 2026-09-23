export default function MethodologyPage() {
  return (
    <div className="page narrow">
      <h1>SEO scoring methodology</h1>
      <article className="card glass">
        <p>SEOlytics starts every audit at <strong>100</strong> and deducts points from technical issues found during the crawl. This is an internal health score for prioritising fixes. It is <strong>not</strong> Google’s ranking algorithm and does not predict search positions.</p>
        <ul className="plain">
          <li>Critical issue types: 8 points each, plus a small extra per additional occurrence (capped).</li>
          <li>High: 5 points per type.</li>
          <li>Medium: 2 points per type.</li>
          <li>Low: 1 point per type.</li>
        </ul>
        <p>Weights are configurable in <code>application.yml</code> under <code>seolytics.scoring</code>.</p>
        <h2>What we check</h2>
        <p>Titles, meta descriptions, canonicals, H1s, viewport, HTTPS, image alt text, HTTP statuses, redirect chains, robots.txt, sitemap.xml, broken links (sampled per page), and common GA4 / gtag / GTM snippets.</p>
        <p>GA4 detection only means a measurement ID or tag snippet appeared in HTML. It does not confirm that hits are received in Google Analytics.</p>
      </article>
    </div>
  );
}
