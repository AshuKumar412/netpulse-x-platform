import React from 'react';
import { Activity, LogOut, Radio, Shield, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useWebSocket } from '../../context/WebSocketContext';

export const Navbar = () => {
  const { user, logout } = useAuth();
  const { connectionStatus, sendPing } = useWebSocket();

  const wsStatusColor = {
    CONNECTED: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30',
    CONNECTING: 'text-amber-400 bg-amber-500/10 border-amber-500/30',
    DISCONNECTED: 'text-rose-400 bg-rose-500/10 border-rose-500/30',
  }[connectionStatus] || 'text-slate-400 bg-slate-500/10 border-slate-500/30';

  return (
    <header className="h-16 border-b border-[#1e2638] bg-[#0a0d14]/80 backdrop-blur sticky top-0 z-30 px-6 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-sky-500/10 border border-sky-500/30 text-sky-400">
          <Activity className="w-5 h-5" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-base font-bold tracking-tight text-white">NetPulse</span>
            <span className="px-1.5 py-0.5 text-[10px] font-extrabold uppercase bg-sky-600 text-white rounded">X</span>
          </div>
          <span className="text-[10px] text-slate-400 hidden sm:inline tracking-wider uppercase font-mono">
            Self-Healing Infrastructure & Traffic Routing
          </span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {/* WebSocket Real-time Status Badge */}
        <button
          onClick={() => sendPing('manual-test-ping')}
          title="Click to test WebSocket ping"
          className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-mono border transition-all ${wsStatusColor}`}
        >
          <Radio className="w-3.5 h-3.5 animate-pulse" />
          <span className="hidden md:inline">GATEWAY:</span>
          <span>{connectionStatus}</span>
        </button>

        {/* User Info & Role */}
        {user && (
          <div className="flex items-center gap-3 pl-4 border-l border-[#1e2638]">
            <div className="text-right hidden sm:block">
              <p className="text-xs font-semibold text-slate-200">{user.name}</p>
              <div className="flex items-center justify-end gap-1 mt-0.5">
                <Shield className="w-3 h-3 text-sky-400" />
                <span className="text-[10px] font-mono uppercase tracking-wider text-sky-400 font-bold">
                  {user.role}
                </span>
              </div>
            </div>

            <button
              onClick={logout}
              title="Sign Out"
              className="p-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
};
