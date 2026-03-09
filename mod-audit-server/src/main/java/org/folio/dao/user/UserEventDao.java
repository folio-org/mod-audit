package org.folio.dao.user;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.UUID;

public interface UserEventDao {

  Future<RowSet<Row>> save(UserAuditEntity userAuditEntity, String tenantId);

  Future<Void> deleteByUserId(UUID userId, String tenantId);

  String tableName();
}
