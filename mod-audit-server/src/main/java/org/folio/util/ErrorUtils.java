package org.folio.util;

import org.folio.HttpStatus;
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
        .withMessage(throwable.getCause().getLocalizedMessage())))
        .withTotalRecords(1);
  }

}
