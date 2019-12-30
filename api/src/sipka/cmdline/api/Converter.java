package sipka.cmdline.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
public @interface Converter {
	//xxx doc: the type to convert to is the type of the annotated element

	//XXX doc: the class which contains the method. default to annotated type, or the enclosing type of the field
	public Class<?> converter() default Converter.class;

	public String method();
}
