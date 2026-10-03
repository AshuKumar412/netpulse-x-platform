import React from 'react';

export const StatusBadge = ({ status }) => {
  const statusConfig = {
    HEALTHY: {
      bg: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400',
      dot: 'bg-emerald-400',
      label: 'HEALTHY',
    },
    WARNING: {
      bg: 'bg-amber-500/10 border-amber-500/30 text-amber-400',
      dot: 'bg-amber-400',
      label: 'WARNING',
    },
    CONGESTED: {
      bg: 'bg-orange-500/10 border-orange-500/30 text-orange-400',
      dot: 'bg-orange-400',
      label: 'CONGESTED',
    },
    FAILED: {
      bg: 'bg-rose-500/10 border-rose-500/30 text-rose-400',
      dot: 'bg-rose-400',
      label: 'FAILED',
    },
    OFFLINE: {
      bg: 'bg-slate-500/10 border-slate-500/30 text-slate-400',
      dot: 'bg-slate-400',
      label: 'OFFLINE',
    },
  };

  const config = statusConfig[status] || statusConfig.OFFLINE;

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold tracking-wide border ${config.bg}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
};
