package com.github.kilianB;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(SOURCE)
@Inherited
@Target({ TYPE, PACKAGE })
/**
 * @author Kilian
 *
 */
public @interface Experimental {
	String description();
}
