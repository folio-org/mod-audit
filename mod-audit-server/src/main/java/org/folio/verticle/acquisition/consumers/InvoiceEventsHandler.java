package org.folio.verticle.acquisition.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.services.acquisition.InvoiceAuditEventsService;
import org.folio.util.KafkaUtils;
import org.springframework.stereotype.Component;

@Component
public class InvoiceEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final InvoiceAuditEventsService invoiceAuditEventsService;

  public InvoiceEventsHandler(InvoiceAuditEventsService invoiceAuditEventsService) {
    this.invoiceAuditEventsService = invoiceAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var tenantId = KafkaUtils.getTenantId(kafkaConsumerRecord);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(InvoiceAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Invoice audit event with id: {} for invoice id: {}", event.getId(), event.getInvoiceId());
    invoiceAuditEventsService.saveInvoiceAuditEvent(event, tenantId)
      .onSuccess(ar -> {
        LOGGER.info("handle:: Invoice audit event with id: {} has been processed for invoice id: {}", event.getId(), event.getInvoiceId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Invoice audit event with id: {} for invoice id: {} received, skipped processing", event.getId(), event.getInvoiceId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Invoice audit event with id: {} for invoice id: {} has been failed", event.getId(), event.getInvoiceId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }
}
