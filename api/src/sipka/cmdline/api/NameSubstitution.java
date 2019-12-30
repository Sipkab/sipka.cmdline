package sipka.cmdline.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({})
public @interface NameSubstitution {
	public String pattern();

	public String replacement();

}