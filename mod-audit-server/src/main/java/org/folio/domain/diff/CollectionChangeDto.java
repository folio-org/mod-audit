package org.folio.domain.diff;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionChangeDto {
  private String collectionName;
  private List<CollectionItemChangeDto> itemChanges;
}
