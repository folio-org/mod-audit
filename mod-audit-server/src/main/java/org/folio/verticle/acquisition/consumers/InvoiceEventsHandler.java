package org.folio.verticle.acquisition.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.producer.KafkaHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.KafkaHeaderUtils;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.InvoiceAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.InvoiceAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final InvoiceAuditEventsService invoiceAuditEventsService;
  private final Vertx vertx;

  public InvoiceEventsHandler(@Autowired Vertx vertx,
                              @Autowired InvoiceAuditEventsService invoiceAuditEventsService) {
    this.vertx = vertx;
    this.invoiceAuditEventsService = invoiceAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = kafkaConsumerRecord.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    InvoiceAuditEvent event = new JsonObject(kafkaConsumerRecord.value()).mapTo(InvoiceAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Invoice audit event with id: {} for invoice id: {}", event.getId(), event.getInvoiceId());

    invoiceAuditEventsService.saveInvoiceAuditEvent(event, okapiConnectionParams.getTenantId())
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
