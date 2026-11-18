/*
 * functions/src/transit/wmata.ts
 *
 * Fetches rail station information from the WMATA API and maps the response
 * into the backend's Station data model. Requires a configured WMATA API key.
 *
 * The station list is cached in memory (per function instance) because it
 * almost never changes and is read on every nearest-station request.
*/

const WMATA_BASE_URL = "https://api.wmata.com";

// Stations change very rarely, so a long TTL is fine.
export const STATIONS_CACHE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours
// If a refresh fails but we still hold stale data, wait this long before
// trying WMATA again so an outage doesn't add a timeout to every request.
export const STALE_RETRY_MS = 60 * 1000; // 1 minute
const FETCH_TIMEOUT_MS = 5000;

// Type for the backend data model
export type Station = {
  code: string;
  name: string;
  latitude: number;
  longitude: number;
  address: {
    street: string;
    city: string;
    state: string;
    zip: string;
  };
};

// Types for the API response
type WmataStation = {
  Code: string;
  Name: string;
  Lat: number;
  Lon: number;
  Address?: {
    Street?: string;
    City?: string;
    State?: string;
    Zip?: string;
  };
};

type WmataStationsResponse = {
  Stations: WmataStation[];
};

/*
 * Thrown when WMATA itself fails (bad status, timeout, malformed payload).
 * Lets the HTTP layer tell "upstream is down" (502) apart from our own bugs.
*/
export class WmataApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = "WmataApiError";
    this.status = status;
  }
}

let cache: { stations: Station[]; expiresAt: number } | null = null;
// Shared by concurrent callers so a cold/expired cache triggers ONE request.
let inFlight: Promise<Station[]> | null = null;

/*
 * Clears the in-memory cache. Mainly useful for tests.
*/
export function clearStationCache(): void {
  cache = null;
  inFlight = null;
}

/*
 * Will return a list of rail stations, served from the in-memory cache when
 * possible. On a cache miss it calls the WMATA API.
 *
 * - Concurrent callers during a refresh share a single request.
 * - If a refresh fails and stale data exists, the stale data is returned.
 * - Throws if an API key is not configured or if the request failed
 *   (and there is nothing cached to fall back on).
*/
export async function getRailStations(): Promise<Station[]> {
  if (cache && Date.now() < cache.expiresAt) {
    return cache.stations;
  }

  if (inFlight) {
    return inFlight;
  }

  const request = fetchRailStations()
    .then((stations) => {
      cache = { stations, expiresAt: Date.now() + STATIONS_CACHE_TTL_MS };
      return stations;
    })
    .catch((error) => {
      if (cache) {
        console.warn("WMATA refresh failed, serving stale stations:", error);
        cache = {
          stations: cache.stations,
          expiresAt: Date.now() + STALE_RETRY_MS,
        };
        return cache.stations;
      }
      throw error;
    })
    .finally(() => {
      // Only clear if a clearStationCache() call hasn't replaced us.
      if (inFlight === request) {
        inFlight = null;
      }
    });

  inFlight = request;
  return request;
}

/*
 * Calls the WMATA API (no caching).
*/
async function fetchRailStations(): Promise<Station[]> {
  const apiKey = process.env.WMATA_API_KEY;

  if (!apiKey) {
    throw new Error("WMATA_API_KEY is not configured");
  }

  let response: Response;
  try {
    response = await fetch(`${WMATA_BASE_URL}/Rail.svc/json/jStations`, {
      headers: {
        api_key: apiKey,
      },
      signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
    });
  } catch (error) {
    throw new WmataApiError(`WMATA API request failed: ${String(error)}`);
  }

  if (!response.ok) {
    throw new WmataApiError(
      `WMATA API request failed: ${response.status} ${response.statusText}`,
      response.status
    );
  }

  const data = (await response.json()) as Partial<WmataStationsResponse>;

  if (!data || !Array.isArray(data.Stations)) {
    throw new WmataApiError("WMATA API returned an unexpected response shape");
  }

  return data.Stations
    // A station with bad coordinates would silently break nearest-station math.
    .filter(
      (station) =>
        Number.isFinite(station.Lat) && Number.isFinite(station.Lon)
    )
    .map((station) => ({
      code: station.Code,
      name: station.Name,
      latitude: station.Lat,
      longitude: station.Lon,
      address: {
        street: station.Address?.Street ?? "",
        city: station.Address?.City ?? "",
        state: station.Address?.State ?? "",
        zip: station.Address?.Zip ?? "",
      },
    }));
}
