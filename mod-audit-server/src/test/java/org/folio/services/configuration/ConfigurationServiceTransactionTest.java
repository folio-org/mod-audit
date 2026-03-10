package org.folio.services.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.util.DbUtils.formatDBTableName;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingGroupDao;
import org.folio.dao.configuration.SettingValueType;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.impl.UserEventDaoImpl;
import org.folio.mapper.configuration.SettingEntityMapper;
import org.folio.rest.persist.Conn;
import org.folio.mapper.configuration.SettingGroupMapper;
import org.folio.mapper.configuration.SettingMapper;
import org.folio.mapper.configuration.SettingMappers;
import org.folio.rest.impl.ApiTestBase;
import org.folio.services.user.UserAuditPurgeHandler;
import org.folio.util.PostgresClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConfigurationServiceTransactionTest extends ApiTestBase {

  private static final String TENANT_ID = "modaudittest";
  private static final String GROUP_ID = SettingGroup.USER.getId();
  private static final String SETTING_KEY = SettingKey.ENABLED.getValue();
  private static final String SETTING_ID = GROUP_ID + "." + SETTING_KEY;

  @InjectMocks
  SettingDao settingDao;
  @InjectMocks
  SettingGroupDao settingGroupDao;
  @InjectMocks
  UserEventDaoImpl userEventDao;
  @Spy
  private PostgresClientFactory postgresClientFactory = new PostgresClientFactory(Vertx.vertx());

  private SettingMappers settingMappers;
  private SettingValidationService validationService;

  @BeforeEach
  void setUp() {
    settingMappers = new SettingMappers(new SettingGroupMapper(), new SettingMapper(), new SettingEntityMapper());
    validationService = new SettingValidationService();
  }

  @SneakyThrows
  @Test
  void shouldDeleteAllRecordsAndUpdateSetting_whenAuditDisabled() {
    // given: enabled = true and some audit records exist
    setEnabled(true);
    insertUserAuditRecord();
    assertThat(countUserAuditRecords()).isGreaterThan(0);

    // when: disable audit via ConfigurationService with real cleanup handler
    var cleanupHandler = new UserAuditPurgeHandler(userEventDao);
    var service = new ConfigurationService(settingDao, settingGroupDao, settingMappers,
      validationService, List.of(cleanupHandler), postgresClientFactory);

    service.updateSetting(GROUP_ID, SETTING_KEY, buildSettingPayload(false), null, TENANT_ID)
      .toCompletionStage().toCompletableFuture().get();

    // then: records deleted and setting changed to false
    assertThat(countUserAuditRecords()).isZero();
    assertThat(getEnabledValue()).isEqualTo(false);
  }

  @SneakyThrows
  @Test
  void shouldRollbackSettingUpdate_whenHandlerFails() {
    // given: enabled = true and some audit records exist
    setEnabled(true);
    deleteAllUserAuditRecords();
    insertUserAuditRecord();
    var recordsBefore = countUserAuditRecords();
    assertThat(recordsBefore).isGreaterThan(0);

    // when: disable audit with a handler that fails
    SettingChangeHandler failingHandler = new SettingChangeHandler() {
      @Override
      public boolean isResponsible(String groupId, String settingKey) {
        return true;
      }

      @Override
      public Future<Void> onSettingChanged(Object newValue, Conn conn, String tenantId) {
        return Future.failedFuture(new RuntimeException("simulated handler failure"));
      }
    };

    var service = new ConfigurationService(settingDao, settingGroupDao, settingMappers,
      validationService, List.of(failingHandler), postgresClientFactory);

    var result = service.updateSetting(GROUP_ID, SETTING_KEY, buildSettingPayload(false), null, TENANT_ID)
      .toCompletionStage().toCompletableFuture();

    assertThatThrownBy(result::get)
      .hasMessageContaining("simulated handler failure");

    // then: setting is still true (rolled back) and records still exist
    assertThat(getEnabledValue()).isEqualTo(true);
    assertThat(countUserAuditRecords()).isEqualTo(recordsBefore);
  }

  @SneakyThrows
  private void setEnabled(boolean value) {
    var entity = SettingEntity.builder()
      .id(SETTING_ID)
      .key(SETTING_KEY)
      .value(value)
      .type(SettingValueType.BOOLEAN)
      .groupId(GROUP_ID)
      .build();
    settingDao.update(entity.getId(), entity, TENANT_ID)
      .toCompletionStage().toCompletableFuture().get();
  }

  @SneakyThrows
  private Object getEnabledValue() {
    return settingDao.getById(SETTING_ID, TENANT_ID)
      .toCompletionStage().toCompletableFuture().get()
      .getValue();
  }

  @SneakyThrows
  private void insertUserAuditRecord() {
    var entity = new UserAuditEntity(
      UUID.randomUUID(), Timestamp.from(Instant.now()), UUID.randomUUID(),
      "CREATE", UUID.randomUUID(), null);
    userEventDao.save(entity, TENANT_ID)
      .toCompletionStage().toCompletableFuture().get();
  }

  @SneakyThrows
  private long countUserAuditRecords() {
    var table = formatDBTableName(TENANT_ID, "user_audit");
    return postgresClientFactory.createInstance(TENANT_ID)
      .execute("SELECT COUNT(*) FROM " + table)
      .map(rs -> rs.iterator().next().getLong(0))
      .toCompletionStage().toCompletableFuture().get();
  }

  @SneakyThrows
  private void deleteAllUserAuditRecords() {
    var table = formatDBTableName(TENANT_ID, "user_audit");
    postgresClientFactory.createInstance(TENANT_ID)
      .execute("DELETE FROM " + table)
      .toCompletionStage().toCompletableFuture().get();
  }

  private org.folio.rest.jaxrs.model.Setting buildSettingPayload(boolean value) {
    var setting = new org.folio.rest.jaxrs.model.Setting();
    setting.setKey(SETTING_KEY);
    setting.setGroupId(GROUP_ID);
    setting.setType(org.folio.rest.jaxrs.model.Setting.Type.BOOLEAN);
    setting.setValue(value);
    return setting;
  }
}
