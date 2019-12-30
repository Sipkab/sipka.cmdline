package sipka.cmdline.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface PositionalParameter {
	//non negative values will be at start, ordered by ascending value 0, 1, 2...
	//negative will be at the end, ordered by descending value -1, -2, -3...
	public int value() default 0;
}
