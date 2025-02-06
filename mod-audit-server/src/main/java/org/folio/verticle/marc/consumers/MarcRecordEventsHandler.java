package org.folio.verticle.marc.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.EventPayload;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

@Component
public class MarcRecordEventsHandler implements AsyncRecordHandler<String, String> {
  private static final Logger LOGGER = LogManager.getLogger();

  private final MarcAuditService marcAuditService;
  private final Vertx vertx;

  public MarcRecordEventsHandler(Vertx vertx,
                                 MarcAuditService marcAuditService) {
    this.vertx = vertx;
    this.marcAuditService = marcAuditService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var event = buildSourceRecordDomainEvent(kafkaConsumerRecord.value(), kafkaConsumerRecord.timestamp());
    LOGGER.info("saveMarcDomainEvent:: Starting processing of Marc Record audit event with id '{} and type '{}''", event.getEventId(), event.getEventType());
    marcAuditService.saveMarcDomainEvent(event)
      .onSuccess(ar -> {
        LOGGER.info("saveMarcDomainEvent:: Saved Marc Record audit event with id '{} and type '{}''", event.getEventId(), event.getEventType());
        result.complete(event.getEventId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("saveMarcDomainEvent:: Duplicate Marc bib audit event with id: {} received, skipped processing", event.getEventId());
          result.complete(event.getEventId());
        } else {
          LOGGER.error("saveMarcDomainEvent:: Processing of Marc Bib audit event with id: {} and type '{}' has been failed", event.getEventId(), event.getEventType(), e);
          result.fail(e);
        }
      });
    return result.future();
  }

  private SourceRecordDomainEvent buildSourceRecordDomainEvent(String eventValue, long eventTime) {
    var eventJson = new JsonObject(eventValue);
    var payload = new JsonObject(String.valueOf(eventJson.remove("eventPayload"))).mapTo(EventPayload.class);
    var event = eventJson.mapTo(SourceRecordDomainEvent.class);
    event.setEventPayload(payload);
    event.getEventMetadata().setEventDate(Instant.ofEpochMilli(eventTime).atZone(ZoneId.systemDefault()).toLocalDateTime());
    return event;
  }
}
