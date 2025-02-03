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
import org.folio.rest.jaxrs.model.InvoiceLineAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.InvoiceLineAuditEventsService;
import org.springframework.stereotype.Component;

@Component
public class InvoiceLineEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final InvoiceLineAuditEventsService invoiceLineAuditEventsService;
  private final Vertx vertx;

  public InvoiceLineEventsHandler(Vertx vertx,
                                  InvoiceLineAuditEventsService invoiceLineAuditEventsService) {
    this.vertx = vertx;
    this.invoiceLineAuditEventsService = invoiceLineAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var kafkaHeaders = kafkaConsumerRecord.headers();
    var okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(InvoiceLineAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Invoice Line audit event with id: {} for invoice line id: {}", event.getId(), event.getInvoiceLineId());
    invoiceLineAuditEventsService.saveInvoiceLineAuditEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("handle:: Invoice Line audit event with id: {} has been processed for invoice line id: {}",
          event.getId(), event.getInvoiceLineId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Invoice Line audit event with id: {} for invoice line id: {} received, skipped processing", event.getId(), event.getInvoiceLineId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Invoice Line audit event with id: {} for invoice line id: {} has been failed", event.getId(), event.getInvoiceLineId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }
}
