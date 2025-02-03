package org.folio.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.rest.jaxrs.model.InventoryAuditItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryEntitiesToAuditCollectionMapperTest {

  private InventoryEntitiesToAuditCollectionMapper mapper;
  private Function<InventoryAuditEntity, InventoryAuditItem> entityToItemMapper;

  @BeforeEach
  void setUp() {
    entityToItemMapper = mock(Function.class);
    mapper = new InventoryEntitiesToAuditCollectionMapper(entityToItemMapper);
  }

  @Test
  void shouldMapInventoryAuditEntitiesToInventoryAuditCollection() {
    var auditEntity1 = mock(InventoryAuditEntity.class);
    var auditEntity2 = mock(InventoryAuditEntity.class);

    var auditItem1 = new InventoryAuditItem();
    var auditItem2 = new InventoryAuditItem();

    when(entityToItemMapper.apply(auditEntity1)).thenReturn(auditItem1);
    when(entityToItemMapper.apply(auditEntity2)).thenReturn(auditItem2);

    var result = mapper.apply(List.of(auditEntity1, auditEntity2));

    assertSame(auditItem1, result.getInventoryAuditItems().get(0));
    assertSame(auditItem2, result.getInventoryAuditItems().get(1));
  }

  @Test
  void shouldMapEmptyInventoryAuditEntitiesToEmptyInventoryAuditCollection() {
    var result = mapper.apply(List.of());

    assertEquals(0, result.getInventoryAuditItems().size());
  }
}