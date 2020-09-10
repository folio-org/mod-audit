package org.folio.util;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.LogEventPayload;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.FEE_FINE;
import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.ITEM_BLOCK;
import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.LOAN;
import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.MANUAL_BLOCK;
import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.NOTICE;
import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.PATRON_BLOCK;
import static org.folio.rest.jaxrs.model.LogEventPayload.LoggedObjectType.REQUEST;

public class ObjectTypeResolver {

  public static Set<String> getObjectFromQuery(String object) {
    String[] objects = StringUtils.remove(StringUtils.remove(object, "("), ")").split("AND|OR|or|and");
    return Arrays.stream(objects)
      .map(String::trim)
      .map(LogEventPayload.LoggedObjectType::fromValue)
      .map(ObjectTypeResolver::getTableNameByObjectType)
      .collect(Collectors.toSet());
  }

  public static String getTableNameByObjectType(LogEventPayload.LoggedObjectType loggedObjectType) {
    if (loggedObjectType == LOAN) {
      return "loans";
    } else if (loggedObjectType == FEE_FINE) {
      return "fees_fines";
    } else if (loggedObjectType == ITEM_BLOCK) {
      return "item_blocks";
    } else if (loggedObjectType == MANUAL_BLOCK) {
      return "manual_blocks";
    } else if(loggedObjectType == PATRON_BLOCK) {
      return "patron_blocks";
    } else if (loggedObjectType == NOTICE) {
      return "notices";
    } else if (loggedObjectType == REQUEST) {
      return "requests";
    } else {
      throw new IllegalArgumentException(String.format("Table name can't be resolved for Logged Object Type: %s", loggedObjectType.value()));
    }
  }
}
