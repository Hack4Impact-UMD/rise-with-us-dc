import { onRequest } from "firebase-functions/v2/https";

// Single HTTPS entry point; Hosting rewrites /api/** here (see ../firebase.json).
// Endpoints from the app spec get added as routes: /snap/*, /routes, /arrivals/*, /alerts.
// Keys (Gemini, Google Routes, WMATA) come from Secret Manager, never from the apps.
export const api = onRequest((req, res) => {
  if (req.method === "GET" && req.path === "/health") {
    res.json({ ok: true });
    return;
  }
  res.status(404).json({ error: "not found" });
});
