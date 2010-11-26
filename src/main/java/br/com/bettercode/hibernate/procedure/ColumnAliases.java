/*
 *
 */
package br.com.bettercode.hibernate.procedure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for columns aliases returned in procedures.
 * 
 * @author Diego Martins
 * @version 1.0
 * @date 08/06/2010 10:39:17
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ColumnAliases {
	/**
	 * This method returns the column name aliases for a return in a procedure.
	 * 
	 * @return
	 */
	String[] nameAliases() default {""};
}
