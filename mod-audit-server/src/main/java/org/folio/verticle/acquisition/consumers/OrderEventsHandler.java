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
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.services.acquisition.OrderAuditEventsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventsHandler implements AsyncRecordHandler<String, String> {

  private static final Logger LOGGER = LogManager.getLogger();

  private OrderAuditEventsService orderAuditEventsService;

  private Vertx vertx;

  public OrderEventsHandler(@Autowired Vertx vertx,
                            @Autowired OrderAuditEventsService orderAuditEventsService) {
    this.vertx = vertx;
    this.orderAuditEventsService = orderAuditEventsService;
  }

  @Override
  public Future<String> handle(KafkaConsumerRecord<String, String> record) {

    Promise<String> result = Promise.promise();
    List<KafkaHeader> kafkaHeaders = record.headers();
    OkapiConnectionParams okapiConnectionParams = new OkapiConnectionParams(KafkaHeaderUtils.kafkaHeadersToMap(kafkaHeaders), vertx);
    OrderAuditEvent event = new JsonObject(record.value()).mapTo(OrderAuditEvent.class);
    LOGGER.info("Starting processing of Order audit event with id: {} for order id: {}", event.getId(), event.getOrderId());

    orderAuditEventsService.saveOrderAuditEvent(event, okapiConnectionParams.getTenantId())
      .onSuccess(ar -> {
        LOGGER.info("Order audit event with id: {} has been processed for order id: {}", event.getId(), event.getOrderId());
        result.complete(event.getId());
      })
      .onFailure(e -> {
        if (e instanceof DuplicateEventException) {
          LOGGER.info("Duplicate Order audit event with id: {} for order id: {} received, skipped processing", event.getId(), event.getOrderId());
          result.complete(event.getId());
        } else {
          LOGGER.error("Processing of Order audit event with id: {} for order id: {} has been failed", event.getId(), event.getOrderId(), e);
          result.fail(e);
        }
      });

    return result.future();
  }
}
