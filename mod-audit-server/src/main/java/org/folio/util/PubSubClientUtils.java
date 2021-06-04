package org.folio.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.client.PubsubClient;
import org.folio.rest.jaxrs.model.EventDescriptor;
import org.folio.rest.jaxrs.model.MessagingDescriptor;
import org.folio.rest.jaxrs.model.MessagingModule.ModuleRole;
import org.folio.rest.jaxrs.model.PublisherDescriptor;
import org.folio.rest.jaxrs.model.SubscriberDescriptor;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.RmbVersion;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.exceptions.MessagingDescriptorNotFoundException;
import org.folio.util.pubsub.exceptions.ModuleRegistrationException;
import org.folio.util.pubsub.exceptions.ModuleUnregistrationException;
import org.folio.util.pubsub.support.DescriptorHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/*
This class was created just as a workaround
to avoid using org.folio.util.pubsub.PubSubClientUtils
class that uses org.folio.rest.tools.PomReader
(PomReader is no longer supported in 33.0.0 version).
Also I added check for null result in client.postPubsubEventTypes
(this exception was not handled in the previous version).
 */
public class PubSubClientUtils {

  private static final Logger LOGGER = LogManager.getLogger();

  private PubSubClientUtils() {
  }

  public static CompletableFuture<Boolean> registerModule(OkapiConnectionParams params) {
    CompletableFuture<Boolean> result = new CompletableFuture();
    PubsubClient client = new PubsubClient(params.getOkapiUrl(), params.getTenantId(), params.getToken());

    try {
      LOGGER.info("Reading MessagingDescriptor.json");
      DescriptorHolder descriptorHolder = readMessagingDescriptor();
      if (descriptorHolder.getPublisherDescriptor() != null && CollectionUtils.isNotEmpty(descriptorHolder.getPublisherDescriptor().getEventDescriptors())) {
        LOGGER.info("Registering events for publishers");
        List<EventDescriptor> eventDescriptors = descriptorHolder.getPublisherDescriptor().getEventDescriptors();
        result = registerEventTypes(client, eventDescriptors).thenCompose((ar) -> registerPublishers(client, descriptorHolder.getPublisherDescriptor()));
      }

      if (descriptorHolder.getSubscriberDescriptor() != null && CollectionUtils.isNotEmpty(descriptorHolder.getSubscriberDescriptor().getSubscriptionDefinitions())) {
        result = result.thenCompose((ar) -> registerSubscribers(client, descriptorHolder.getSubscriberDescriptor()));
      }
    } catch (Throwable var5) {
      LOGGER.error("Error during registration module in PubSub", var5);
      result.completeExceptionally(var5);
    }

    return result;
  }

  private static CompletableFuture<Void> registerEventTypes(PubsubClient client, List<EventDescriptor> events) {
    ArrayList list = new ArrayList();

    try {
      Iterator var3 = events.iterator();

      while(var3.hasNext()) {
        EventDescriptor eventDescriptor = (EventDescriptor)var3.next();
        CompletableFuture<Boolean> future = new CompletableFuture();
        client.postPubsubEventTypes((String)null, eventDescriptor, (ar) -> {

          // added check for null result
          if (ar.result() == null || ((HttpResponse)ar.result()).statusCode() == HttpStatus.HTTP_CREATED.toInt()) {
            future.complete(true);
          } else {
            ModuleRegistrationException exception = new ModuleRegistrationException(String.format("EventDescriptor was not registered for eventType: %s . Status code: %s", eventDescriptor.getEventType(),

              // added check for null result
              ar.result() == null ? ((HttpResponse)ar.result()).statusCode() : HttpStatus.HTTP_NO_CONTENT.toInt()));
            LOGGER.error(exception);
            future.completeExceptionally(exception);
          }

        });
        list.add(future);
      }
    } catch (Exception var6) {
      CompletableFuture<Void> future = new CompletableFuture();
      LOGGER.error("Module's events were not registered in PubSub.", var6);
      future.completeExceptionally(var6);
      return future;
    }

    return CompletableFuture.allOf((CompletableFuture[])list.toArray(new CompletableFuture[list.size()]));
  }

  private static CompletableFuture<Boolean> registerSubscribers(PubsubClient client, SubscriberDescriptor descriptor) {
    LOGGER.info("Registering module's subscribers");
    CompletableFuture subscribersResult = new CompletableFuture();

    try {
      client.postPubsubEventTypesDeclareSubscriber(descriptor, (ar) -> {
        if (ar.result() == null || ((HttpResponse)ar.result()).statusCode() == HttpStatus.HTTP_CREATED.toInt()) {
          LOGGER.info("Module's subscribers were successfully registered");
          subscribersResult.complete(true);
        } else {
          ModuleRegistrationException exception = new ModuleRegistrationException("Module's subscribers were not registered in PubSub. HTTP status: " + ((HttpResponse)ar.result()).statusCode());
          LOGGER.error(exception);
          subscribersResult.completeExceptionally(exception);
        }

      });
    } catch (Exception var4) {
      LOGGER.error("Module's subscribers were not registered in PubSub.", var4);
      subscribersResult.completeExceptionally(var4);
    }

    return subscribersResult;
  }

  private static CompletableFuture<Boolean> registerPublishers(PubsubClient client, PublisherDescriptor descriptor) {
    LOGGER.info("Registering module's publishers");
    CompletableFuture publishersResult = new CompletableFuture();

    try {
      client.postPubsubEventTypesDeclarePublisher(descriptor, (ar) -> {
        if (ar.result() == null || ((HttpResponse)ar.result()).statusCode() == HttpStatus.HTTP_CREATED.toInt()) {
          LOGGER.info("Module's publishers were successfully registered");
          publishersResult.complete(true);
        } else {
          ModuleRegistrationException exception = new ModuleRegistrationException("Module's publishers were not registered in PubSub. HTTP status: " + ((HttpResponse)ar.result()).statusCode());
          LOGGER.error(exception);
          publishersResult.completeExceptionally(exception);
        }

      });
    } catch (Exception var4) {
      LOGGER.error("Module's publishers were not registered in PubSub.", var4);
      publishersResult.completeExceptionally(var4);
    }

    return publishersResult;
  }

  public static CompletableFuture<Boolean> unregisterModule(OkapiConnectionParams params) {
    PubsubClient client = new PubsubClient(params.getOkapiUrl(), params.getTenantId(), params.getToken());
    String moduleId = constructModuleName();
    return unregisterModuleByIdAndRole(client, moduleId, ModuleRole.PUBLISHER).thenCompose((ar) -> unregisterModuleByIdAndRole(client, moduleId, ModuleRole.SUBSCRIBER));
  }

  private static CompletableFuture<Boolean> unregisterModuleByIdAndRole(PubsubClient client, String moduleId, ModuleRole moduleRole) {
    Promise<Boolean> promise = Promise.promise();
    CompletableFuture future = new CompletableFuture();

    try {
      LOGGER.info("Trying to unregister module with name '{}' as {}", moduleId, moduleRole);
      client.deletePubsubMessagingModules(moduleId, moduleRole.value(), (response) -> {
        if (response.result() == null || response.result().statusCode() == HttpStatus.HTTP_NO_CONTENT.toInt()) {
          LOGGER.info("Module {} was successfully unregistered as '{}'", moduleId, moduleRole);
          future.complete(true);
        } else {
          String msg = String.format("Module %s was not unregistered as '%s' in PubSub. HTTP status: %s", moduleId, moduleRole, response.result().statusCode());
          LOGGER.error(msg);
          future.completeExceptionally(new ModuleUnregistrationException(msg));
        }

      });
    } catch (Exception var6) {
      LOGGER.error("Module was not unregistered as '{}' in PubSub.", moduleRole, var6);
      promise.fail(var6);
      future.completeExceptionally(var6);
    }

    return future;
  }

  static DescriptorHolder readMessagingDescriptor() throws Throwable {
    ObjectMapper objectMapper = new ObjectMapper();

    try {
      MessagingDescriptor messagingDescriptor = objectMapper.readValue(getMessagingDescriptorInputStream(), MessagingDescriptor.class);
      return (new DescriptorHolder()).withPublisherDescriptor((new PublisherDescriptor()).withModuleId(constructModuleName()).withEventDescriptors(messagingDescriptor.getPublications())).withSubscriberDescriptor((new SubscriberDescriptor()).withModuleId(constructModuleName()).withSubscriptionDefinitions(messagingDescriptor.getSubscriptions()));
    } catch (JsonMappingException | JsonParseException var3) {
      String errorMessage = "Can not read messaging descriptor, cause: " + var3.getMessage();
      LOGGER.error(errorMessage);
      throw new IllegalArgumentException(var3);
    }
  }

  private static InputStream getMessagingDescriptorInputStream() throws Throwable {
    return (InputStream)((Optional) Optional.ofNullable(System.getProperty("messaging_config_path")).flatMap(PubSubClientUtils::getFileInputStreamByParentPath).or(() -> getFileInputStreamFromClassPath("MessagingDescriptor.json"))).orElseThrow(() -> new MessagingDescriptorNotFoundException("Messaging descriptor file 'MessagingDescriptor.json' not found"));
  }

  private static Optional<InputStream> getFileInputStreamByParentPath(String parentPath) {
    if (Paths.get(parentPath).isAbsolute()) {
      return getFileInputStreamByAbsoluteParentPath(parentPath);
    } else {
      String fullRelativeFilePath = parentPath + File.separatorChar + "MessagingDescriptor.json";
      return getFileInputStreamFromClassPath(fullRelativeFilePath);
    }
  }

  private static Optional<InputStream> getFileInputStreamByAbsoluteParentPath(String absoluteParentPath) {
    File file = new File(absoluteParentPath, "MessagingDescriptor.json");

    try {
      return Optional.of(new FileInputStream(file));
    } catch (FileNotFoundException var3) {
      return Optional.empty();
    }
  }

  private static Optional<InputStream> getFileInputStreamFromClassPath(String path) {
    String preparedPath = path.replace('\\', '/');
    InputStream fileStream = org.folio.util.pubsub.PubSubClientUtils.class.getClassLoader().getResourceAsStream(preparedPath);
    return fileStream == null ? Optional.empty() : Optional.of(fileStream);
  }

  public static String constructModuleName() {
    String var10000 = ModuleName.getModuleName().replace("_", "-");
    return var10000 + "-" + RmbVersion.getRmbVersion();
  }
}
