import api from './api';

/**
 * Phase 5 — Self-Healing Failover API service.
 *
 * All data originates from the backend PostgreSQL database and in-memory
 * failover state machine. This service NEVER generates or fakes any data.
 */
export const failoverService = {
  /**
   * GET /api/failover/status
   * Returns platform status, active failovers count, recovery metrics.
   * recoverySuccessRate will be null when no completed attempts exist.
   */
  getStatus() {
    return api.get('/api/failover/status').then((res) => res.data);
  },

  /**
   * GET /api/failover/events?page={page}&size={size}
   * Paginated failover events history, sorted by startedAt DESC.
   */
  getEvents(page = 0, size = 20) {
    return api.get('/api/failover/events', { params: { page, size } }).then((res) => res.data);
  },

  /**
   * GET /api/failover/events/{eventId}
   * Returns a single failover event by its eventId.
   */
  getEventById(eventId) {
    return api.get(`/api/failover/events/${eventId}`).then((res) => res.data);
  },

  /**
   * GET /api/failover/active
   * Returns all currently active failover/recovery operations.
   */
  getActiveEvents() {
    return api.get('/api/failover/active').then((res) => res.data);
  },

  /**
   * GET /api/failover/policies
   * Returns the current failover policy configuration.
   */
  getPolicies() {
    return api.get('/api/failover/policies').then((res) => res.data);
  },

  /**
   * POST /api/failover/recovery/{nodeId}
   * Initiates a controlled manual recovery for a specific node.
   * Requires ADMIN or OPERATOR role.
   */
  initiateRecovery(nodeId, reason = null, force = false) {
    return api
      .post(`/api/failover/recovery/${nodeId}`, { reason, force })
      .then((res) => res.data);
  },

  /**
   * POST /api/failover/retry/{eventId}
   * Retries a failed recovery operation.
   * Requires ADMIN or OPERATOR role.
   */
  retryRecovery(eventId) {
    return api.post(`/api/failover/retry/${eventId}`).then((res) => res.data);
  },
};

export default failoverService;
