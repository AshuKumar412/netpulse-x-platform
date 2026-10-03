import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Activity, User, Mail, Lock, CheckCircle2, AlertCircle, KeyRound, RotateCw } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { authService } from '../services/authService';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';

export const Register = () => {
  // Step 1 State: Registration
  const [step, setStep] = useState(1); // 1 = Registration Form, 2 = OTP Verification
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Step 2 State: OTP Verification
  const [otp, setOtp] = useState('');
  const [resendCooldown, setResendCooldown] = useState(60);
  const [resendLoading, setResendLoading] = useState(false);

  // Common UI State
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();
  const timerRef = useRef(null);

  // Countdown timer for OTP resend cooldown
  useEffect(() => {
    if (step === 2 && resendCooldown > 0) {
      timerRef.current = setInterval(() => {
        setResendCooldown((prev) => (prev > 0 ? prev - 1 : 0));
      }, 1000);
    }
    return () => clearInterval(timerRef.current);
  }, [step, resendCooldown]);

  const handleRegisterSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!name.trim() || !email.trim() || !password) {
      setError('All fields are required');
      return;
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters long');
      return;
    }

    if (confirmPassword && password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      await register(name, email, password);
      setSuccess('Verification code sent to your email.');
      setStep(2);
      setResendCooldown(60);
    } catch (err) {
      setError(err.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtpSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!otp.trim() || otp.trim().length !== 6) {
      setError('Please enter the complete 6-digit verification code');
      return;
    }

    setLoading(true);
    try {
      await authService.verifyEmail(email.trim(), otp.trim());
      setSuccess('Email verified successfully! Welcome to NetPulse X. Redirecting to login...');
      setTimeout(() => {
        navigate('/login');
      }, 2000);
    } catch (err) {
      setError(err.message || 'Verification failed. Please check your code and try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (resendCooldown > 0 || resendLoading) return;
    setError('');
    setSuccess('');
    setResendLoading(true);
    try {
      await authService.resendOtp(email.trim());
      setSuccess('A new verification code has been sent to your email.');
      setResendCooldown(60);
    } catch (err) {
      setError(err.message || 'Failed to resend code');
    } finally {
      setResendLoading(false);
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

        {/* Card */}
        <div className="bg-[#111622] border border-[#1e2638] rounded-2xl p-6 sm:p-8 shadow-2xl shadow-black/50">
          {step === 1 ? (
            <>
              <div className="mb-6">
                <h2 className="text-lg font-semibold text-white">Register Operator Account</h2>
                <p className="text-xs text-slate-400 mt-1">
                  Step 1 of 2: Create your identity. An email verification code will be dispatched.
                </p>
              </div>

              {error && (
                <div className="mb-6 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-start gap-2.5 text-xs text-rose-300">
                  <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <span>{error}</span>
                </div>
              )}

              <form onSubmit={handleRegisterSubmit} className="space-y-4">
                <Input
                  id="name"
                  type="text"
                  label="Full Name / Operator Handle"
                  placeholder="Alex Rivers"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  icon={User}
                  required
                />

                <Input
                  id="email"
                  type="email"
                  label="Corporate / Engineer Email"
                  placeholder="alex@netpulse.io"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  icon={Mail}
                  required
                />

                <Input
                  id="password"
                  type="password"
                  label="Password (min 6 characters)"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  icon={Lock}
                  required
                />

                <Input
                  id="confirmPassword"
                  type="password"
                  label="Confirm Password"
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  icon={Lock}
                />

                <div className="p-3 rounded-lg bg-slate-900/50 border border-slate-800 text-[11px] text-slate-400">
                  <span className="font-semibold text-slate-300">Email Verification:</span> A 6-digit OTP code will be sent to your email. Account activation is required before platform access is granted.
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  loading={loading}
                  className="w-full mt-2"
                >
                  Continue to Email Verification
                </Button>
              </form>
            </>
          ) : (
            <>
              <div className="mb-6">
                <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-sky-500/10 border border-sky-500/30 text-sky-400 text-[11px] font-mono mb-2">
                  <KeyRound className="w-3.5 h-3.5" /> Step 2: Email Verification
                </div>
                <h2 className="text-lg font-semibold text-white">Verify Your Email Address</h2>
                <p className="text-xs text-slate-400 mt-1">
                  We've sent a 6-digit verification code to <span className="text-sky-400 font-mono font-semibold">{email}</span>.
                </p>
              </div>

              {error && (
                <div className="mb-6 p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 flex items-start gap-2.5 text-xs text-rose-300 font-mono">
                  <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <span>{error}</span>
                </div>
              )}

              {success && (
                <div className="mb-6 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-start gap-2.5 text-xs text-emerald-300 font-mono">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <span>{success}</span>
                </div>
              )}

              <form onSubmit={handleVerifyOtpSubmit} className="space-y-5">
                <div>
                  <label className="block text-xs font-mono uppercase tracking-wider text-slate-400 mb-2">
                    6-Digit Verification Code
                  </label>
                  <input
                    id="otp"
                    type="text"
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value.replace(/[^0-9]/g, ''))}
                    placeholder="123456"
                    className="w-full text-center text-2xl tracking-[0.5em] font-mono py-3 bg-slate-950 border border-slate-700 rounded-xl text-white focus:outline-none focus:border-sky-500 focus:ring-1 focus:ring-sky-500"
                    autoFocus
                    required
                  />
                  <p className="text-[11px] text-slate-400 font-mono mt-2 text-center">
                    Code expires in 10 minutes. Maximum 5 attempts allowed.
                  </p>
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  size="md"
                  loading={loading}
                  disabled={!!success || otp.length !== 6}
                  className="w-full"
                >
                  Verify Email & Activate Account
                </Button>

                <div className="pt-2 flex flex-col items-center gap-2">
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={resendCooldown > 0 || resendLoading}
                    className={`inline-flex items-center gap-1.5 text-xs font-mono transition-colors ${
                      resendCooldown > 0
                        ? 'text-slate-400 cursor-not-allowed'
                        : 'text-sky-400 hover:text-sky-300 underline'
                    }`}
                  >
                    <RotateCw className={`w-3.5 h-3.5 ${resendLoading ? 'animate-spin' : ''}`} />
                    {resendCooldown > 0
                      ? `Resend available in ${resendCooldown}s`
                      : 'Resend Verification Code'}
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setStep(1);
                      setError('');
                      setSuccess('');
                    }}
                    className="text-[11px] text-slate-400 hover:text-slate-300 font-mono underline mt-1"
                  >
                    ← Change Email Address
                  </button>
                </div>
              </form>
            </>
          )}

          <div className="mt-6 pt-6 border-t border-[#1e2638] text-center">
            <p className="text-xs text-slate-400">
              Already have an account?{' '}
              <Link to="/login" className="text-sky-400 hover:text-sky-300 font-medium transition-colors">
                Sign In
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Register;
