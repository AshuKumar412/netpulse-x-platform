import React, { createContext, useContext, useEffect, useState, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
  const { isAuthenticated, token } = useAuth();
  const [connectionStatus, setConnectionStatus] = useState('DISCONNECTED'); // CONNECTING, CONNECTED, DISCONNECTED
  const [lastMessage, setLastMessage] = useState(null);
  const [topologyUpdate, setTopologyUpdate] = useState(null);
  const [simulationUpdate, setSimulationUpdate] = useState(null);
  const [telemetryUpdate, setTelemetryUpdate] = useState(null);
  const [healthUpdate, setHealthUpdate] = useState(null);
  const [heartbeatUpdate, setHeartbeatUpdate] = useState(null);
  const [monitoringUpdate, setMonitoringUpdate] = useState(null);
  const [routingUpdate, setRoutingUpdate] = useState(null);
  const [failoverUpdate, setFailoverUpdate] = useState(null);
  const [chaosUpdate, setChaosUpdate] = useState(null);
  const [schedulerUpdate, setSchedulerUpdate] = useState(null);
  const [whatIfUpdate, setWhatIfUpdate] = useState(null);
  const [predictionUpdate, setPredictionUpdate] = useState(null);
  const [systemMetricsUpdate, setSystemMetricsUpdate] = useState(null);
  const clientRef = useRef(null);

  useEffect(() => {
    if (!isAuthenticated) {
      if (clientRef.current) {
        clientRef.current.deactivate();
        clientRef.current = null;
      }
      setConnectionStatus('DISCONNECTED');
      return;
    }

    setConnectionStatus('CONNECTING');

    const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnectionStatus('CONNECTED');

        // Status ping topic
        client.subscribe('/topic/status', (message) => {
          try {
            setLastMessage(JSON.parse(message.body));
          } catch (e) {
            setLastMessage(message.body);
          }
        });

        // Topology updates
        client.subscribe('/topic/topology', (message) => {
          try {
            setTopologyUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Simulation updates
        client.subscribe('/topic/simulation', (message) => {
          try {
            setSimulationUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 3: Telemetry updates
        client.subscribe('/topic/telemetry', (message) => {
          try {
            setTelemetryUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 3: Health updates
        client.subscribe('/topic/health', (message) => {
          try {
            setHealthUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 3: Heartbeat updates
        client.subscribe('/topic/heartbeat', (message) => {
          try {
            setHeartbeatUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 3: Monitoring lifecycle updates
        client.subscribe('/topic/monitoring', (message) => {
          try {
            setMonitoringUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 4: Traffic Routing decisions
        client.subscribe('/topic/routing', (message) => {
          try {
            setRoutingUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 5: Self-Healing Failover events
        client.subscribe('/topic/failover', (message) => {
          try {
            setFailoverUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 6: Chaos Engineering events
        client.subscribe('/topic/chaos', (message) => {
          try {
            setChaosUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 7: OS CPU Scheduler events
        client.subscribe('/topic/scheduler', (message) => {
          try {
            setSchedulerUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Phase 8: What-If Infrastructure Simulator events
        client.subscribe('/topic/what-if', (message) => {
          try {
            setWhatIfUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Predictive Congestion Detection & ML Intelligence events
        client.subscribe('/topic/predictions', (message) => {
          try {
            setPredictionUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });

        // Host Machine Live Discovery & Metrics updates
        client.subscribe('/topic/system-metrics', (message) => {
          try {
            setSystemMetricsUpdate(JSON.parse(message.body));
          } catch (e) {
            // Ignore malformed payloads
          }
        });
      },
      onDisconnect: () => {
        setConnectionStatus('DISCONNECTED');
      },
      onStompError: (frame) => {
        console.error('STOMP protocol error:', frame.headers['message']);
        setConnectionStatus('DISCONNECTED');
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [isAuthenticated, token]);

  const sendPing = (msg = 'ping') => {
    if (clientRef.current && connectionStatus === 'CONNECTED') {
      clientRef.current.publish({
        destination: '/app/ping',
        body: JSON.stringify({ message: msg }),
      });
    }
  };

  return (
    <WebSocketContext.Provider
      value={{
        connectionStatus,
        lastMessage,
        topologyUpdate,
        simulationUpdate,
        telemetryUpdate,
        healthUpdate,
        heartbeatUpdate,
        monitoringUpdate,
        routingUpdate,
        failoverUpdate,
        chaosUpdate,
        schedulerUpdate,
        whatIfUpdate,
        predictionUpdate,
        systemMetricsUpdate,
        sendPing,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};


export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};

export default WebSocketContext;
