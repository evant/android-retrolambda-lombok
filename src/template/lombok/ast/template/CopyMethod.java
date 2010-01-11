package lombok.ast.template;

public @interface CopyMethod {
	String accessModifier() default "public";
}
