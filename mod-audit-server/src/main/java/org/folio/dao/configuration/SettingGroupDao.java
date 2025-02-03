package org.folio.dao.configuration;

import static java.lang.String.format;
import static org.folio.dao.configuration.SettingGroupEntity.DESCRIPTION_COLUMN;
import static org.folio.dao.configuration.SettingGroupEntity.ID_COLUMN;
import static org.folio.dao.configuration.SettingGroupEntity.NAME_COLUMN;
import static org.folio.util.DbUtils.formatDBTableName;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.function.Function;
import org.folio.util.DbUtils;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class SettingGroupDao {

  private static final String TABLE_NAME = "setting_group";
  private static final String SELECT_ALL_SQL = "SELECT * FROM %s ORDER BY id";
  private static final String EXIST_BY_ID_SQL = "SELECT 1 FROM %s WHERE id = $1";

  private final PostgresClientFactory pgClientFactory;

  protected SettingGroupDao(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  public Future<List<SettingGroupEntity>> getAll(String tenantId) {
    var promise = Promise.<RowSet<Row>>promise();
    var query = prepareSql(SELECT_ALL_SQL, tenantId);
    pgClientFactory.createInstance(tenantId).select(query, promise);
    return promise.future().map(this::mapToSettingGroupList);
  }

  public Future<Boolean> exists(String groupId, String tenantId) {
    var promise = Promise.<RowSet<Row>>promise();
    var query = prepareSql(EXIST_BY_ID_SQL, tenantId);
    pgClientFactory.createInstance(tenantId).select(query, Tuple.of(groupId), promise);
    return promise.future().map(rowSet -> rowSet.iterator().hasNext());
  }

  private String prepareSql(String sql, String tenantId) {
    var table = formatDBTableName(tenantId, TABLE_NAME);
    return format(sql, table);
  }

  private List<SettingGroupEntity> mapToSettingGroupList(RowSet<Row> rowSet) {
    return DbUtils.streamOf(rowSet)
      .map(settingGroupMapper())
      .toList();
  }

  private Function<Row, SettingGroupEntity> settingGroupMapper() {
    return row -> new SettingGroupEntity(
      row.getString(ID_COLUMN),
      row.getString(NAME_COLUMN),
      row.getString(DESCRIPTION_COLUMN)
    );
  }
}
