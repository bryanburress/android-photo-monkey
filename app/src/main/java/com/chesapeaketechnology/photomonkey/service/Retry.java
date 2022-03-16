package com.chesapeaketechnology.photomonkey.service;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a Retry annotation that can be used to annotate Retrofit API calls so that they will be retried if they fail.
 *
 * @since 0.2.0
 */
@Documented
@Target(METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry
{
    /**
     * @return The max retry attempts before the API call is considered "failed" and will no longer be retried.
     */
    int max() default 3;
}
