import React from 'react';

export const LoadingState = ({ message = 'Loading infrastructure state...' }) => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      <div className="relative flex items-center justify-center">
        <div className="w-12 h-12 rounded-full border-2 border-slate-800 border-t-sky-500 animate-spin" />
        <div className="absolute w-6 h-6 rounded-full bg-sky-500/20 animate-pulse" />
      </div>
      <p className="mt-4 text-sm font-medium text-slate-400">{message}</p>
    </div>
  );
};
