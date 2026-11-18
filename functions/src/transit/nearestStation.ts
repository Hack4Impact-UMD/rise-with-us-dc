/*
 * /functions/src/transit/nearestStation.ts
 *
 * Finds the WMATA transit station nearest to a given latitude and longitude.
 * Fetches station data from the WMATA API and calculates the distance to each
 * station to determine the closest one.
*/

import { getRailStations, Station } from "./wmata";
import { distanceInMiles } from "./distance";

export type NearestStation = {
  station: Station;
  distanceMiles: number;
};

/*
 * Returns a NearestStation given a latitude and longitude.
 * This function will call the WMATA API to receive station information.
*/
export async function findNearestStation(
  latitude: number,
  longitude: number
): Promise<NearestStation> {
  const stations = await getRailStations();

  if (stations.length === 0) {
    throw new Error("No WMATA stations found");
  }

  let nearestStation = stations[0];
  let nearestDistance = distanceInMiles(
    latitude,
    longitude,
    nearestStation.latitude,
    nearestStation.longitude
  );

  for (const station of stations.slice(1)) {
    const distance = distanceInMiles(
      latitude,
      longitude,
      station.latitude,
      station.longitude
    );

    if (distance < nearestDistance) {
      nearestStation = station;
      nearestDistance = distance;
    }
  }

  console.log(nearestStation)

  return {
    station: nearestStation,
    distanceMiles: nearestDistance,
  };
}
