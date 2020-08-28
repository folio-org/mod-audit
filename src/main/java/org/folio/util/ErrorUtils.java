package org.folio.util;

import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.Error;

public class ErrorUtils {
  public static Error buildError(int status, String message) {
    return new Error().withCode(HttpStatus.get(status).name()).withMessage(message);
  }
}
