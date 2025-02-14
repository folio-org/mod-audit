package org.folio.services.diff;

import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.domain.diff.ChangeType;
import org.folio.domain.diff.CollectionChangeDto;
import org.folio.domain.diff.CollectionItemChangeDto;
import org.folio.domain.diff.FieldChangeDto;
import org.folio.util.inventory.InventoryResourceType;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.core.diff.changetype.InitialValueChange;
import org.javers.core.diff.changetype.TerminalValueChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.CollectionChange;
import org.javers.core.diff.changetype.container.ElementValueChange;
import org.javers.core.diff.changetype.container.ValueAddOrRemove;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.metamodel.object.ValueObjectId;

public abstract class DiffCalculator<T> {

  protected final Javers javers = JaversBuilder.javers()
    .withListCompareAlgorithm(ListCompareAlgorithm.AS_SET)
    .build();

  /**
   * Calculates the difference between the old and new values.
   *
   * @param oldValue the old value
   * @param newValue the new value
   * @return the difference between the old and new values or null if there is no difference
   */
  public ChangeRecordDto calculateDiff(Map<String, Object> oldValue, Map<String, Object> newValue) {
    var oldRecord = new JsonObject(oldValue).mapTo(getType());
    var newRecord = new JsonObject(newValue).mapTo(getType());
    var diff = javers.compare(access(oldRecord).get(), access(newRecord).get());
    return convert(diff.getChanges());
  }

  public abstract InventoryResourceType getResourceType();

  /**
   * Accessor for the object to be compared. May be useful to initialize some fields before comparison.
   */
  protected abstract Supplier<T> access(T value);

  protected abstract Class<T> getType();

  private ChangeRecordDto convert(Changes changes) {
    if (changes == null || changes.isEmpty()) {
      return null;
    }
    var result = new ChangeRecordDto();
    var groupedChanges = groupChanges(changes);
    var parentChanges = groupedChanges.get(getType().getName());
    var changeList = parentChanges.values().iterator().next();
    var fieldChanges = new ArrayList<FieldChangeDto>();
    var collectionChanges = new ArrayList<CollectionChangeDto>();

    for (Change change : changeList) {
      if (change instanceof ValueChange valueChange) {
        fieldChanges.add(processValueChange(valueChange));
      } else if (change instanceof CollectionChange<?> collectionChange) {
        collectionChanges.add(processCollectionChange(collectionChange, groupedChanges));
      }
    }

    result.setFieldChanges(fieldChanges);
    result.setCollectionChanges(collectionChanges);
    return result;
  }

  private CollectionChangeDto processCollectionChange(CollectionChange<?> collectionChange,
                                                      Map<String, Map<String, List<Change>>> groupedChanges) {
    Set<CollectionItemChangeDto> itemChanges = new HashSet<>();

    collectionChange.getChanges().forEach(c -> {
      if (c instanceof ElementValueChange elementValueChange) {
        itemChanges.add(processElementValueChange(elementValueChange));
      } else if (c instanceof ValueAddOrRemove valueAddOrRemove) {
        processValueAddOrRemove(valueAddOrRemove, groupedChanges).ifPresent(itemChanges::add);
      }
    });

    return new CollectionChangeDto(collectionChange.getPropertyName(), new ArrayList<>(itemChanges));
  }

  private Optional<CollectionItemChangeDto> processValueAddOrRemove(ValueAddOrRemove valueAddOrRemove,
                                                                    Map<String, Map<String, List<Change>>> groupedChanges) {
    CollectionItemChangeDto itemChange = null;
    var value = valueAddOrRemove.getValue();
    if (value instanceof ValueObjectId valueObjectId) {
      for (var change : groupedChanges.get(valueObjectId.getTypeName()).get(valueObjectId.value())) {
        if (change instanceof ValueChange valueChange) {
          itemChange = CollectionItemChangeDto.of(
            determineAddOrRemove(valueAddOrRemove, ChangeType.ADDED, ChangeType.REMOVED),
            determineAddOrRemove(valueAddOrRemove, null, valueChange.getAffectedObject().orElse(null)),
            determineAddOrRemove(valueAddOrRemove, valueChange.getAffectedObject().orElse(null), null)
          );
        }
      }
    } else {
      itemChange = determineAddOrRemove(valueAddOrRemove, CollectionItemChangeDto.of(null, value),
        CollectionItemChangeDto.of(value, null));
    }
    return Optional.ofNullable(itemChange);
  }

  private <R> R determineAddOrRemove(ValueAddOrRemove valueAdded, R ifTrue, R ifFalse) {
    return valueAdded instanceof ValueAdded ? ifTrue : ifFalse;
  }

  private CollectionItemChangeDto processElementValueChange(ElementValueChange elementValueChange) {
    return CollectionItemChangeDto.modified(
      elementValueChange.getLeftValue(),
      elementValueChange.getRightValue()
    );
  }

  private FieldChangeDto processValueChange(ValueChange valueChange) {
    return FieldChangeDto.of(
      valueChange.getPropertyName(),
      valueChange.getPropertyNameWithPath(),
      valueChange.getLeft(),
      valueChange.getRight()
    );
  }

  /**
   * Groups changes by type and object id. Main map key is type name, secondary map key is object id.
   */
  private Map<String, Map<String, List<Change>>> groupChanges(Changes changes) {
    Map<String, Map<String, List<Change>>> groupedChanges = new HashMap<>();

    for (Change change : changes) {
      String typeName;
      String objectId;
      if (change instanceof InitialValueChange || change instanceof TerminalValueChange) {
        typeName = change.getAffectedGlobalId().getTypeName();
        objectId = change.getAffectedGlobalId().value();
      } else {
        typeName = change.getAffectedGlobalId().masterObjectId().getTypeName();
        objectId = change.getAffectedGlobalId().masterObjectId().value();
      }

      groupedChanges
        .computeIfAbsent(typeName, k -> new HashMap<>())
        .computeIfAbsent(objectId, k -> new ArrayList<>())
        .add(change);
    }

    return groupedChanges;
  }

}
