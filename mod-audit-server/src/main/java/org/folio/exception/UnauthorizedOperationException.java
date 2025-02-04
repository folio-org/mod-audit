package org.folio.exception;

public class UnauthorizedOperationException extends RuntimeException {

  private static final String MESSAGE = "Permission '%s' required to perform this operation";

  public UnauthorizedOperationException(String requiredPermission) {
    super(MESSAGE.formatted(requiredPermission));
  }
}
