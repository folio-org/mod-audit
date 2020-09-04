package org.folio.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.acq.model.LogEventPayload;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class PubSubLogPublisherUtil {

  private PubSubLogPublisherUtil() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubLogPublisherUtil.class);

  public static final String EVENT_TYPE = "LOG_RECORD_EVENT";

  /**
   * This method registers LogEventPublisher on PubSub
   *
   * @param headers OKAPI headers
   * @param vertx   vertx
   * @return true if LogEventPublisher registered successfully, otherwise - false
   */
  public static CompletableFuture<Boolean> registerLogEventPublisher(Map<String, String> headers, Vertx vertx) {

    final CompletableFuture<Boolean> registrationResult = new CompletableFuture<>();

    PubSubClientUtils.registerModule(new OkapiConnectionParams(headers, vertx))
      .whenComplete((result, throwable) -> {
        if (throwable == null) {
          LOGGER.info("Module was successfully registered as publisher/subscriber in mod-pubsub");
          registrationResult.complete(true);
        } else {
          LOGGER.error("Error during module registration in mod-pubsub", throwable);
          registrationResult.completeExceptionally(throwable);
        }
      });

    return registrationResult;
  }

  /**
   * This method publish {@link Event} with {@link LogEventPayload}
   *
   * @param payload {@link LogEventPayload} with information used for {@link org.folio.rest.jaxrs.model.LogRecord} creation
   * @param params  connection parameters
   * @return true if event published successfully, otherwise - false
   */
  public static CompletableFuture<Boolean> sendEventWithPayload(LogEventPayload payload, OkapiConnectionParams params) {
    Event event = buildLogRecordEvent(payload, params);

    final CompletableFuture<Boolean> publishResult = new CompletableFuture<>();

    PubSubClientUtils.sendEventMessage(event, params)
      .whenComplete((result, throwable) -> {
        if (Boolean.TRUE.equals(result)) {
          LOGGER.debug("LogEvent published successfully. ID: {}, payload: {}", event.getId(), event.getEventPayload());
          publishResult.complete(true);
        } else {
          LOGGER.error("Failed to publish LogEvent. ID: {}, payload: {}", throwable, event.getId(), event.getEventPayload());
          if (throwable != null && throwable.getMessage() != null && throwable.getMessage().toLowerCase().contains("there is no subscribers registered for event type")) {
            publishResult.complete(true);
          } else {
            publishResult.completeExceptionally(throwable);
          }
        }
      });

    return publishResult;
  }

  /**
   * This method builds {@link Event} with {@link LogEventPayload}
   * @param payload payload with needed data
   * @param params OKAPI connection parameters
   * @return event with LogEventPayload
   */
  private static Event buildLogRecordEvent(LogEventPayload payload, OkapiConnectionParams params) {
    return new Event().withId(UUID.randomUUID()
      .toString())
      .withEventType(EVENT_TYPE)
      .withEventPayload(JsonObject.mapFrom(payload).encode())
      .withEventMetadata(new EventMetadata().withTenantId(params.getTenantId())
        .withEventTTL(1)
        .withPublishedBy(PubSubClientUtils.constructModuleName()));
  }
}
