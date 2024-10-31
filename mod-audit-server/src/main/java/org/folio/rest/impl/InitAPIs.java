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
import org.folio.verticle.acquisition.OrderEventConsumersVerticle;
import org.folio.verticle.acquisition.OrderLineEventConsumersVerticle;
import org.folio.verticle.acquisition.PieceEventConsumersVerticle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.Arrays;

public class InitAPIs implements InitAPI {
  private final Logger LOGGER = LogManager.getLogger();
  private static final String SPRING_CONTEXT_KEY = "springContext";

  @Value("${acq.orders.kafka.consumer.instancesNumber:1}")
  private int acqOrderConsumerInstancesNumber;
  @Value("${acq.order-lines.kafka.consumer.instancesNumber:1}")
  private int acqOrderLineConsumerInstancesNumber;
  @Value("${acq.pieces.kafka.consumer.instancesNumber:1}")
  private int acqPieceConsumerInstancesNumber;
  @Value("${acq.invoices.kafka.consumer.instancesNumber:1}")
  private int acqInvoiceConsumerInstancesNumber;

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

    vertx.deployVerticle(getVerticleName(verticleFactory, OrderEventConsumersVerticle.class),
      new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
        .setInstances(acqOrderConsumerInstancesNumber), orderEventsConsumer);

    vertx.deployVerticle(getVerticleName(verticleFactory, OrderLineEventConsumersVerticle.class),
      new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
        .setInstances(acqOrderLineConsumerInstancesNumber), orderLineEventsConsumer);

    vertx.deployVerticle(getVerticleName(verticleFactory, PieceEventConsumersVerticle.class),
      new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
        .setInstances(acqPieceConsumerInstancesNumber), pieceEventsConsumer);

    vertx.deployVerticle(getVerticleName(verticleFactory, InvoiceEventConsumersVerticle.class),
      new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER)
        .setInstances(acqInvoiceConsumerInstancesNumber), invoiceEventsConsumer);

    LOGGER.info("deployConsumersVerticles:: All consumer verticles were successfully deployed");
    return GenericCompositeFuture.all(Arrays.asList(
      orderEventsConsumer.future(),
      orderLineEventsConsumer.future(),
      pieceEventsConsumer.future(),
      invoiceEventsConsumer.future()));
  }

  private <T> String getVerticleName(VerticleFactory verticleFactory, Class<T> clazz) {
    LOGGER.debug("getVerticleName:: Retrieving Verticle name");
    return verticleFactory.prefix() + ":" + clazz.getName();
  }
}
