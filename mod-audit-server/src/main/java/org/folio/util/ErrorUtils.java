package org.folio.util;

import static org.folio.util.AuditEventDBConstants.UNIQUE_CONSTRAINT_VIOLATION_CODE;

import io.vertx.core.Future;
import io.vertx.pgclient.PgException;
import javax.ws.rs.core.Response;
import org.folio.HttpStatus;
import org.folio.kafka.exception.DuplicateEventException;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;

import java.util.Collections;

public class ErrorUtils {

  private ErrorUtils(){
  }

  public static Error buildError(int status, String message) {
    return new Error().withCode(HttpStatus.get(status).name()).withMessage(message);
  }

  public static Errors buildErrors(String statusCode, Throwable throwable) {
    return new Errors().withErrors(Collections.singletonList(new Error().withCode(statusCode)
        .withMessage(throwable.getMessage())))
        .withTotalRecords(1);
  }

  public static <T> Future<T> handleFailures(Throwable throwable, String id) {
    return (throwable instanceof PgException pgException && pgException.getSqlState().equals(UNIQUE_CONSTRAINT_VIOLATION_CODE)) ?
      Future.failedFuture(new DuplicateEventException(String.format("Event with id=%s is already processed.", id))) :
      Future.failedFuture(throwable);
  }

  public static Response errorResponse(HttpStatus status, ErrorCodes errorCode, Throwable throwable) {
    return Response.status(status.toInt())
      .entity(buildErrors(errorCode.getCode(), throwable))
      .header("Content-Type", "application/json")
      .build();
  }
}
