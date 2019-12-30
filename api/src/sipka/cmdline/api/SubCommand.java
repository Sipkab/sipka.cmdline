package sipka.cmdline.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE })
@Repeatable(SubCommands.class)
public @interface SubCommand {
	String[] name();

	Class<?> type();

	boolean defaultCommand() default false;
}