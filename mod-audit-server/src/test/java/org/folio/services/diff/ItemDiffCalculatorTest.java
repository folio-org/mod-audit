package org.folio.services.diff;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.folio.CopilotGenerated;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.rest.external.EffectiveCallNumberComponents;
import org.folio.rest.external.Item;
import org.folio.util.inventory.InventoryResourceType;
import org.folio.utils.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@CopilotGenerated
class ItemDiffCalculatorTest {

  private final ItemDiffCalculator itemDiffCalculator = new ItemDiffCalculator();

  @Test
  void shouldReturnItemResourceType() {
    assertThat(itemDiffCalculator.getResourceType()).isEqualTo(InventoryResourceType.ITEM);
  }

  @Test
  void shouldInitializeInnerObjectFields() {
    // given
    var item = new Item();

    // when
    var result = itemDiffCalculator.access(item).get();

    // then
    assertThat(result.getTags()).isNotNull();
    assertThat(result.getEffectiveCallNumberComponents()).isNotNull();
    assertThat(result.getLastCheckIn()).isNotNull();
    assertThat(result.getStatus()).isNotNull();
  }

  @Test
  void shouldDetectFieldModifiedChange() {
    // given
    var oldItem = getMap(new Item().withId("1").withBarcode("Barcode 1"));
    var newItem = getMap(new Item().withId("1").withBarcode("Barcode 2"));

    // when
    var changeRecordDTO = itemDiffCalculator.calculateDiff(oldItem, newItem);

    // then
    assertThat(changeRecordDTO.getFieldChanges())
      .as("Field changes should contain barcode field modified change")
      .hasSize(1)
      .containsExactlyInAnyOrder(FieldChangeDto.modified("barcode", "barcode", "Barcode 1", "Barcode 2"));
  }

  @Test
  void testEffectiveCallNumberIsIgnored() {

    Item oldItem = new Item();
    oldItem.setItemLevelCallNumber("ABC");
    oldItem.setItemLevelCallNumberPrefix("P1");
    EffectiveCallNumberComponents effectiveCallNumberComponents = new EffectiveCallNumberComponents();
    effectiveCallNumberComponents.setCallNumber("ABC");
    effectiveCallNumberComponents.setPrefix("P1");
    oldItem.setEffectiveCallNumberComponents(effectiveCallNumberComponents);


    Item newItem = new Item();
    newItem.setItemLevelCallNumber("XYZ");
    newItem.setItemLevelCallNumberPrefix("P1");
    EffectiveCallNumberComponents effectiveCallNumberComponents1 = new EffectiveCallNumberComponents();
    effectiveCallNumberComponents1.setCallNumber("XYZ");
    newItem.setEffectiveCallNumberComponents(effectiveCallNumberComponents1);

    Map<String, Object> oldMap = getMap(oldItem);
    Map<String, Object> newMap = getMap(newItem);

    var diff = itemDiffCalculator.calculateDiff(oldMap, newMap);

    assertThat(diff.getFieldChanges()).as("Only one change (itemLevelCallNumber) should be recorded").hasSize(1);

  }

  private static Map<String, Object> getMap(Item obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}
