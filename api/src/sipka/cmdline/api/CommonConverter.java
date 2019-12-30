package sipka.cmdline.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@Repeatable(CommonConverters.class)
public @interface CommonConverter {
	public Class<?> type();

	//XXX doc: the class which contains the method. default to annotated type
	public Class<?> converter() default CommonConverter.class;

	public String method();
}
