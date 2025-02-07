package org.folio.util.inventory;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.folio.CopilotGenerated;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class InventoryDeleteEventSubTypeTest {

  @Test
  void shouldReturnSoftDeleteForSoftDeleteValue() {
    assertSame(InventoryDeleteEventSubType.SOFT_DELETE, InventoryDeleteEventSubType.fromValue("SOFT_DELETE"));
  }

  @Test
  void shouldReturnUnknownForUnknownValue() {
    assertSame(InventoryDeleteEventSubType.UNKNOWN, InventoryDeleteEventSubType.fromValue("UNKNOWN"));
  }

  @Test
  void shouldReturnUnknownForNonExistentValue() {
    assertSame(InventoryDeleteEventSubType.UNKNOWN, InventoryDeleteEventSubType.fromValue("NON_EXISTENT_VALUE"));
  }
}
