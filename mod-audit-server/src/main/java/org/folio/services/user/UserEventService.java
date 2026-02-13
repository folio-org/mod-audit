package org.folio.services.user;

import io.vertx.core.Future;
import org.folio.util.user.UserEvent;

public interface UserEventService {

  Future<String> processEvent(UserEvent event, String tenantId);
}
