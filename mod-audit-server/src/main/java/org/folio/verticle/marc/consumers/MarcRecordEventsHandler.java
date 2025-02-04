package org.folio.verticle.marc.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.marc.MarcBibAuditService;
import org.folio.util.marc.EventPayload;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

@Component
public class MarcRecordEventsHandler implements AsyncRecordHandler<String, String> {
  private static final String RECORD_TYPE_HEADER = "folio.srs.recordType";
  private static final Logger LOGGER = LogManager.getLogger();

  private final MarcBibAuditService marcBibAuditService;
  private final Vertx vertx;

  public MarcRecordEventsHandler(Vertx vertx,
                                 MarcBibAuditService marcBibAuditService) {
    this.vertx = vertx;
    this.marcBibAuditService = marcBibAuditService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var kafkaHeaders = KafkaHeaderUtils.kafkaHeadersToMap(kafkaConsumerRecord.headers());
    var okapiConnectionParams = new OkapiConnectionParams(kafkaHeaders, vertx);
    var userId = JWT.parse(okapiConnectionParams.getToken()).getJsonObject("payload").getString("user_id");
    var event = buildSourceRecordDomainEvent(kafkaConsumerRecord.value(), kafkaConsumerRecord.timestamp(), kafkaHeaders);
    var recordId = event.getRecordId();
    LOGGER.info("handle:: Starting processing of Marc Record audit event for record with type: '{}', action '{}' and id: '{};", event.getRecordType(), event.getEventType(), recordId);
    marcBibAuditService.saveMarcBibDomainEvent(event, okapiConnectionParams.getTenantId(), userId)
      .onSuccess(ar -> {
        LOGGER.info("handle:: Marc Bib audit event with id: {}", event.getEventId());
        result.complete(event.getEventId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Marc bib audit event with id: {} received, skipped processing", event.getEventId());
          result.complete(event.getEventId());
        } else {
          LOGGER.error("Processing of Marc Bib audit event with id: {} has been failed", event.getEventId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }

  private SourceRecordDomainEvent buildSourceRecordDomainEvent(String eventValue, long eventTime, Map<String, String> kafkaHeaders) {
    var recordType = SourceRecordType.valueOf(kafkaHeaders.get(RECORD_TYPE_HEADER));
    var eventJson = new JsonObject(eventValue);
    var payload = new JsonObject(String.valueOf(eventJson.remove("eventPayload"))).mapTo(EventPayload.class);
    var event = eventJson.mapTo(SourceRecordDomainEvent.class);
    event.setEventPayload(payload);
    event.setRecordType(recordType);
    event.getEventMetadata().setEventDate(Instant.ofEpochMilli(eventTime).atZone(ZoneId.systemDefault()).toLocalDateTime());
    return event;
  }
}
