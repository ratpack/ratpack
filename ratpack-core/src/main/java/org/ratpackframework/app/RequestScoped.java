package org.ratpackframework.app;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.*;

/**
 * Apply this to implementation classes when you want one instance per request.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
@Inherited
public @interface RequestScoped {}