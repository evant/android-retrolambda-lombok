package lombok.ast.template;

public @interface NotChildOfNode {
	String rawFormParser() default "";
	String rawFormGenerator() default "";
	String initialValue() default "";
}
