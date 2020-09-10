package org.folio.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.HttpStatus;
import org.folio.rest.client.PubsubClient;
import org.folio.rest.jaxrs.model.EventDescriptor;
import org.folio.rest.jaxrs.model.PublisherDescriptor;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;
import org.folio.util.pubsub.exceptions.ModuleRegistrationException;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PubSubModuleRegistrationUtil {

  private PubSubModuleRegistrationUtil() {
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PubSubModuleRegistrationUtil.class);

  public static final int EVENT_TTL = 5;

  /**
   * This method registers LogEventPublisher on PubSub
   *
   * @param headers OKAPI headers
   * @param vertx   vertx
   * @return true if LogEventPublisher registered successfully, otherwise - false
   */
  public static CompletableFuture<Boolean> registerLogEventPublisher(Map<String, String> headers, Vertx vertx) {
    final CompletableFuture<Boolean> registration = new CompletableFuture<>();
    registerModuleOnPubSub(new OkapiConnectionParams(headers, vertx)).whenComplete((result, throwable) -> {
      if (throwable == null) {
        LOGGER.info("Module: {} was successfully registered as Log Event Publisher in mod-pubsub", PubSubClientUtils.constructModuleName());
        registration.complete(true);
      } else {
        LOGGER.error(String.format("Error during %s registration in mod-pubsub for publishing log record events", PubSubClientUtils.constructModuleName()), throwable);
        registration.complete(false);
      }
    });
    return registration;
  }

  private static CompletableFuture<Boolean> registerModuleOnPubSub(OkapiConnectionParams params) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    try {
      PubsubClient client = new PubsubClient(params.getOkapiUrl(), params.getTenantId(), params.getToken());
      EventDescriptor evenDescriptor = buildLogRecordEventDescriptor();
      LOGGER.info("Registering {} for publishing", evenDescriptor.getEventType());
      return registerLogRecordEventType(client, evenDescriptor)
        .thenCompose(ar -> registerLogEventPublisher(client, buildLogEventPublisherDescriptor(evenDescriptor)));
    } catch (Exception e) {
      return logAndCompleteExceptionally(result, e, "Error during registration module in PubSub");
    }
  }

  private static CompletableFuture<Boolean> registerLogEventPublisher(PubsubClient client, PublisherDescriptor descriptor) {
    LOGGER.info("Registering module's publishers");
    CompletableFuture<Boolean> publisherRegistrationResult = new CompletableFuture<>();
    try {
      client.postPubsubEventTypesDeclarePublisher(descriptor, ar -> {
        if (ar.statusCode() == HttpStatus.HTTP_CREATED.toInt()) {
          LOGGER.info("LogEventPublisher was successfully registered");
          publisherRegistrationResult.complete(true);
        } else {
          ModuleRegistrationException e = new ModuleRegistrationException("LogEventPublisher was not registered in PubSub. HTTP status: " + ar.statusCode());
          LOGGER.error(e);
          publisherRegistrationResult.completeExceptionally(e);
        }
      });
    } catch (Exception e) {
      return logAndCompleteExceptionally(publisherRegistrationResult, e, "LogEventPublisher was not not registered in PubSub");
    }
    return publisherRegistrationResult;
  }

  private static CompletableFuture<Boolean> registerLogRecordEventType(PubsubClient client, EventDescriptor eventDescriptor) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    try {
      client.postPubsubEventTypes(null, eventDescriptor, ar -> {
        if (ar.statusCode() == HttpStatus.HTTP_CREATED.toInt()) {
          future.complete(true);
        } else {
          ModuleRegistrationException exception = new ModuleRegistrationException(String.format("LogEventDescriptor was not registered for eventType: %s . Status code: %s",
            eventDescriptor.getEventType(), ar.statusCode()));
          LOGGER.error(exception);
          future.completeExceptionally(exception);
        }
      });
    } catch (Exception e) {
      return logAndCompleteExceptionally(future, e, "LogEventDescriptor was not registered in PubSub");
    }
    return future;
  }

  private static PublisherDescriptor buildLogEventPublisherDescriptor(EventDescriptor evenDescriptor) {
    return new PublisherDescriptor().withEventDescriptors(Collections.singletonList(evenDescriptor))
      .withModuleId(PubSubClientUtils.constructModuleName());
  }

  private static EventDescriptor buildLogRecordEventDescriptor() {
    return new EventDescriptor().withEventType(EventType.LOG_RECORD_EVENT.name())
      .withDescription(EventType.LOG_RECORD_EVENT.description())
      .withEventTTL(EVENT_TTL)
      .withSigned(false);
  }

  private static CompletableFuture<Boolean> logAndCompleteExceptionally(CompletableFuture<Boolean> result, Exception e, String message) {
    LOGGER.error(message, e);
    result.completeExceptionally(e);
    return result;
  }
}
