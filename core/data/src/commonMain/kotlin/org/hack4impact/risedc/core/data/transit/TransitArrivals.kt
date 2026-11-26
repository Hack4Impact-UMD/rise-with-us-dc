package org.hack4impact.risedc.core.data.transit

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.hack4impact.risedc.core.data.model.BusStop
import org.hack4impact.risedc.core.data.model.RailLine
import org.hack4impact.risedc.core.data.model.Station
import kotlin.time.Duration.Companion.seconds

sealed interface ArrivalsResult {
    data class Arrivals(val list: List<Arrival>, val fetchedAt: Instant, val stale: Boolean) : ArrivalsResult
    data class Unavailable(val reason: String) : ArrivalsResult
}

data class Arrival(
    val lineOrRoute: String,
    val destination: String,
    val minutes: Int,
    val status: ArrivalStatus,
)

enum class ArrivalStatus { ARRIVING, BOARDING, MINUTES }

class TransitArrivals(private val client: HttpClient, private val apiKey: String) {

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private val cache = mutableMapOf<String, Pair<String, Instant>>()
    private val lineOrder = mutableMapOf<String, List<String>>()

    private val transferCodes = mapOf(
        "A01" to "C01", "C01" to "A01",
        "B01" to "F01", "F01" to "B01",
        "B06" to "E06", "E06" to "B06",
        "D03" to "F03", "F03" to "D03",
    )

    private fun codesFor(station: Station): List<String> =
        listOfNotNull(station.code, transferCodes[station.code])

    suspend fun railArrivals(station: Station, line: RailLine, toward: Station): ArrivalsResult {
        val lineCode = line.name
        val order = stationOrder(lineCode) ?: return ArrivalsResult.Unavailable("Couldn't load line")
        val here = order.indexOfFirst { it in codesFor(station) }
        val there = order.indexOfFirst { it in codesFor(toward) }
        if (here == -1 || there == -1 || here == there) {
            return ArrivalsResult.Unavailable("Station not on this line")
        }

        val (body, fetchedAt, stale) = fetch("https://api.wmata.com/StationPrediction.svc/json/GetPrediction/${order[here]}")
            ?: return ArrivalsResult.Unavailable("Couldn't reach WMATA")

        val response = runCatching { json.decodeFromString<RailResponse>(body) }.getOrNull()
            ?: return ArrivalsResult.Unavailable("Bad response from WMATA")

        val trains = response.trains
            .filter { it.line == lineCode }
            .filter { train ->
                val dest = order.indexOf(train.destinationCode)
                dest != -1 && (if (there > here) dest > here else dest < here)
            }
            .mapNotNull { train ->
                when (train.min) {
                    "ARR" -> Arrival(train.line, train.destinationName, 0, ArrivalStatus.ARRIVING)
                    "BRD" -> Arrival(train.line, train.destinationName, 0, ArrivalStatus.BOARDING)
                    else -> train.min.toIntOrNull()?.let { Arrival(train.line, train.destinationName, it, ArrivalStatus.MINUTES) }
                }
            }
            .sortedBy { it.minutes }

        return ArrivalsResult.Arrivals(trains, fetchedAt, stale)
    }

    suspend fun busArrivals(stop: BusStop, route: String): ArrivalsResult {
        val (body, fetchedAt, stale) = fetch("https://api.wmata.com/NextBusService.svc/json/jPredictions?StopID=${stop.id}")
            ?: return ArrivalsResult.Unavailable("Couldn't reach WMATA")

        val response = runCatching { json.decodeFromString<BusResponse>(body) }.getOrNull()
            ?: return ArrivalsResult.Unavailable("Bad response from WMATA")

        val buses = response.predictions
            .filter { it.routeId.equals(route, ignoreCase = true) }
            .map {
                val status = if (it.minutes <= 0) ArrivalStatus.ARRIVING else ArrivalStatus.MINUTES
                Arrival(it.routeId, it.directionText, maxOf(it.minutes, 0), status)
            }
            .sortedBy { it.minutes }

        return ArrivalsResult.Arrivals(buses, fetchedAt, stale)
    }

    private suspend fun fetch(url: String): Triple<String, Instant, Boolean>? {
        val now = Clock.System.now()
        val cached = lock.withLock { cache[url] }
        if (cached != null && now - cached.second < 20.seconds) {
            return Triple(cached.first, cached.second, false)
        }
        return try {
            val response = client.get(url) { header("api_key", apiKey) }
            if (!response.status.isSuccess()) error("HTTP ${response.status}")
            val body = response.bodyAsText()
            lock.withLock { cache[url] = body to now }
            Triple(body, now, false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cached?.let { Triple(it.first, it.second, true) }
        }
    }

    private suspend fun stationOrder(lineCode: String): List<String>? {
        lineOrder[lineCode]?.let { return it }
        return try {
            val (linesBody) = fetch("https://api.wmata.com/Rail.svc/json/jLines") ?: return null
            val line = json.decodeFromString<LinesResponse>(linesBody).lines.firstOrNull { it.lineCode == lineCode } ?: return null
            val (pathBody) = fetch(
                "https://api.wmata.com/Rail.svc/json/jPath?FromStationCode=${line.startStationCode}&ToStationCode=${line.endStationCode}"
            ) ?: return null
            json.decodeFromString<PathResponse>(pathBody).path
                .sortedBy { it.seqNum }
                .map { it.stationCode }
                .also { lineOrder[lineCode] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
private data class RailResponse(@SerialName("Trains") val trains: List<Train> = emptyList())

@Serializable
private data class Train(
    @SerialName("Line") val line: String = "",
    @SerialName("DestinationCode") val destinationCode: String? = null,
    @SerialName("DestinationName") val destinationName: String = "",
    @SerialName("Min") val min: String = "",
)

@Serializable
private data class BusResponse(@SerialName("Predictions") val predictions: List<BusPrediction> = emptyList())

@Serializable
private data class BusPrediction(
    @SerialName("RouteID") val routeId: String,
    @SerialName("DirectionText") val directionText: String = "",
    @SerialName("Minutes") val minutes: Int,
)

@Serializable
private data class LinesResponse(@SerialName("Lines") val lines: List<LineInfo> = emptyList())

@Serializable
private data class LineInfo(
    @SerialName("LineCode") val lineCode: String,
    @SerialName("StartStationCode") val startStationCode: String,
    @SerialName("EndStationCode") val endStationCode: String,
)

@Serializable
private data class PathResponse(@SerialName("Path") val path: List<PathStation> = emptyList())

@Serializable
private data class PathStation(
    @SerialName("StationCode") val stationCode: String,
    @SerialName("SeqNum") val seqNum: Int,
)