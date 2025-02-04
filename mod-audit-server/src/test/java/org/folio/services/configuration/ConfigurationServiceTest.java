package org.folio.services.configuration;

import static org.folio.utils.EntityUtils.createSettingEntity;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import java.util.Collections;
import java.util.UUID;
import javax.ws.rs.NotFoundException;
import org.folio.dao.configuration.SettingDao;
import org.folio.dao.configuration.SettingEntity;
import org.folio.dao.configuration.SettingGroupDao;
import org.folio.dao.configuration.SettingValueType;
import org.folio.mapper.configuration.SettingEntityMapper;
import org.folio.mapper.configuration.SettingGroupMapper;
import org.folio.mapper.configuration.SettingMapper;
import org.folio.mapper.configuration.SettingMappers;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

  private static final String TENANT_ID = "tenantId";
  private static final String GROUP_ID = "groupId";
  private static final String SETTING_KEY = "settingKey";
  private static final String SETTING_ID = "groupId.settingKey";

  @Mock
  private SettingDao settingDao;
  @Mock
  private SettingGroupDao settingGroupDao;
  @Mock
  private SettingMappers settingMappers;
  @Mock
  private SettingValidationService validationService;

  @InjectMocks
  private ConfigurationService configurationService;

  @Test
  void getAllSettingGroups_shouldReturnSettingGroupCollection() {
    when(settingGroupDao.getAll(anyString())).thenReturn(Future.succeededFuture(Collections.emptyList()));
    when(settingMappers.getSettingGroupMapper()).thenReturn(new SettingGroupMapper());

    var result = configurationService.getAllSettingGroups(TENANT_ID);

    assertDoesNotThrow(result::result);
    verify(settingGroupDao).getAll(TENANT_ID);
  }

  @Test
  void getAllSettingsByGroupId_shouldReturnSettingCollection() {
    when(settingGroupDao.exists(anyString(), anyString())).thenReturn(Future.succeededFuture(true));
    when(settingDao.getAllByGroupId(anyString(), anyString())).thenReturn(
      Future.succeededFuture(Collections.emptyList()));
    when(settingMappers.getSettingMapper()).thenReturn(new SettingMapper());

    var result = configurationService.getAllSettingsByGroupId(GROUP_ID, TENANT_ID);

    assertDoesNotThrow(result::result);
    verify(settingGroupDao).exists(GROUP_ID, TENANT_ID);
    verify(settingDao).getAllByGroupId(GROUP_ID, TENANT_ID);
  }

  @Test
  void updateSetting_shouldUpdateSetting() {
    var setting = getSetting();
    var argumentCaptor = ArgumentCaptor.forClass(SettingEntity.class);
    var userId = UUID.randomUUID();
    when(settingDao.exists(anyString(), anyString())).thenReturn(Future.succeededFuture(true));
    when(settingDao.update(anyString(), argumentCaptor.capture(), anyString())).thenReturn(Future.succeededFuture());
    when(settingMappers.getSettingEntityMapper()).thenReturn(new SettingEntityMapper());

    var result = configurationService.updateSetting(GROUP_ID, SETTING_KEY, setting, userId.toString(), TENANT_ID);

    assertTrue(result.succeeded());
    var settingEntity = argumentCaptor.getValue();
    assertEquals(SETTING_ID, settingEntity.getId());
    assertEquals(setting.getKey(), settingEntity.getKey());
    assertEquals(setting.getValue(), settingEntity.getValue());
    assertEquals(SettingValueType.STRING, settingEntity.getType());
    assertEquals(setting.getGroupId(), settingEntity.getGroupId());
    assertEquals(setting.getDescription(), settingEntity.getDescription());
    assertEquals(userId, settingEntity.getUpdatedByUserId());
    assertNotNull(settingEntity.getUpdatedDate());

    verify(validationService).validateSetting(setting, GROUP_ID, SETTING_KEY);
    verify(settingDao).exists(SETTING_ID, TENANT_ID);
    verify(settingDao).update(eq(SETTING_ID), any(), eq(TENANT_ID));
  }

  @Test
  void updateSetting_shouldThrowNotFoundException_whenSettingDoesNotExist() {
    var setting = getSetting();
    when(settingDao.exists(anyString(), anyString())).thenReturn(Future.succeededFuture(false));
    when(settingMappers.getSettingEntityMapper()).thenReturn(new SettingEntityMapper());

    var result = configurationService.updateSetting(GROUP_ID, SETTING_KEY, setting, null, TENANT_ID);

    assertTrue(result.failed());
    assertEquals(NotFoundException.class, result.cause().getClass());
    verify(validationService).validateSetting(setting, GROUP_ID, SETTING_KEY);
    verify(settingDao).exists(SETTING_ID, TENANT_ID);
  }

  @Test
  void getSetting_shouldReturnSetting() {
    var setting = org.folio.services.configuration.Setting.INVENTORY_RECORDS_PAGE_SIZE;
    var settingEntity = createSettingEntity();
    var mapper = mock(SettingMapper.class);
    var expected = getSetting();

    when(settingDao.getById(setting.getSettingId(), TENANT_ID)).thenReturn(Future.succeededFuture(settingEntity));
    when(settingMappers.getSettingMapper()).thenReturn(mapper);
    when(mapper.apply(settingEntity)).thenReturn(expected);

    var result = configurationService.getSetting(setting, TENANT_ID);

    assertEquals(expected, result.result());
  }

  private Setting getSetting() {
    var setting = new Setting();
    setting.setKey(SETTING_KEY);
    setting.setGroupId(GROUP_ID);
    setting.setType(Setting.Type.STRING);
    setting.setValue("value");
    return setting;
  }
}