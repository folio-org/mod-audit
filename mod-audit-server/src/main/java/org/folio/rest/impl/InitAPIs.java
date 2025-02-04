package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.config.ApplicationConfig;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.spring.SpringContextUtil;
import org.folio.verticle.SpringVerticleFactory;
import org.folio.verticle.acquisition.InvoiceEventConsumersVerticle;
import org.folio.verticle.acquisition.InvoiceLineEventConsumersVerticle;
import org.folio.verticle.acquisition.OrderEventConsumersVerticle;
import org.folio.verticle.acquisition.OrderLineEventConsumersVerticle;
import org.folio.verticle.acquisition.OrganizationEventConsumersVerticle;
import org.folio.verticle.acquisition.PieceEventConsumersVerticle;
import org.folio.verticle.inventory.HoldingsConsumersVerticle;
import org.folio.verticle.inventory.InstanceConsumersVerticle;
import org.folio.verticle.inventory.ItemConsumersVerticle;
import org.folio.verticle.marc.MarcRecordEventConsumersVerticle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Arrays;

public class InitAPIs implements InitAPI {
  private final Logger LOGGER = LogManager.getLogger();
  private static final String SPRING_CONTEXT_KEY = "springContext";

  @Value("${acq.orders.kafka.consumer.instancesNumber:1}")
  private int acqOrderConsumerInstancesNumber;
  @Value("${acq.orders.kafka.consumer.pool.size:5}")
  private int acqOrderConsumerPoolSize;

  @Value("${acq.order-lines.kafka.consumer.instancesNumber:1}")
  private int acqOrderLineConsumerInstancesNumber;
  @Value("${acq.order-lines.kafka.consumer.pool.size:5}")
  private int acqOrderLineConsumerPoolSize;

  @Value("${acq.pieces.kafka.consumer.instancesNumber:1}")
  private int acqPieceConsumerInstancesNumber;
  @Value("${acq.orders.kafka.consumer.pool.size:5}")
  private int acqPieceConsumerPoolSize;

  @Value("${acq.invoices.kafka.consumer.instancesNumber:1}")
  private int acqInvoiceConsumerInstancesNumber;
  @Value("${acq.invoices.kafka.consumer.pool.size:5}")
  private int acqInvoiceConsumerPoolSize;

  @Value("${acq.invoice-lines.kafka.consumer.instancesNumber:1}")
  private int acqInvoiceLineConsumerInstancesNumber;
  @Value("${acq.invoice-lines.kafka.consumer.pool.size:5}")
  private int acqInvoiceLineConsumerPoolSize;

  @Value("${acq.organizations.kafka.consumer.instancesNumber:1}")
  private int acqOrganizationConsumerInstancesNumber;
  @Value("${acq.organizations.kafka.consumer.pool.size:5}")
  private int acqOrganizationConsumerPoolSize;

  @Value("${inv.instance.kafka.consumer.instancesNumber:1}")
  private int invInstanceConsumerInstancesNumber;
  @Value("${inv.instance.kafka.consumer.pool.size:5}")
  private int invInstanceConsumerPoolSize;

  @Value("${inv.holdings.kafka.consumer.instancesNumber:1}")
  private int invHoldingsConsumerInstancesNumber;
  @Value("${inv.holdings.kafka.consumer.pool.size:5}")
  private int invHoldingsConsumerPoolSize;

  @Value("${inv.item.kafka.consumer.instancesNumber:1}")
  private int invItemConsumerInstancesNumber;
  @Value("${inv.item.kafka.consumer.pool.size:5}")
  private int invItemConsumerPoolSize;

  @Value("${src.source-records.kafka.consumer.instancesNumber:1}")
  private int srsSourceRecordsConsumerInstancesNumber;
  @Value("${src.source-records.kafka.consumer.pool.size:5}")
  private int srsSourceRecordsConsumerPoolSize;

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    LOGGER.debug("init:: InitAPI starting...");
    try {
      SpringContextUtil.init(vertx, context, ApplicationConfig.class);
      SpringContextUtil.autowireDependencies(this, context);
      deployConsumersVerticles(vertx)
        .onSuccess(car -> {
          handler.handle(Future.succeededFuture());
          LOGGER.info("init:: Consumer Verticles were successfully started");
        })
        .onFailure(th -> {
          handler.handle(Future.failedFuture(th));
          LOGGER.warn("Consumer Verticles were not started", th);
        });
    } catch (Throwable th) {
      LOGGER.warn("Error during module init", th);
      handler.handle(Future.failedFuture(th));
    }
  }

  private Future<?> deployConsumersVerticles(Vertx vertx) {
    LOGGER.debug("deployConsumersVerticles:: Deploying Consumers Verticle");
    AbstractApplicationContext springContext = vertx.getOrCreateContext().get(SPRING_CONTEXT_KEY);
    VerticleFactory verticleFactory = springContext.getBean(SpringVerticleFactory.class);
    vertx.registerVerticleFactory(verticleFactory);

    Promise<String> orderEventsConsumer = Promise.promise();
    Promise<String> orderLineEventsConsumer = Promise.promise();
    Promise<String> pieceEventsConsumer = Promise.promise();
    Promise<String> invoiceEventsConsumer = Promise.promise();
    Promise<String> invoiceLineEventsConsumer = Promise.promise();
    Promise<String> organizationsEventsConsumer = Promise.promise();
    Promise<String> inventoryInstanceConsumer = Promise.promise();
    Promise<String> inventoryHoldingsConsumer = Promise.promise();
    Promise<String> inventoryItemConsumer = Promise.promise();
    Promise<String> sourceRecordsConsumer = Promise.promise();

    deployVerticle(vertx, verticleFactory, OrderEventConsumersVerticle.class, acqOrderConsumerInstancesNumber, acqOrderConsumerPoolSize, orderEventsConsumer);
    deployVerticle(vertx, verticleFactory, OrderLineEventConsumersVerticle.class, acqOrderLineConsumerInstancesNumber, acqOrderLineConsumerPoolSize, orderLineEventsConsumer);
    deployVerticle(vertx, verticleFactory, PieceEventConsumersVerticle.class, acqPieceConsumerInstancesNumber, acqPieceConsumerPoolSize, pieceEventsConsumer);
    deployVerticle(vertx, verticleFactory, InvoiceEventConsumersVerticle.class, acqInvoiceConsumerInstancesNumber, acqInvoiceConsumerPoolSize, invoiceEventsConsumer);
    deployVerticle(vertx, verticleFactory, InvoiceLineEventConsumersVerticle.class, acqInvoiceLineConsumerInstancesNumber, acqInvoiceLineConsumerPoolSize, invoiceLineEventsConsumer);
    deployVerticle(vertx, verticleFactory, OrganizationEventConsumersVerticle.class, acqOrganizationConsumerInstancesNumber, acqOrganizationConsumerPoolSize, organizationsEventsConsumer);
    deployVerticle(vertx, verticleFactory, InstanceConsumersVerticle.class, invInstanceConsumerInstancesNumber, invInstanceConsumerPoolSize, inventoryInstanceConsumer);
    deployVerticle(vertx, verticleFactory, HoldingsConsumersVerticle.class, invHoldingsConsumerInstancesNumber, invHoldingsConsumerPoolSize, inventoryHoldingsConsumer);
    deployVerticle(vertx, verticleFactory, ItemConsumersVerticle.class, invItemConsumerInstancesNumber, invItemConsumerPoolSize, inventoryItemConsumer);
    deployVerticle(vertx, verticleFactory, MarcRecordEventConsumersVerticle.class, srsSourceRecordsConsumerInstancesNumber, srsSourceRecordsConsumerPoolSize, sourceRecordsConsumer);

    LOGGER.info("deployConsumersVerticles:: All consumer verticles were successfully deployed");
    return GenericCompositeFuture.all(Arrays.asList(
      orderEventsConsumer.future(),
      orderLineEventsConsumer.future(),
      pieceEventsConsumer.future(),
      invoiceEventsConsumer.future(),
      invoiceLineEventsConsumer.future(),
      organizationsEventsConsumer.future(),
      inventoryInstanceConsumer.future(),
      inventoryHoldingsConsumer.future(),
      inventoryItemConsumer.future(),
      sourceRecordsConsumer.future()
    ));
  }

  private <T> void deployVerticle(Vertx vertx, VerticleFactory verticleFactory, Class<T> consumerClass,
                                  int instancesNumber, int poolSize, Promise<String> eventsConsumer) {
    DeploymentOptions deploymentOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
      .setInstances(instancesNumber).setWorkerPoolSize(poolSize);
    vertx.deployVerticle(getVerticleName(verticleFactory, consumerClass), deploymentOptions, eventsConsumer);
  }

  private <T> String getVerticleName(VerticleFactory verticleFactory, Class<T> clazz) {
    LOGGER.debug("getVerticleName:: Retrieving Verticle name");
    return verticleFactory.prefix() + ":" + clazz.getName();
  }
}
