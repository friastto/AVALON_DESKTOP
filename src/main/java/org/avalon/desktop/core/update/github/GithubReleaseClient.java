package org.avalon.desktop.core.update.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Cliente para interactuar con la API de GitHub Releases usando Jackson.
 */
public class GithubReleaseClient {
    private final String owner;
    private final String repo;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public GithubReleaseClient(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Obtiene la información de la última release desde GitHub.
     */
    public Optional<ReleaseInfo> getLatestRelease() {
        String url = String.format("https://api.github.com/repos/%s/%s/releases/latest", owner, repo);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "AvalonDesktop-App") // GitHub requiere un User-Agent
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // Jackson parsea automáticamente el JSON al Record ReleaseInfo
                return Optional.of(mapper.readValue(response.body(), ReleaseInfo.class));
            }
        } catch (Exception e) {
            // Log del error para depuración
            System.err.println("Error al consultar GitHub API: " + e.getMessage());
        }

        return Optional.empty();
    }
}
