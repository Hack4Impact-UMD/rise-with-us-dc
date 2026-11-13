import { onRequest } from "firebase-functions/v2/https";

import { findNearestStation } from "./transit/nearestStation"

// Single HTTPS entry point; Hosting rewrites /api/** here (see ../firebase.json).
// Endpoints from the app spec get added as routes: /snap/*, /routes, /arrivals/*, /alerts.
// Keys (Gemini, Google Routes, WMATA) come from Secret Manager, never from the apps.
export const api = onRequest(async (req, res) => {
  if (req.method === "GET" && req.path === "/health") {
    res.json({ ok: true });
    return;
  }

  // /transit/nearest?lat=___&lon=___
  // Get the nearest station for a given latitude/longitude.
  if (req.method === "GET" && req.path === "/transit/nearest") {
    try {
      const latitude = Number(req.query.lat);
      const longitude = Number(req.query.lon);

      // Input Validation
      if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
        res.status(400).json({
          error: "lat and lon must be valid numbers",
        });
        return;
      }

      if (latitude < -90 || latitude > 90) {
        res.status(400).json({
          error: "latitude must be between -90 and 90",
        });
        return;
      }

      if (longitude < -180 || longitude > 180) {
        res.status(400).json({
          error: "longitude must be between -180 and 180",
        });
        return;
      }

      const nearest = await findNearestStation(latitude, longitude);

      res.json(nearest);
    } catch (error) {
      console.error(error);
      res.status(500).json({
        error: "Failed to find nearest station",
      });
    }

    return;
  }
  res.status(404).json({ error: "not found" });
});
