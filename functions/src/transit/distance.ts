/*
 * /functions/src/transit/distance.ts
 *
 * Calculates the straight-line distance between two latitude/longitude
 * coordinates using the Haversine formula. Distances are returned in miles.
*/

const EARTH_RADIUS_MILES = 3958.8;

/*
 * Use the Haversine formula to find the distance between two
 * (longitude, latitude) points. It will return the distance in
 * miles.
 *
 * Expects well-formed longitude/latitude.
*/
export function distanceInMiles(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const lat1Radians = (lat1 * Math.PI) / 180;
  const lat2Radians = (lat2 * Math.PI) / 180;

  const deltaLat = ((lat2 - lat1) * Math.PI) / 180;
  const deltaLon = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(deltaLat / 2) ** 2 +
    Math.cos(lat1Radians) *
      Math.cos(lat2Radians) *
      Math.sin(deltaLon / 2) ** 2;

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return EARTH_RADIUS_MILES * c;
}
