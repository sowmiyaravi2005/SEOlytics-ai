import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Loader2 } from 'lucide-react';
import { useAuth } from '../context/useAuth.js';
import AuthShell from '../components/AuthShell.jsx';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to run crawls and open your audit history.">
      <form className="form" onSubmit={onSubmit}>
        {error && <div className="banner error">{error}</div>}
        <label><span>Email</span><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" placeholder="alex@company.com" required /></label>
        <label><span>Password</span><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" placeholder="Your password" required minLength={8} /></label>
        <button className="btn primary submit-btn" disabled={busy}>
          {busy ? <Loader2 className="spin" size={18} /> : <ArrowRight size={18} />}
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
        <p className="auth-switch">Need an account? <Link to="/register">Create one</Link></p>
      </form>
    </AuthShell>
  );
}
