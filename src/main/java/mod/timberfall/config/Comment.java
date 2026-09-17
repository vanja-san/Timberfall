package mod.timberfall.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Human-readable description of a {@link Config} field. The text is written as
 * a {@code //} comment directly above the matching entry in the JSON5 file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Comment {

	String value();
}
