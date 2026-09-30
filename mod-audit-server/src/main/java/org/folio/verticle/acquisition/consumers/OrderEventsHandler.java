package org.folio.verticle.acquisition.consumers;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.kafka.AsyncRecordHandler;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.folio.util.KafkaUtils;
import org.springframework.stereotype.Component;

@Component
public class OrderEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private final OrderAuditEventsService orderAuditEventsService;

  public OrderEventsHandler(OrderAuditEventsService orderAuditEventsService) {
    this.orderAuditEventsService = orderAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> kafkaConsumerRecord) {
    var result = Promise.<String>promise();
    var tenantId = KafkaUtils.getTenantId(kafkaConsumerRecord);
    var event = new JsonObject(kafkaConsumerRecord.value()).mapTo(OrderAuditEvent.class);
    LOGGER.info("handle:: Starting processing of Order audit event with id: {} for order id: {}", event.getId(), event.getOrderId());
    orderAuditEventsService.saveOrderAuditEvent(event, tenantId)
      .onSuccess(ar -> {
        LOGGER.info("handle:: Order audit event with id: {} has been processed for order id: {}", event.getId(), event.getOrderId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("handle:: Duplicate Order audit event with id: {} for order id: {} received, skipped processing", event.getId(), event.getOrderId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Order audit event with id: {} for order id: {} has been failed", event.getId(), event.getOrderId(), e);
          result.fail(e);
        }
      });
    return result.future();
  }
}
