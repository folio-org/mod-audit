package org.folio.util;

import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.tools.utils.ValidationHelper;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public final class ExceptionHelper {
  private static final Logger LOGGER = LogManager.getLogger();

  private ExceptionHelper() {
  }

  public static Response mapExceptionToResponse(Throwable throwable) {
    if (throwable instanceof BadRequestException) {
      return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).type("text/plain").entity(throwable.getMessage()).build();
    } else if (throwable instanceof NotFoundException) {
      return Response.status(Response.Status.NOT_FOUND.getStatusCode()).type("text/plain").entity(throwable.getMessage()).build();
    } else {
      Promise<Response> validationFuture = Promise.promise();
      ValidationHelper.handleError(throwable, validationFuture);
      if (validationFuture.future().isComplete()) {
        Response response = validationFuture.future().result();
        if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
          LOGGER.error(throwable.getMessage(), throwable);
        }

        return response;
      } else {
        LOGGER.error(throwable.getMessage(), throwable);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).type("text/plain").entity(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase()).build();
      }
    }
  }
}

