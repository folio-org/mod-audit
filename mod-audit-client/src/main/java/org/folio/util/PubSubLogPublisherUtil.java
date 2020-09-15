package org.folio.util;

import static org.folio.util.PubSubModuleRegistrationUtil.EVENT_TTL;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.LogEventPayload;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public final class PubSubLogPublisherUtil {

  private PubSubLogPublisherUtil() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubLogPublisherUtil.class);

  /**
   * This method publish {@link Event} with {@link LogEventPayload}
   *
   * @param payload {@link String} with information used for {@link org.folio.rest.jaxrs.model.LogRecord} creation
   * @param params  connection parameters
   * @return true if event published successfully, otherwise - false
   */
  public static CompletableFuture<Boolean> sendLogRecordEvent(String payload, OkapiConnectionParams params) {

    Event event = buildLogRecordEvent(payload, params);

    final CompletableFuture<Boolean> eventPublishingResult = new CompletableFuture<>();

    PubSubClientUtils.sendEventMessage(event, params)
      .whenComplete((result, throwable) -> {
        if (Boolean.TRUE.equals(result)) {
          LOGGER.debug("LogEvent published successfully. ID: {}, payload: {}", event.getId(), event.getEventPayload());
          eventPublishingResult.complete(true);
        } else {
          if (throwable != null && throwable.getMessage() != null && throwable.getMessage().toLowerCase().contains("there is no subscribers registered for event type")) {
            eventPublishingResult.complete(true);
          } else {
            LOGGER.error("Failed to publish Log Event. ID: {}, payload: {}", throwable, event.getId(), event.getEventPayload());
            eventPublishingResult.complete(false);
          }
        }
      });
    return eventPublishingResult;
  }

  /**
   * This method publish {@link Event} with {@link LogEventPayload}
   *
   * @param payload {@link LogEventPayload} with information used for {@link org.folio.rest.jaxrs.model.LogRecord} creation
   * @param params  connection parameters
   * @return true if event published successfully, otherwise - false
   */
  public static CompletableFuture<Boolean> sendLogRecordEvent(LogEventPayload payload, OkapiConnectionParams params) {
    return sendLogRecordEvent(JsonObject.mapFrom(payload).encode(), params);
  }

  public static Event buildLogRecordEvent(String payload, OkapiConnectionParams params) {
    return new Event().withId(UUID.randomUUID()
      .toString())
      .withEventType(EventType.LOG_RECORD_EVENT.name())
      .withEventPayload(payload)
      .withEventMetadata(new EventMetadata()
        .withTenantId(params.getTenantId())
        .withEventTTL(EVENT_TTL)
        .withPublishedBy(PubSubClientUtils.constructModuleName()));
  }
}
