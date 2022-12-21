package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.config.ApplicationConfig;
import org.folio.rest.resource.interfaces.InitAPI;
import org.folio.spring.SpringContextUtil;
import org.folio.verticle.SpringVerticleFactory;
import org.folio.verticle.acquisition.OrderEventConsumersVerticle;
import org.folio.verticle.acquisition.OrderLineEventConsumersVerticle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.AbstractApplicationContext;

public class InitAPIs implements InitAPI {
  private final Logger LOGGER = LogManager.getLogger();
  private static final String SPRING_CONTEXT_KEY = "springContext";

  @Value("${acq.orders.kafka.consumer.instancesNumber:1}")
  private int orderEventConsumerInstancesNumber;

  @Override
  public void init(Vertx vertx, Context context, Handler<AsyncResult<Boolean>> handler) {
    LOGGER.info("InitAPI starting...");
    try {
      SpringContextUtil.init(vertx, context, ApplicationConfig.class);
      SpringContextUtil.autowireDependencies(this, context);
      deployConsumersVerticles(vertx)
        .onSuccess(car -> {
          handler.handle(Future.succeededFuture());
          LOGGER.info("Consumer Verticles were successfully started");
        })
        .onFailure(th -> {
          handler.handle(Future.failedFuture(th));
          LOGGER.error("Consumer Verticles were not started", th);
        });
    } catch (Throwable th) {
      LOGGER.error("Error during module init", th);
      handler.handle(Future.failedFuture(th));
    }
  }

  private Future<?> deployConsumersVerticles(Vertx vertx) {
    AbstractApplicationContext springContext = vertx.getOrCreateContext().get(SPRING_CONTEXT_KEY);
    VerticleFactory verticleFactory = springContext.getBean(SpringVerticleFactory.class);
    vertx.registerVerticleFactory(verticleFactory);

    Promise<String> deployOrderAndOrderLineEventConsumer = Promise.promise();
    vertx.deployVerticle(getVerticleName(verticleFactory, OrderEventConsumersVerticle.class),
      new DeploymentOptions()
        .setWorker(true)
        .setInstances(orderEventConsumerInstancesNumber), deployOrderAndOrderLineEventConsumer);

    vertx.deployVerticle(getVerticleName(verticleFactory, OrderLineEventConsumersVerticle.class),
      new DeploymentOptions()
        .setWorker(true)
        .setInstances(orderEventConsumerInstancesNumber), deployOrderAndOrderLineEventConsumer);

    return deployOrderAndOrderLineEventConsumer.future();
  }

  private <T> String getVerticleName(VerticleFactory verticleFactory, Class<T> clazz) {
    return verticleFactory.prefix() + ":" + clazz.getName();
  }
}
