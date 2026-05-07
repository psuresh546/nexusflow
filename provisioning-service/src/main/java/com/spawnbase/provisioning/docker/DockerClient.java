package com.spawnbase.provisioning.docker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spawnbase.provisioning.dto.ContainerCreateRequest;
import com.spawnbase.provisioning.dto.ContainerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@Component
@Slf4j
public class DockerClient {

    private final String dockerHost;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DockerClient(
            @Value("${docker.host}") String dockerHost,
            ObjectMapper objectMapper) {
        this.dockerHost = dockerHost;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Pull a Docker image from Docker Hub.
     * POST /images/create?fromImage=postgres&tag=15
     *
     * This can take minutes for first pull.
     * Subsequent pulls are instant (cached layers).
     */
    public void pullImage(String image) {
        log.info("Pulling Docker image: {}", image);

        String[] parts = image.split(":");
        String imageName = parts[0];
        String tag = parts.length > 1 ? parts[1] : "latest";

        String url = dockerHost + "/images/create" + "?fromImage=" + imageName + "&tag=" + tag;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofMinutes(5)) // pulls can be slow
                .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() != 200) {
            throw new DockerApiException(response.statusCode(), "Failed to pull image: " + image);
        }

        log.info("Image pulled successfully: {}", image);
    }

    /**
     * Create a container from an image.
     * POST /containers/create?name={containerName}
     *
     * @return the Docker container ID (short hash)
     */
    public String createContainer(
            String containerName,
            ContainerCreateRequest createRequest) {

        log.info("Creating container: {} from image: {}", containerName, createRequest.getImage());

        String url = dockerHost + "/containers/create?name=" + containerName;

        try {
            String body = objectMapper.writeValueAsString(createRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = send(request);

            if (response.statusCode() != 201) {
                throw new DockerApiException(response.statusCode(), "Failed to create container: " + response.body());
            }

            JsonNode json = objectMapper
                    .readTree(response.body());
            String containerId = json.get("Id").asText();

            log.info("Container created: {} → id: {}", containerName, containerId.substring(0, 12));

            return containerId;

        } catch (DockerApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DockerApiException("Failed to create container: " + e.getMessage(), e);
        }
    }

    /**
     * Start a container.
     * POST /containers/{id}/start
     */
    public void startContainer(String containerId) {
        log.info("Starting container: {}", containerId.substring(0, 12));

        String url = dockerHost + "/containers/" + containerId + "/start";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request);

        // 204 = started successfully
        // 304 = container already running (not an error)
        if (response.statusCode() != 204 && response.statusCode() != 304) {
            throw new DockerApiException(response.statusCode(), "Failed to start container: " + response.body());
        }

        log.info("Container started: {}", containerId.substring(0, 12));
    }

    /**
     * Stop a container gracefully.
     * POST /containers/{id}/stop?t=10
     *
     * t=10 means give the container 10 seconds to
     * shutdown gracefully before force-killing it.
     */
    public void stopContainer(String containerId) {
        log.info("Stopping container: {}", containerId.substring(0, 12));

        String url = dockerHost + "/containers/" + containerId + "/stop?t=10";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request);

        // 204 = stopped successfully
        // 304 = container already stopped (not an error)
        if (response.statusCode() != 204
                && response.statusCode() != 304) {
            throw new DockerApiException(response.statusCode(), "Failed to stop container: " + response.body());
        }

        log.info("Container stopped: {}", containerId.substring(0, 12));
    }

    /**
     * Remove a container permanently.
     * DELETE /containers/{id}?force=true
     *
     * force=true removes even if running.
     * v=true would also remove associated volumes.
     */
    public void removeContainer(String containerId) {
        log.info("Removing container: {}", containerId.substring(0, 12));

        String url = dockerHost + "/containers/" + containerId + "?force=true";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() != 204) {
            throw new DockerApiException(response.statusCode(), "Failed to remove container: " + response.body());
        }

        log.info("Container removed: {}", containerId.substring(0, 12));
    }

    /**
     * Inspect a container — get its current state.
     * GET /containers/{id}/json
     *
     * Returns full container info including:
     * - running state
     * - health check status
     * - port mappings
     */
    public ContainerInfo inspectContainer(String containerId) {
        log.debug("Inspecting container: {}", containerId.substring(0, 12));

        String url = dockerHost + "/containers/" + containerId + "/json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() == 404) {
            throw new DockerApiException(404, "Container not found: " + containerId);
        }

        if (response.statusCode() != 200) {
            throw new DockerApiException( response.statusCode(), "Failed to inspect container: " + response.body());
        }

        try {
            return objectMapper.readValue(response.body(), ContainerInfo.class);
        } catch (Exception e) {
            throw new DockerApiException( "Failed to parse container info: " + e.getMessage(), e);
        }
    }

    /**
     * Restart a running container.
     * POST /containers/{id}/restart?t=10
     */
    public void restartContainer(String containerId) {
        log.info("Restarting container: {}", containerId.substring(0, 12));

        String url = dockerHost + "/containers/" + containerId + "/restart?t=10";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() != 204) {
            throw new DockerApiException( response.statusCode(), "Failed to restart container: " + response.body());
        }

        log.info("Container restarted: {}", containerId.substring(0, 12));
    }


    /**
     * Send HTTP request and get response.
     * Wraps checked exceptions into unchecked.
     */
    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new DockerApiException("Failed to connect to Docker: " + e.getMessage(), e);
        }
    }
}