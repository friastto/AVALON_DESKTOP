package org.avalon.desktop.core.update.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Representa la información de una versión de GitHub usando anotaciones de Jackson.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReleaseInfo(
    @JsonProperty("tag_name") String tagName,
    @JsonProperty("name") String name,
    @JsonProperty("body") String body,
    @JsonProperty("assets") List<Asset> assets
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Asset(
        @JsonProperty("name") String name,
        @JsonProperty("browser_download_url") String browserDownloadUrl
    ) {}
}
