package org.folio.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import org.folio.dao.user.UserAuditEntity;
import org.folio.rest.jaxrs.model.UserAuditItem;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class UserAuditEntitiesToCollectionMapperTest {

  private UserAuditEntitiesToCollectionMapper mapper;
  private Function<UserAuditEntity, UserAuditItem> entityToItemMapper;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    entityToItemMapper = mock(Function.class);
    mapper = new UserAuditEntitiesToCollectionMapper(entityToItemMapper);
  }

  @Test
  void shouldMapUserAuditEntitiesToUserAuditCollection() {
    var auditEntity1 = mock(UserAuditEntity.class);
    var auditEntity2 = mock(UserAuditEntity.class);

    var auditItem1 = new UserAuditItem();
    var auditItem2 = new UserAuditItem();

    when(entityToItemMapper.apply(auditEntity1)).thenReturn(auditItem1);
    when(entityToItemMapper.apply(auditEntity2)).thenReturn(auditItem2);

    var result = mapper.apply(List.of(auditEntity1, auditEntity2));

    assertSame(auditItem1, result.getUserAuditItems().get(0));
    assertSame(auditItem2, result.getUserAuditItems().get(1));
  }

  @Test
  void shouldMapEmptyUserAuditEntitiesToEmptyUserAuditCollection() {
    var result = mapper.apply(List.of());

    assertEquals(0, result.getUserAuditItems().size());
  }
}
