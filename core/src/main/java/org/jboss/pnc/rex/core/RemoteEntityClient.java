/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021-2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.rex.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.rex.core.api.TaskController;
import org.jboss.pnc.rex.core.api.TaskRegistry;
import org.jboss.pnc.rex.core.delegates.WithTransactions;
import org.jboss.pnc.rex.model.Request;
import org.jboss.pnc.rex.model.Task;
import org.jboss.pnc.rex.model.requests.StartRequest;
import org.jboss.pnc.rex.model.requests.StopRequest;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * TODO: Possibly convert to JAX-RS RestClient builder to enable dynamic url
 * @see <a href="https://download.eclipse.org/microprofile/microprofile-rest-client-1.2.1/microprofile-rest-client-1.2.1.html#_sample_builder_usage">JAX-RS Builder</a>
 */

@Unremovable
@ApplicationScoped
@Slf4j
public class RemoteEntityClient {

    private static final String INTERNAL_ENDPOINT_PATH = "/rest/internal";
    private static final String SUCCESS_ENDPOINT_PATH = INTERNAL_ENDPOINT_PATH + "/%s/succeed";
    private static final String FAILED_ENDPOINT_PATH = INTERNAL_ENDPOINT_PATH + "/%s/fail";
    private static final String SINGLE_FINISH_ENDPOINT_PATH = INTERNAL_ENDPOINT_PATH + "/%s/finish";

    private final TaskController controller;

    private final TaskRegistry taskRegistry;

    private final GenericVertxHttpClient client;

    private final ObjectMapper mapper;

    private final String baseUrl;

    public RemoteEntityClient(GenericVertxHttpClient client,
                              @WithTransactions TaskController controller,
                              TaskRegistry taskRegistry,
                              ObjectMapper mapper,
                              @ConfigProperty(name = "scheduler.baseUrl",
                                      defaultValue = "http://localhost:8080") String baseUrl) {
        this.controller = controller;
        this.taskRegistry = taskRegistry;
        this.client = client;
        this.mapper = mapper;
        this.baseUrl = baseUrl;
    }

    public void stopJob(Task task) {
        Request requestDefinition = task.getRemoteCancel();

        URI url;
        try {
            url = new URI(requestDefinition.getUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("remoteCancel.url is not a valid URL for task with name " +
                    task.getName(), e);
        }


        org.jboss.pnc.api.dto.Request callbackRequest = getCallbackRequest(task.getName(), SINGLE_FINISH_ENDPOINT_PATH);
        org.jboss.pnc.api.dto.Request positiveCallback = getCallbackRequest(task.getName(), SUCCESS_ENDPOINT_PATH);
        org.jboss.pnc.api.dto.Request negativeCallback = getCallbackRequest(task.getName(), FAILED_ENDPOINT_PATH);

        StopRequest request = StopRequest.builder()
                .payload(requestDefinition.getAttachment())
                .taskResults(getTaskResultsIfConfigurationAllows(task))
                .callback(callbackRequest)
                .positiveCallback(positiveCallback)
                .negativeCallback(negativeCallback)
                .build();

        client.makeRequest(url,
                requestDefinition.getMethod(),
                requestDefinition.getHeaders(),
                request,
                response -> handleResponse(response, task),
                throwable -> handleConnectionFailure(throwable, task));
    }


    public void startJob(Task task) {
        Request requestDefinition = task.getRemoteStart();

        URI uri;
        try {
            uri = new URI(requestDefinition.getUrl());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("remoteStart.url is not a valid URL for task with name " +
                    task.getName(), e);
        }

        org.jboss.pnc.api.dto.Request callbackRequest = getCallbackRequest(task.getName(), SINGLE_FINISH_ENDPOINT_PATH);
        org.jboss.pnc.api.dto.Request positiveCallback = getCallbackRequest(task.getName(), SUCCESS_ENDPOINT_PATH);
        org.jboss.pnc.api.dto.Request negativeCallback = getCallbackRequest(task.getName(), FAILED_ENDPOINT_PATH);

        StartRequest request = StartRequest.builder()
                .payload(requestDefinition.getAttachment())
                .taskResults(getTaskResultsIfConfigurationAllows(task))
                .callback(callbackRequest)
                .positiveCallback(positiveCallback)
                .negativeCallback(negativeCallback)
                .build();

        client.makeRequest(uri,
                requestDefinition.getMethod(),
                requestDefinition.getHeaders(),
                request,
                response -> handleResponse(response, task),
                throwable -> handleConnectionFailure(throwable, task));
    }

    private void handleResponse(HttpResponse<Buffer> response, Task task) {
        if (200 <= response.statusCode() && response.statusCode() <= 299) {
            log.info("RESPONSE {}: Got positive response.", task.getName());
            controller.accept(task.getName(), parseBody(response));
        } else if (300 <= response.statusCode() && response.statusCode() <= 399) {
            log.info("RESPONSE {}: Got redirect to {}", task.getName(), response.getHeader("Location"));
            // TODO do not fail after proper redirect handling
            controller.fail(task.getName(), parseBody(response));
        } else {
            log.info("RESPONSE {}: Got negative response. (STATUS CODE: {})", task.getName(), response.statusCode());
            controller.fail(task.getName(), parseBody(response));
        }
    }

    private void handleConnectionFailure(Throwable exception, Task task) {
        log.error("ERROR " + task.getName() + ": Couldn't reach the remote entity.", exception);
        // format of the simulated "response" could be better (mainly not String)
        Uni.createFrom().voidItem()
            .onItem().invoke(() -> controller.fail(task.getName(), "Remote entity failed to respond. Exception: " + exception.toString()))
            .onFailure().retry().atMost(5)
            .onFailure().invoke((throwable) -> log.error("ERROR: Couldn't commit transaction. Data corruption is possible.", throwable))
            .onFailure().recoverWithNull()
            .await().indefinitely();
    }

    private Object parseBody(HttpResponse<Buffer> response) {
        String body = response.bodyAsString();
        Object objectResponse = null;
        if (body != null) {
            try {
                objectResponse = mapper.readValue(body, Object.class);
            } catch (JsonProcessingException ignored) {
                log.warn("Response(statusCode: {}) could not be parsed. Response: {}", response.statusCode(), response.bodyAsString());
            }
        }
        return objectResponse;
    }

    private org.jboss.pnc.api.dto.Request getCallbackRequest(String taskName, String templateEndpoint) {
        String callback = baseUrl + templateEndpoint.formatted(taskName);

        org.jboss.pnc.api.dto.Request callbackRequest;
        try {
            URI callbackUri = new URI(callback);
            List<org.jboss.pnc.api.dto.Request.Header> headers = List.of(new org.jboss.pnc.api.dto.Request.Header(CONTENT_TYPE, APPLICATION_JSON));
            callbackRequest = new org.jboss.pnc.api.dto.Request(org.jboss.pnc.api.dto.Request.Method.POST, callbackUri, headers);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("callbackUri " + callback + " is not a valid URL for task with name " + taskName, e);
        }
        return callbackRequest;
    }

    /**
     * Get task results of dependencies if the configuration allows it, otherwise return null
     * @param task to process
     *
     * @return task results of dependencies
     */
    private Map<String, Object> getTaskResultsIfConfigurationAllows(Task task) {
        if (task.getConfiguration() != null && task.getConfiguration().isPassResultsOfDependencies()) {
            return taskRegistry.getTaskResults(task);
        } else {
            return null;
        }
    }

}
