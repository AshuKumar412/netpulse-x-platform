import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Server,
  FileCode2,
  HeartPulse,
  GitFork,
  Cpu,
  Zap,
  Lock,
  Route as RouteIcon,
  HelpCircle,
  BrainCircuit,
} from 'lucide-react';

export const Sidebar = () => {
  const activeClass =
    'flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 shadow-sm shadow-cyan-500/5';
  const inactiveClass =
    'flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors';

  return (
    <aside className="w-64 border-r border-[#1e2638] bg-[#0d111a] flex flex-col justify-between hidden md:flex min-h-[calc(100vh-4rem)]">
      <div className="p-4 space-y-6">

        {/* Core Platform Nav */}
        <div>
          <span className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 font-mono">
            Platform Core
          </span>
          <nav className="mt-2 space-y-1">
            <NavLink
              to="/dashboard"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>Telemetry Dashboard</span>
            </NavLink>

            <NavLink
              to="/intelligence"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <BrainCircuit className="w-4 h-4 text-violet-400" />
              <span>ML Intelligence</span>
            </NavLink>

            <NavLink
              to="/what-if"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <HelpCircle className="w-4 h-4 text-cyan-400" />
              <span>What-If Simulator</span>
            </NavLink>

            <NavLink
              to="/routing"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <RouteIcon className="w-4 h-4" />
              <span>Traffic Routing</span>
            </NavLink>

            <NavLink
              to="/failover"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <HeartPulse className="w-4 h-4" />
              <span>Self-Healing Failover</span>
            </NavLink>

            <NavLink
              to="/chaos"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <Zap className="w-4 h-4 text-rose-400" />
              <span>Chaos Engineering</span>
            </NavLink>

            <NavLink
              to="/scheduler"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <Cpu className="w-4 h-4 text-violet-400" />
              <span>OS CPU Scheduler</span>
            </NavLink>

            <NavLink
              to="/topology"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <GitFork className="w-4 h-4" />
              <span>Topology Graph</span>
            </NavLink>

            <NavLink
              to="/nodes"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <Server className="w-4 h-4" />
              <span>Node Management</span>
            </NavLink>
          </nav>
        </div>

        {/* Developer & API */}
        <div>
          <span className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 font-mono">
            Developer & API
          </span>
          <nav className="mt-2 space-y-1">
            <a
              href={`${import.meta.env.VITE_API_BASE_URL || ''}/swagger-ui.html`}
              target="_blank"
              rel="noreferrer"
              className={inactiveClass}
            >
              <FileCode2 className="w-4 h-4 text-sky-400" />
              <span>OpenAPI Swagger UI</span>
            </a>
            <a
              href={`${import.meta.env.VITE_API_BASE_URL || ''}/actuator/health`}
              target="_blank"
              rel="noreferrer"
              className={inactiveClass}
            >
              <HeartPulse className="w-4 h-4 text-emerald-400" />
              <span>Actuator Health Probe</span>
            </a>
          </nav>
        </div>
      </div>

      {/* Phase badge */}
      <div className="p-4 border-t border-[#1e2638]">
        <div className="px-3 py-2 rounded-lg bg-slate-900/60 border border-slate-800/80">
          <p className="text-[11px] font-semibold text-violet-400">Phase 9 Active</p>
          <p className="text-[10px] text-slate-400 mt-0.5">Predictive Congestion Detection & ML Engine</p>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
