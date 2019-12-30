package sipka.cmdline.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
//XXX doc: annotation to include common converters declared on other types
public @interface IncludeCommonConverters {

	public Class<?>[] value() default {};
}
