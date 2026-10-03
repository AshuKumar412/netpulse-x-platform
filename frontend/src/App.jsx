import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { WebSocketProvider } from './context/WebSocketContext';
import { ProtectedRoute } from './components/routing/ProtectedRoute';
import { Layout } from './components/layout/Layout';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Nodes } from './pages/Nodes';
import { Topology } from './pages/Topology';
import { Routing } from './pages/Routing';
import { SelfHealing } from './pages/SelfHealing';
import { Chaos } from './pages/Chaos';
import { Scheduler } from './pages/Scheduler';
import { WhatIf } from './pages/WhatIf';
import { Intelligence } from './pages/Intelligence';

export function App() {
  return (
    <AuthProvider>
      <WebSocketProvider>
        <Router>
          <Routes>
            {/* Public Auth Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Protected Core Platform Routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="intelligence" element={<Intelligence />} />
              <Route path="what-if" element={<WhatIf />} />
              <Route path="nodes" element={<Nodes />} />
              <Route path="topology" element={<Topology />} />
              <Route path="routing" element={<Routing />} />
              <Route path="failover" element={<SelfHealing />} />
              <Route path="chaos" element={<Chaos />} />
              <Route path="scheduler" element={<Scheduler />} />
            </Route>

            {/* Fallback */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Router>
      </WebSocketProvider>
    </AuthProvider>
  );
}

export default App;
