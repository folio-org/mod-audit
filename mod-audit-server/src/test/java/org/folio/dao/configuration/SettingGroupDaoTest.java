package org.folio.dao.configuration;

import static org.folio.utils.EntityUtils.TENANT_ID;
import static org.folio.utils.MockUtils.mockPostgresHandlerSuccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.folio.rest.persist.PostgresClient;
import org.folio.util.PostgresClientFactory;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SettingGroupDaoTest {

  @Mock
  private PostgresClientFactory postgresClientFactory;
  @Mock
  private PostgresClient postgresClient;
  @InjectMocks
  private SettingGroupDao settingGroupDao;

  @BeforeEach
  public void setUp() {
    when(postgresClientFactory.createInstance(TENANT_ID)).thenReturn(postgresClient);
  }

  @SuppressWarnings("unchecked")
  @Test
  void getAll_positive() {
    // given
    var query = "SELECT * FROM diku_mod_audit.setting_group ORDER BY id";
    when(postgresClient.select(query)).thenReturn(Future.succeededFuture(mock(RowSet.class)));

    // when
    settingGroupDao.getAll(TENANT_ID);

    // then
    verify(postgresClient).select(query);
  }

  @Test
  void exists_positive() {
    // given
    var groupId = "groupId";
    var query = "SELECT 1 FROM diku_mod_audit.setting_group WHERE id = $1";
    var captor = ArgumentCaptor.forClass(Tuple.class);
    mockPostgresHandlerSuccess(2).when(postgresClient).select(eq(query), captor.capture(), any());

    // when
    settingGroupDao.exists(groupId, TENANT_ID);

    // then
    assertEquals(groupId, captor.getValue().getString(0));
    verify(postgresClient).select(eq(query), any(Tuple.class), any());
  }
}
