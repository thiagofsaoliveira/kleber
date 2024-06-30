package io.thiagofsaoliveira.kleber.spotify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.thiagofsaoliveira.kleber.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SpotifyManager {

    private static final Logger log =
            LoggerFactory.getLogger(SpotifyManager.class);

    private static final String TOKEN_ENDPOINT =
            "https://accounts.spotify.com/api/token";
    private static final String TRACK_ENDPOINT =
            "https://api.spotify.com/v1/tracks/";
    private static final String FORM = "grant_type=client_credentials";
    private static final String CONTENT_TYPE =
            "application/x-www-form-urlencoded";

    private final String authorization;
    private final Map<String, AudioTrackData> cache = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    private String accessToken;

    public static SpotifyManager newSpotifyManager(Configuration config) {
        String value = "%s:%s".formatted(
                config.getClientId(),
                config.getClientSecret());
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedValue = encoder.encodeToString(value.getBytes());
        String authorization = "Basic %s".formatted(encodedValue);
        return new SpotifyManager(authorization);
    }

    public SpotifyManager(String authorization) {
        this.authorization = authorization;
        accessToken = "";
    }

    public AudioTrackData getDataFor(String id) {
        if (cache.containsKey(id)) {
            log.debug("Fetching data from cache for id: {}", id);
            return cache.get(id);
        }

        try (var client = HttpClient.newHttpClient()) {
            log.debug("Fetching data for id: {}", id);
            HttpResponse<String> response = getAudioTrackData(client, id);

            if (response.statusCode() == 200) {
                log.debug("Data fetched");
                var data = mapToAudioTrackData(response.body());
                cache.put(id, data);
                return data;
            }

            if (response.statusCode() == 400) {
                log.debug("Requesting authorization");
                response = getAccessToken(client);

                if (response.statusCode() == 200) {
                    accessToken = mapToAccessToken(response.body());
                    log.debug("Authorization granted");
                    log.debug("Fetching data for id: {}", id);
                    response = getAudioTrackData(client, id);

                    if (response.statusCode() == 200) {
                        log.debug("Data fetched");
                        var data = mapToAudioTrackData(response.body());
                        cache.put(id, data);
                        return data;
                    }
                }
            }

            throw new SpotifyException(
                    "Error fetching data. code: " + response.statusCode());

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SpotifyException(e);
        }
    }

    private HttpResponse<String> getAudioTrackData(HttpClient client, String id)
            throws IOException,
            InterruptedException {
        String uri = TRACK_ENDPOINT + id;
        var request = HttpRequest.newBuilder(URI.create(uri))
                .GET()
                .headers("Authorization", accessToken)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private AudioTrackData mapToAudioTrackData(String json)
            throws JsonProcessingException {
        JsonNode root = mapper.readTree(json);
        JsonNode nameNode = root.get("name");
        JsonNode artistsArray = root.get("artists");
        JsonNode artistNode = artistsArray.get(0);
        JsonNode artistNameNode = artistNode.get("name");
        return new AudioTrackData(
                nameNode.asText(),
                artistNameNode.asText());
    }

    private HttpResponse<String> getAccessToken(HttpClient client)
            throws IOException,
            InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
                .POST(HttpRequest.BodyPublishers.ofString(FORM))
                .headers(
                        "Authorization", authorization,
                        "Content-Type", CONTENT_TYPE)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String mapToAccessToken(String json)
            throws JsonProcessingException {
        JsonNode root = mapper.readTree(json);
        JsonNode tokenNode = root.get("access_token");
        return "Bearer " + tokenNode.asText();
    }
}
