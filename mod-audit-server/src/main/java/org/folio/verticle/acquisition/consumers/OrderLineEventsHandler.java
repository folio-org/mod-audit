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
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.OrderLineAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderLineEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private OrderLineAuditEventsService orderLineAuditEventsService;

  private Vertx vertx;

  public OrderLineEventsHandler(@Autowired Vertx vertx,
                            @Autowired OrderLineAuditEventsService orderLineAuditEventsService) {
    this.vertx = vertx;
    this.orderLineAuditEventsService = orderLineAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> record) {
    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = record.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    OrderLineAuditEvent event = new JsonObject(record.value()).mapTo(OrderLineAuditEvent.class);
    LOGGER.info("Starting processing of Order Line audit event with id: {} for order id: {} and order line id: {}",
      event.getId(), event.getOrderId(), event.getOrderLineId());

    orderLineAuditEventsService.saveOrderLineAuditEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("Order Line audit event with id: {} has been processed for order id: {} and order line id: {}",
          event.getId(), event.getOrderId(), event.getOrderLineId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("Duplicate Order Line audit event with id: {} for order id: {} and order line id: {} received, skipped processing",
            event.getId(), event.getOrderId(), event.getOrderLineId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Order Line audit event with id: {} for order id: {} and order line id: {} has been failed",
            event.getId(), event.getOrderId(), event.getOrderLineId(), e);
          result.fail(e);
        }
      });

    return result.future();
  }
}
