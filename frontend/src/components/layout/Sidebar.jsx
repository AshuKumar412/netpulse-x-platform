import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Server,
  HeartPulse,
  GitFork,
  Cpu,
  Zap,
  Route as RouteIcon,
  HelpCircle,
  BrainCircuit,
  Activity,
  HardDrive,
} from 'lucide-react';

export const Sidebar = () => {
  const activeClass =
    'flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 shadow-sm shadow-cyan-500/5';
  const inactiveClass =
    'flex items-center gap-3 px-3.5 py-2.5 rounded-lg text-sm font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors';

  return (
    <aside className="w-64 border-r border-[#1e2638] bg-[#0d111a] flex flex-col justify-between hidden md:flex min-h-[calc(100vh-4rem)]">
      <div className="p-4 space-y-6">

        {/* Live System & Infrastructure */}
        <div>
          <span className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 font-mono">
            Live System & Hardware
          </span>
          <nav className="mt-2 space-y-1">
            <NavLink
              to="/system"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <HardDrive className="w-4 h-4 text-cyan-400" />
              <span>System Overview</span>
            </NavLink>

            <NavLink
              to="/dashboard"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>Live Monitoring</span>
            </NavLink>

            <NavLink
              to="/topology"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <GitFork className="w-4 h-4 text-emerald-400" />
              <span>Network Topology</span>
            </NavLink>

            <NavLink
              to="/nodes"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <Server className="w-4 h-4" />
              <span>Infrastructure Nodes</span>
            </NavLink>
          </nav>
        </div>

        {/* Traffic & Resilience Operations */}
        <div>
          <span className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 font-mono">
            Operations & Control
          </span>
          <nav className="mt-2 space-y-1">
            <NavLink
              to="/routing"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <RouteIcon className="w-4 h-4 text-sky-400" />
              <span>Traffic Management</span>
            </NavLink>

            <NavLink
              to="/failover"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <HeartPulse className="w-4 h-4 text-rose-400" />
              <span>Recovery Operations</span>
            </NavLink>

            <NavLink
              to="/intelligence"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <BrainCircuit className="w-4 h-4 text-purple-400" />
              <span>Predictive Intelligence</span>
            </NavLink>
          </nav>
        </div>

        {/* Simulation & Engineering */}
        <div>
          <span className="px-3 text-[11px] font-bold uppercase tracking-wider text-slate-400 font-mono">
            Simulation & Testing
          </span>
          <nav className="mt-2 space-y-1">
            <NavLink
              to="/what-if"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <HelpCircle className="w-4 h-4 text-amber-400" />
              <span>Scenario Simulator</span>
            </NavLink>

            <NavLink
              to="/chaos"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <Zap className="w-4 h-4 text-red-400" />
              <span>Fault Testing</span>
            </NavLink>

            <NavLink
              to="/scheduler"
              className={({ isActive }) => (isActive ? activeClass : inactiveClass)}
            >
              <Cpu className="w-4 h-4 text-indigo-400" />
              <span>CPU & Process Engine</span>
            </NavLink>
          </nav>
        </div>
      </div>

      {/* System Status Footprint */}
      <div className="p-4 border-t border-[#1e2638]">
        <div className="px-3 py-2 rounded-lg bg-slate-900/60 border border-slate-800/80 flex items-center justify-between">
          <div>
            <p className="text-[11px] font-semibold text-cyan-400">NetPulse X Platform</p>
            <p className="text-[10px] text-slate-400 mt-0.5">Autonomous Self-Healing</p>
          </div>
          <span className="flex h-2 w-2 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
          </span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
