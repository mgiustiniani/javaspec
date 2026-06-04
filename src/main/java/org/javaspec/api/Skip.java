package org.javaspec.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public example method as intentionally skipped.
 * <p>
 * The reflection runner reports a skipped result without constructing the specification or running lifecycle
 * methods. When {@link Skip} and {@link Pending} are both present on the same method, {@link Skip} takes
 * precedence.
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Skip {
    /**
     * Optional skip reason. Alias for {@link #reason()}.
     */
    String value() default "";

    /**
     * Optional skip reason. When both {@link #reason()} and {@link #value()} are set, reason wins.
     */
    String reason() default "";
}
