package org.folio.dao.configuration;

import static java.lang.String.format;
import static org.folio.dao.configuration.SettingEntity.CREATED_BY_COLUMN;
import static org.folio.dao.configuration.SettingEntity.CREATED_DATE_COLUMN;
import static org.folio.dao.configuration.SettingEntity.DESCRIPTION_COLUMN;
import static org.folio.dao.configuration.SettingEntity.GROUP_ID_COLUMN;
import static org.folio.dao.configuration.SettingEntity.ID_COLUMN;
import static org.folio.dao.configuration.SettingEntity.KEY_COLUMN;
import static org.folio.dao.configuration.SettingEntity.TYPE_COLUMN;
import static org.folio.dao.configuration.SettingEntity.UPDATED_BY_COLUMN;
import static org.folio.dao.configuration.SettingEntity.UPDATED_DATE_COLUMN;
import static org.folio.dao.configuration.SettingEntity.VALUE_COLUMN;
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
public class SettingDao {

  private static final String TABLE_NAME = "setting";
  private static final String SELECT_BY_GROUP_ID_SQL = "SELECT * FROM %s WHERE group_id = $1 ORDER BY key";
  private static final String EXIST_BY_ID_SQL = "SELECT 1 FROM %s WHERE id = $1";
  private static final String SELECT_BY_ID_SQL = "SELECT * FROM %s WHERE id = $1";
  private static final String UPDATE_SQL = """
    UPDATE %s SET
      key = $1,
      value = to_jsonb($2::$type),
      type = $3,
      description = $4,
      updated_by = $5,
      updated_date = $6
    WHERE id = $7""";

  private static final String TYPE_CAST_PLACEHOLDER = "$type";

  private final PostgresClientFactory pgClientFactory;

  public SettingDao(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  public Future<List<SettingEntity>> getAllByGroupId(String groupId, String tenantId) {
    var promise = Promise.<RowSet<Row>>promise();
    var query = prepareSql(SELECT_BY_GROUP_ID_SQL, tenantId);
    pgClientFactory.createInstance(tenantId).select(query, Tuple.of(groupId), promise);
    return promise.future().map(this::mapToSettingList);
  }

  public Future<Boolean> exists(String settingId, String tenantId) {
    var promise = Promise.<RowSet<Row>>promise();
    var query = prepareSql(EXIST_BY_ID_SQL, tenantId);
    pgClientFactory.createInstance(tenantId).select(query, Tuple.of(settingId), promise);
    return promise.future().map(rowSet -> rowSet.iterator().hasNext());
  }

  public Future<SettingEntity> getById(String settingId, String tenantId) {
    var promise = Promise.<RowSet<Row>>promise();
    var query = prepareSql(SELECT_BY_ID_SQL, tenantId);
    pgClientFactory.createInstance(tenantId).select(query, Tuple.of(settingId), promise);
    return promise.future().map(rowSet -> settingMapper().apply(rowSet.iterator().next()));
  }

  public Future<Void> update(String settingId, SettingEntity entity, String tenantId) {
    var promise = Promise.<RowSet<Row>>promise();
    var query = prepareSql(UPDATE_SQL, tenantId).replace(TYPE_CAST_PLACEHOLDER, getTypeCast(entity.getType()));
    var params = Tuple.of(entity.getKey(), entity.getValue(), entity.getType().value(), entity.getDescription(),
      entity.getUpdatedByUserId(), entity.getUpdatedDate(), settingId);
    pgClientFactory.createInstance(tenantId).execute(query, params, promise);
    return promise.future().mapEmpty();
  }

  private String prepareSql(String sql, String tenantId) {
    var table = formatDBTableName(tenantId, TABLE_NAME);
    return format(sql, table);
  }

  private List<SettingEntity> mapToSettingList(RowSet<Row> rowSet) {
    return DbUtils.streamOf(rowSet)
      .map(settingMapper())
      .toList();
  }

  private Function<Row, SettingEntity> settingMapper() {
    return row -> {
      var valueType = SettingValueType.fromValue(row.getString(TYPE_COLUMN));
      return SettingEntity.builder()
        .id(row.getString(ID_COLUMN))
        .key(row.getString(KEY_COLUMN))
        .value(getValue(row, valueType))
        .type(valueType)
        .description(row.getString(DESCRIPTION_COLUMN))
        .groupId(row.getString(GROUP_ID_COLUMN))
        .createdByUserId(row.getUUID(CREATED_BY_COLUMN))
        .createdDate(row.getLocalDateTime(CREATED_DATE_COLUMN))
        .updatedByUserId(row.getUUID(UPDATED_BY_COLUMN))
        .updatedDate(row.getLocalDateTime(UPDATED_DATE_COLUMN))
        .build();
    };
  }

  private Object getValue(Row row, SettingValueType valueType) {
    return switch (valueType) {
      case STRING -> row.getString(VALUE_COLUMN);
      case INTEGER -> row.getInteger(VALUE_COLUMN);
      case BOOLEAN -> row.getBoolean(VALUE_COLUMN);
    };
  }

  private CharSequence getTypeCast(SettingValueType type) {
    return switch (type) {
      case STRING -> "text";
      case INTEGER -> "integer";
      case BOOLEAN -> "boolean";
    };
  }
}
