package org.folio.services.user;

import io.vertx.core.Future;
import java.sql.Timestamp;
import org.folio.rest.jaxrs.model.UserAuditCollection;
import org.folio.util.user.UserEvent;

public interface UserEventService {

  /**
   * Processes a UserEvent (save, update, or delete audit records)
   *
   * @param event    UserEvent to process
   * @param tenantId id of tenant
   * @return Future with event id
   */
  Future<String> processEvent(UserEvent event, String tenantId);

  /**
   * Retrieves user audit events with seek-based pagination
   *
   * @param userId   user id
   * @param eventTs  event timestamp to seek from (milliseconds), or null for first page
   * @param tenantId id of tenant
   * @return Future with UserAuditCollection
   */
  Future<UserAuditCollection> getEvents(String userId, String eventTs, String tenantId);

  Future<Void> expireRecords(String tenantId, Timestamp expireOlderThan);
}
