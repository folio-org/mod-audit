package org.folio.verticle.acquisition.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrganizationAuditEvent;
import org.folio.services.acquisition.OrganizationAuditEventsService;
import org.folio.util.KafkaUtils;
import org.springframework.stereotype.Component;

@Component
public class OrganizationEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrganizationAuditEventsService organizationAuditEventsService;

  public OrganizationEventsHandler(OrganizationAuditEventsService organizationAuditEventsService) {
    this.organizationAuditEventsService = organizationAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var tenantId = KafkaUtils.getTenantId(kafkaConsumerRecord);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(OrganizationAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Organization audit event with id: {} for organization id: {}", event.getId(), event.getOrganizationId());
    organizationAuditEventsService.saveOrganizationAuditEvent(event, tenantId)
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
