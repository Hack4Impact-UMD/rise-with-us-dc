// Sets the TestFlight "What to Test" note on a just-uploaded build.
// Run with `xcrun swift testflight-notes.swift`. Uses only Foundation + CryptoKit.
//
// Env: ASC_KEY_ID, ASC_ISSUER_ID, ASC_KEY_PATH, BUNDLE_ID, BUILD_NUMBER, WHATS_NEW

import CryptoKit
import Foundation

let env = ProcessInfo.processInfo.environment
func required(_ name: String) -> String {
    guard let value = env[name], !value.isEmpty else {
        FileHandle.standardError.write("Missing env \(name)\n".data(using: .utf8)!)
        exit(1)
    }
    return value
}

let keyID = required("ASC_KEY_ID")
let issuerID = required("ASC_ISSUER_ID")
let privateKey = try P256.Signing.PrivateKey(
    pemRepresentation: String(contentsOfFile: required("ASC_KEY_PATH"), encoding: .utf8)
)
let bundleID = required("BUNDLE_ID")
let buildNumber = required("BUILD_NUMBER")
let whatsNew = String(required("WHATS_NEW").prefix(4000))
let locale = "en-US"

struct APIError: Error, CustomStringConvertible {
    let description: String
}

func base64URL(_ data: Data) -> String {
    data.base64EncodedString()
        .replacingOccurrences(of: "+", with: "-")
        .replacingOccurrences(of: "/", with: "_")
        .replacingOccurrences(of: "=", with: "")
}

// Tokens live at most 20 minutes, so make a fresh one per request.
func token() throws -> String {
    let now = Int(Date().timeIntervalSince1970)
    let header = try JSONSerialization.data(withJSONObject: ["alg": "ES256", "kid": keyID, "typ": "JWT"])
    let claims = try JSONSerialization.data(withJSONObject: [
        "iss": issuerID, "iat": now, "exp": now + 600, "aud": "appstoreconnect-v1",
    ])
    let signingInput = base64URL(header) + "." + base64URL(claims)
    let signature = try privateKey.signature(for: Data(signingInput.utf8))
    return signingInput + "." + base64URL(signature.rawRepresentation)
}

func api(_ method: String, _ path: String, query: [String: String] = [:], body: [String: Any]? = nil) async throws -> [String: Any] {
    var components = URLComponents(string: "https://api.appstoreconnect.apple.com" + path)!
    if !query.isEmpty {
        components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
    }
    var request = URLRequest(url: components.url!)
    request.httpMethod = method
    request.setValue("Bearer \(try token())", forHTTPHeaderField: "Authorization")
    if let body {
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
    }
    let (data, response) = try await URLSession.shared.data(for: request)
    let status = (response as? HTTPURLResponse)?.statusCode ?? 0
    guard (200..<300).contains(status) else {
        throw APIError(description: "\(method) \(path) -> \(status): \(String(decoding: data, as: UTF8.self))")
    }
    return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
}

func items(_ json: [String: Any]) -> [[String: Any]] {
    json["data"] as? [[String: Any]] ?? []
}

guard let appID = items(try await api("GET", "/v1/apps", query: ["filter[bundleId]": bundleID])).first?["id"] as? String else {
    print("No App Store Connect app with bundle ID \(bundleID)")
    exit(1)
}

// The build appears a few minutes after upload. Keep trying until the note sticks.
let deadline = Date().addingTimeInterval(25 * 60)
while true {
    do {
        let builds = items(try await api("GET", "/v1/builds", query: [
            "filter[app]": appID, "filter[version]": buildNumber, "limit": "1",
        ]))
        guard let buildID = builds.first?["id"] as? String else {
            throw APIError(description: "Build \(buildNumber) not visible yet")
        }
        let existing = items(try await api("GET", "/v1/builds/\(buildID)/betaBuildLocalizations"))
            .first { ($0["attributes"] as? [String: Any])?["locale"] as? String == locale }
        if let localizationID = existing?["id"] as? String {
            _ = try await api("PATCH", "/v1/betaBuildLocalizations/\(localizationID)", body: ["data": [
                "type": "betaBuildLocalizations", "id": localizationID,
                "attributes": ["whatsNew": whatsNew],
            ]])
        } else {
            _ = try await api("POST", "/v1/betaBuildLocalizations", body: ["data": [
                "type": "betaBuildLocalizations",
                "attributes": ["locale": locale, "whatsNew": whatsNew],
                "relationships": ["build": ["data": ["type": "builds", "id": buildID]]],
            ]])
        }
        print("Set What to Test on \(bundleID) build \(buildNumber)")
        exit(0)
    } catch {
        guard Date() < deadline else {
            print("Giving up: \(error)")
            exit(1)
        }
        print("\(error); retrying in 30s")
        try await Task.sleep(nanoseconds: 30_000_000_000)
    }
}
