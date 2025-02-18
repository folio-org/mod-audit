package org.folio.services.diff;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import org.folio.CopilotGenerated;
import org.folio.domain.diff.FieldChangeDto;
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

  private static Map<String, Object> getMap(Item obj) {
    return new JsonObject(Json.encode(obj)).getMap();
  }
}