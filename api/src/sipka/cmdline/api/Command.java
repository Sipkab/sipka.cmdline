package sipka.cmdline.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE })
public @interface Command {
	public boolean main() default false;

	public String className() default "";

	public String[] helpCommand() default {};
}
