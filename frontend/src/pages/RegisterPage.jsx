import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Loader2 } from 'lucide-react';
import { useAuth } from '../context/useAuth.js';
import AuthShell from '../components/AuthShell.jsx';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await register(fullName, email, password);
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell title="Create your workspace" subtitle="Register to start a technical SEO crawl on a public website.">
      <form className="form" onSubmit={onSubmit}>
        {error && <div className="banner error">{error}</div>}
        <label><span>Full name</span><input value={fullName} onChange={(e) => setFullName(e.target.value)} autoComplete="name" placeholder="Alex Morgan" required /></label>
        <label><span>Email</span><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" placeholder="alex@company.com" required /></label>
        <label><span>Password</span><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" placeholder="At least 8 characters" required minLength={8} /></label>
        <button className="btn primary submit-btn" disabled={busy}>
          {busy ? <Loader2 className="spin" size={18} /> : <ArrowRight size={18} />}
          {busy ? 'Creating account...' : 'Create account'}
        </button>
        <p className="auth-switch">Already registered? <Link to="/login">Sign in</Link></p>
      </form>
    </AuthShell>
  );
}
