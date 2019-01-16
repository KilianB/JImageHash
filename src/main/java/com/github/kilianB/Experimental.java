package com.github.kilianB;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Inherited
@Retention(SOURCE)
@Target({ TYPE, PACKAGE })
/**
 * Annotation indicating that a class or method is not ready to be used yet and
 * might change quickly between versions.
 * 
 * @author Kilian
 *
 */
public @interface Experimental {
	String value();
}
