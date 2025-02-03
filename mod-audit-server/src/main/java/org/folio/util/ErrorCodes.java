package org.folio.util;

import lombok.Getter;
import org.folio.rest.jaxrs.model.Error;

@Getter
public enum ErrorCodes {

  GENERIC_ERROR_CODE("genericError", "Generic error"),
  NOT_FOUND_ERROR_CODE("notFound", "Not found error"),
  UNAUTHORIZED_ERROR_CODE("unauthorized", "Unauthorized error"),
  VALIDATION_ERROR_CODE("validation", "Validation error");

  private final String code;
  private final String description;

  ErrorCodes(String code, String description) {
    this.code = code;
    this.description = description;
  }

  @Override
  public String toString() {
    return code + ": " + description;
  }

  public Error toError() {
    return new Error().withCode(code).withMessage(description);
  }
}
