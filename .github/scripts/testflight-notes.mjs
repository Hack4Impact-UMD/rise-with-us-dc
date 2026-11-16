// Sets the TestFlight "What to Test" note on a just-uploaded build.
// Run with `node testflight-notes.mjs` (Node 20+, no dependencies).
//
// Env: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_P8, BUNDLE_ID, BUILD_NUMBER, WHATS_NEW

import { createPrivateKey, sign } from "node:crypto";
import { setTimeout as sleep } from "node:timers/promises";

function required(name) {
  const value = process.env[name];
  if (!value) {
    console.error(`Missing env ${name}`);
    process.exit(1);
  }
  return value;
}

const keyID = required("ASC_KEY_ID");
const issuerID = required("ASC_ISSUER_ID");
const privateKey = createPrivateKey(required("ASC_KEY_P8"));
const bundleID = required("BUNDLE_ID");
const buildNumber = required("BUILD_NUMBER");
const whatsNew = required("WHATS_NEW").slice(0, 4000);
const locale = "en-US";

// Worth another try: the build isn't visible or is still processing, a rate
// limit, an Apple outage or a network failure. Anything else (a bad key, a
// missing role, a malformed request) fails the same way every time.
class Retryable extends Error {}
const retryableStatus = (status) => status === 409 || status === 429 || status >= 500;

// Tokens live at most 20 minutes, so make a fresh one per request.
function token() {
  const now = Math.floor(Date.now() / 1000);
  const encode = (value) => Buffer.from(JSON.stringify(value)).toString("base64url");
  const input = `${encode({ alg: "ES256", kid: keyID, typ: "JWT" })}.${encode({
    iss: issuerID, iat: now, exp: now + 600, aud: "appstoreconnect-v1",
  })}`;
  const signature = sign("sha256", Buffer.from(input), { key: privateKey, dsaEncoding: "ieee-p1363" });
  return `${input}.${signature.toString("base64url")}`;
}

async function api(method, path, { query = {}, body } = {}) {
  const url = new URL(`https://api.appstoreconnect.apple.com${path}`);
  for (const [name, value] of Object.entries(query)) url.searchParams.set(name, value);
  let response;
  try {
    response = await fetch(url, {
      method,
      headers: { Authorization: `Bearer ${token()}`, "Content-Type": "application/json" },
      body: body && JSON.stringify(body),
      signal: AbortSignal.timeout(60_000),
    });
  } catch (error) {
    throw new Retryable(`${method} ${path}: ${error.message}`);
  }
  const text = await response.text();
  if (!response.ok) {
    const message = `${method} ${path} -> ${response.status}: ${text}`;
    throw retryableStatus(response.status) ? new Retryable(message) : new Error(message);
  }
  return text ? JSON.parse(text) : {};
}

// filter[bundleId] isn't an exact match (it can return e.g. a ".dev" app too), so check it here.
async function findApp() {
  const { data = [] } = await api("GET", "/v1/apps", { query: { "filter[bundleId]": bundleID } });
  const app = data.find((candidate) => candidate.attributes?.bundleId === bundleID);
  if (!app) throw new Error(`No App Store Connect app with bundle ID ${bundleID}`);
  return app.id;
}

async function setNote(appID) {
  const { data: builds = [] } = await api("GET", "/v1/builds", {
    query: { "filter[app]": appID, "filter[version]": buildNumber, limit: "1" },
  });
  const buildID = builds[0]?.id;
  if (!buildID) throw new Retryable(`Build ${buildNumber} not visible yet`);

  const { data: localizations = [] } = await api("GET", `/v1/builds/${buildID}/betaBuildLocalizations`);
  const existing = localizations.find((localization) => localization.attributes?.locale === locale);
  if (existing) {
    await api("PATCH", `/v1/betaBuildLocalizations/${existing.id}`, {
      body: { data: { type: "betaBuildLocalizations", id: existing.id, attributes: { whatsNew } } },
    });
  } else {
    await api("POST", "/v1/betaBuildLocalizations", {
      body: {
        data: {
          type: "betaBuildLocalizations",
          attributes: { locale, whatsNew },
          relationships: { build: { data: { type: "builds", id: buildID } } },
        },
      },
    });
  }
}

// The build appears a few minutes after upload. Keep trying until the note sticks.
const deadline = Date.now() + 25 * 60_000;
let appID;
for (;;) {
  try {
    appID ??= await findApp();
    await setNote(appID);
    console.log(`Set What to Test on ${bundleID} build ${buildNumber}`);
    break;
  } catch (error) {
    if (!(error instanceof Retryable) || Date.now() >= deadline) {
      console.error(`Giving up: ${error.message}`);
      process.exit(1);
    }
    console.log(`${error.message}; retrying in 30s`);
    await sleep(30_000);
  }
}
