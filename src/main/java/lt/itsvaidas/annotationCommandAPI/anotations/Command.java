package lt.itsvaidas.annotationCommandAPI.anotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String name();
    String description();
    String[] aliases() default {};
    String permission() default "";
}
