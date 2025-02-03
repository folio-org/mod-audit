package org.folio.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

@Tag("unit")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface UnitTest {
}
