package org.folio.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ListUtils {

  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    Objects.requireNonNull(source, "Collection is null");
    return source.stream().map(mapper).collect(Collectors.toList());
  }
}
