package org.folio.dao.configuration;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.EntityUtils.createSettingEntity;
import static org.folio.utils.MockUtils.mockPostgresExecutionSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.MockUtils;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({VertxExtension.class, MockitoExtension.class})
class SettingDaoTest {

  @Mock
  private PostgresClientFactory postgresClientFactory;
  @Mock
  private PostgresClient postgresClient;
  @InjectMocks
  private SettingDao instanceEventDao;

  private static Stream<Arguments> updateTestData() {
    return Stream.of(
      Arguments.of(1, SettingValueType.INTEGER, "integer"),
      Arguments.of("string", SettingValueType.STRING, "text"),
      Arguments.of(true, SettingValueType.BOOLEAN, "boolean")
    );
  }

  @BeforeEach
  public void setUp() {
    when(postgresClientFactory.createInstance(TENANT_ID)).thenReturn(postgresClient);
  }

  @Test
  void getAllByGroupId_positive() {
    // given
    var groupId = "groupId";
    var query = "SELECT * FROM diku_mod_audit.setting WHERE group_id = $1 ORDER BY key";
    var captor = ArgumentCaptor.forClass(Tuple.class);
    mockPostgresExecutionSuccess(2).when(postgresClient).select(eq(query), captor.capture(), any());

    // when
    instanceEventDao.getAllByGroupId(groupId, TENANT_ID);

    // then
    assertEquals(groupId, captor.getValue().getString(0));
    verify(postgresClient).select(eq(query), any(Tuple.class), any());
  }

  @Test
  void exists_positive() {
    // given
    var settingId = "settingId";
    var query = "SELECT 1 FROM diku_mod_audit.setting WHERE id = $1";
    var captor = ArgumentCaptor.forClass(Tuple.class);
    mockPostgresExecutionSuccess(2).when(postgresClient).select(eq(query), captor.capture(), any());

    // when
    instanceEventDao.exists(settingId, TENANT_ID);

    // then
    assertEquals(settingId, captor.getValue().getString(0));
    verify(postgresClient).select(eq(query), any(Tuple.class), any());
  }

  @Test
  void getById_positive(VertxTestContext ctx) {
    // given
    var settingId = "settingId";
    var query = "SELECT * FROM diku_mod_audit.setting WHERE id = $1";
    var settingEntity = createSettingEntity();
    var captor = ArgumentCaptor.forClass(Tuple.class);

    mockPostgresExecutionSuccess(2, mockRowSet(settingEntity))
      .when(postgresClient).select(eq(query), captor.capture(), any());

    // when
    instanceEventDao.getById(settingId, TENANT_ID)
      .onComplete(ctx.succeeding(result -> {
        assertEquals(settingId, captor.getValue().getString(0));
        assertEquals(10, result.getValue());
        ctx.completeNow();
      }));
  }

  @ParameterizedTest
  @MethodSource("updateTestData")
  void update_positive(Object value, SettingValueType type, String typeValue) {
    // given
    var entity = new SettingEntity("group.key", "key", value, type, "description", "group",
      LocalDateTime.now(), UUID.randomUUID(), LocalDateTime.now(), UUID.randomUUID());
    var query = """
      UPDATE diku_mod_audit.setting SET
        key = $1,
        value = to_jsonb($2::%s),
        type = $3,
        description = $4,
        updated_by = $5,
        updated_date = $6
      WHERE id = $7""".formatted(typeValue);
    var captor = ArgumentCaptor.forClass(Tuple.class);
    mockPostgresExecutionSuccess(2).when(postgresClient).execute(eq(query), captor.capture(), any());

    // when
    instanceEventDao.update(entity.getId(), entity, TENANT_ID);

    // then
    var captorValue = captor.getValue();
    assertEquals(entity.getKey(), captorValue.getString(0));
    assertEquals(entity.getValue(), captorValue.getValue(1));
    assertEquals(entity.getType().value(), captorValue.getString(2));
    assertEquals(entity.getDescription(), captorValue.getString(3));
    assertEquals(entity.getUpdatedByUserId(), captorValue.getUUID(4));
    assertEquals(entity.getUpdatedDate(), captorValue.getLocalDateTime(5));
    assertEquals(entity.getId(), captorValue.getString(6));
  }

  private RowSet<Row> mockRowSet(SettingEntity entity) {
    var row = mock(Row.class);
    when(row.getString("type")).thenReturn(entity.getType().value());
    when(row.getInteger("value")).thenReturn((Integer) entity.getValue());
    return MockUtils.mockRowSet(row);
  }
}