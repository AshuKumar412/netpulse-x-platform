import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Activity, Mail, Lock, AlertCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';

export const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || '/dashboard';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email || !password) {
      setError('Please provide both email and password');
      return;
    }

    setLoading(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message || 'Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0a0d14] flex flex-col justify-center items-center p-4 sm:p-6">
      <div className="w-full max-w-md">
        {/* Brand Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-sky-500/10 border border-sky-500/30 text-sky-400 mb-4 shadow-lg shadow-sky-950/40">
            <Activity className="w-7 h-7" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white">
            NetPulse <span className="text-sky-500">X</span>
          </h1>
          <p className="mt-1.5 text-xs text-slate-400 font-mono tracking-wide uppercase">
            Self-Healing Network Infrastructure & Adaptive Traffic Routing
          </p>
        </div>

        {/* Login Card */}
        <div className="bg-[#111622] border border-[#1e2638] rounded-2xl p-6 sm:p-8 shadow-2xl shadow-black/50">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-white">Platform Sign In</h2>
            <p className="text-xs text-slate-400 mt-1">
              Enter your credentials to access the topology management console.
            </p>
          </div>

          {error && (
            <div className="mb-6 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-start gap-2.5 text-xs text-rose-300">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <Input
              id="email"
              type="email"
              label="Operator Email"
              placeholder="operator@netpulse.io"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              icon={Mail}
              required
            />

            <Input
              id="password"
              type="password"
              label="Security Password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              icon={Lock}
              required
            />

            <Button
              type="submit"
              variant="primary"
              size="md"
              loading={loading}
              className="w-full mt-2"
            >
              Authenticate & Enter
            </Button>
          </form>

          <div className="mt-6 pt-6 border-t border-[#1e2638] text-center">
            <p className="text-xs text-slate-400">
              Need access?{' '}
              <Link to="/register" className="text-sky-400 hover:text-sky-300 font-medium transition-colors">
                Register Operator Account
              </Link>
            </p>
          </div>
        </div>

        <div className="mt-6 text-center">
          <p className="text-[11px] text-slate-400 font-mono">
            NetPulse X Engine v1.0.0 (Phase 1 Foundation)
          </p>
        </div>
      </div>
    </div>
  );
};
