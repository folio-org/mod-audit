package org.folio.verticle.marc.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.MarcEventPayload;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

@Component
public class MarcRecordEventsHandler implements AsyncRecordHandler<String, String> {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final String RECORD_TYPE = "folio.srs.recordType";
  private static final String EVENT_PAYLOAD_KEY = "eventPayload";
  private static final String LOG_DATA = "Marc Record audit event with id '%s', action '%s' and record type '%s'";

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
    if (SourceRecordDomainEventType.UNKNOWN == event.getEventType()) {
      LOGGER.debug("handle:: Event type not supported [eventId: {}]", event.getEventId());
      result.complete(event.getEventId());
      return result.future();
    }
    LOGGER.info("handle:: Starting processing of Marc Record audit event with id '{} and type '{}''", event.getEventId(), event.getEventType());
    var headers = KafkaHeaderUtils.kafkaHeadersToMap(kafkaConsumerRecord.headers());
    var event = buildSourceRecordDomainEvent(kafkaConsumerRecord.value(), kafkaConsumerRecord.timestamp(), headers);
    var log = String.format(LOG_DATA, event.getEventId(), event.getEventType(), event.getRecordType());
    LOGGER.info("saveMarcDomainEvent:: Starting processing of {}", log);
    marcAuditService.saveMarcDomainEvent(event)
      .onSuccess(ar -> {
        LOGGER.info("saveMarcDomainEvent:: Saved {}", log);
        result.complete(event.getEventId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("saveMarcDomainEvent:: Duplicate Marc bib audit event with id: {} received, skipped processing", event.getEventId());
          result.complete(event.getEventId());
        } else {
          LOGGER.error("saveMarcDomainEvent:: Processing of {}", log, e);
          result.fail(e);
        }
      });
    return result.future();
  }

  private SourceRecordDomainEvent buildSourceRecordDomainEvent(String eventValue, long eventTime, Map<String, String> headers) {
    var eventJson = new JsonObject(eventValue);
    var payload = new JsonObject(String.valueOf(eventJson.remove(EVENT_PAYLOAD_KEY))).mapTo(MarcEventPayload.class);
    var event = eventJson.mapTo(SourceRecordDomainEvent.class);
    event.setEventPayload(payload);
    event.getEventMetadata().setEventDate(Instant.ofEpochMilli(eventTime).atZone(ZoneId.systemDefault()).toLocalDateTime());
    event.setRecordType(SourceRecordType.valueOf(headers.get(RECORD_TYPE)));
    return event;
  }
}
