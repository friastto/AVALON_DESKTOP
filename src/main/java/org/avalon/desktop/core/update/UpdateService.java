package org.avalon.desktop.core.update;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.avalon.desktop.core.update.github.GithubReleaseClient;
import org.avalon.desktop.core.update.github.ReleaseInfo;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Singleton
public class UpdateService {
    private final String CURRENT_VERSION = "1.0.0";
    private final String REPO_OWNER = "friastto";
    private final String REPO_NAME = "AVALON_DESKTOP";
    
    private final GithubReleaseClient client;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0);

    @Inject
    public UpdateService() {
        this.client = new GithubReleaseClient(REPO_OWNER, REPO_NAME);
    }

    public Optional<ReleaseInfo> checkForUpdates() {
        return client.getLatestRelease().filter(release -> 
                VersionComparator.isNewer(CURRENT_VERSION, release.tagName()));
    }

    public void downloadAndApply(ReleaseInfo release) {
        if (release.assets() == null || release.assets().isEmpty()) return;
        
        ReleaseInfo.Asset asset = release.assets().get(0);
        String url = asset.browserDownloadUrl();
        String fileName = asset.name();
        
        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                
                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                
                long totalBytes = Long.parseLong(response.headers().firstValue("Content-Length").orElse("-1"));
                Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
                
                try (InputStream is = response.body();
                     var os = Files.newOutputStream(tempFile)) {
                    
                    long bytesRead = 0;
                    byte[] buffer = new byte[8192];
                    int read;
                    
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                        bytesRead += read;
                        final double progress = totalBytes > 0 ? (double) bytesRead / totalBytes : -1;
                        Platform.runLater(() -> downloadProgress.set(progress));
                    }
                }
                
                launchUpdater(tempFile.toString());
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void launchUpdater(String newJarPath) {
        System.out.println("Lanzando updater para: " + newJarPath);
        Platform.exit();
        System.exit(0);
    }

    public DoubleProperty downloadProgressProperty() { return downloadProgress; }
}
