package com.nemouw.specialist.profile;

import java.lang.annotation.*;

/** The authenticated specialist's id. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentSpecialistId {
}