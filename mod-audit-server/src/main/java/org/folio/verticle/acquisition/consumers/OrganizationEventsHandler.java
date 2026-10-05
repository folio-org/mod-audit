package org.folio.verticle.acquisition.consumers;

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
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.OrganizationAuditEventsService;
import org.springframework.stereotype.Component;

@Component
public class OrganizationEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrganizationAuditEventsService organizationAuditEventsService;
  private final Vertx vertx;

  public OrganizationEventsHandler(Vertx vertx,
                                   OrganizationAuditEventsService organizationAuditEventsService) {
    this.vertx = vertx;
    this.organizationAuditEventsService = organizationAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var kafkaHeaders = kafkaConsumerRecord.headers();
    var okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(OrganizationAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Organization audit event with id: {} for organization id: {}", event.getId(), event.getOrganizationId());
    organizationAuditEventsService.saveOrganizationAuditEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("handle:: Organization audit event with id: {} has been processed for organization id: {}", event.getId(), event.getOrganizationId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Organization audit event with id: {} for organization id: {} received, skipped processing", event.getId(), event.getOrganizationId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Organization audit event with id: {} for organization id: {} has been failed", event.getId(), event.getOrganizationId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }
}
