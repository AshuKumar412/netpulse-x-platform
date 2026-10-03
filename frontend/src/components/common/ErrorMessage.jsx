import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

export const ErrorMessage = ({
  title = 'System Error',
  message = 'Failed to load requested data',
  onRetry,
}) => {
  return (
    <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-6 text-center max-w-lg mx-auto">
      <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-rose-500/20 text-rose-400 mb-3">
        <AlertTriangle className="w-6 h-6" />
      </div>
      <h4 className="text-base font-semibold text-rose-200">{title}</h4>
      <p className="mt-1 text-sm text-rose-300/80">{message}</p>
      {onRetry && (
        <div className="mt-4">
          <Button variant="secondary" size="sm" onClick={onRetry} icon={RefreshCw}>
            Retry Connection
          </Button>
        </div>
      )}
    </div>
  );
};
