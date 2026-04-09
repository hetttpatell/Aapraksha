/**
 * Aapraksha Cloud Functions — Main Entry Point
 * 
 * Architecture:
 * - calculateDangerZones: Firestore onWrite trigger (Phase 1)
 *   Recalculates danger zone scores when SOS events change.
 *   
 * - generateSmartMessage: Callable function (Phase 5)
 *   Generates context-aware SOS messages via Gemini, with template caching.
 *   
 * - generateIncidentReport: Firestore onUpdate trigger (Phase 5)
 *   Generates incident reports when SOS status changes to RESOLVED.
 * 
 * - handleCheckInTimeout: Scheduled function (Phase 9)
 *   Runs every minute to check for expired check-ins with no response.
 * 
 * All functions stay within Firebase free tier limits:
 * - Danger scoring: ~1 invocation per SOS event (not scheduled)
 * - Smart messages: ~1 Gemini call per zone (cached for 6 hours)
 * - Reports: ~1 Gemini call per resolved SOS
 * - Check-in timeouts: ~1 invocation per minute (minimal cost)
 */

const { calculateDangerZones } = require("./calculateDangerZones");

// Phase 1: Danger Intelligence Layer
exports.onSOSCreated = calculateDangerZones.onSOSCreated;
exports.onSOSUpdated = calculateDangerZones.onSOSUpdated;

// Phase 5: Smart Messaging & Incident Reports
const { generateSmartMessage } = require("./generateSmartMessage");
const { generateIncidentReport } = require("./generateIncidentReport");

exports.getSmartMessage = generateSmartMessage;
exports.onIncidentResolved = generateIncidentReport;

// Phase 9: Check-In System
const { handleCheckInTimeout } = require("./handleCheckInTimeout");

exports.checkInTimeoutHandler = handleCheckInTimeout;

