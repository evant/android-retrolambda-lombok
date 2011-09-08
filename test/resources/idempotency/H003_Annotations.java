public @interface H003_Annotations {
    int value() default 10;
}

@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
@interface ComplexAnnotation {
    int x = 10;
    
    String[] v1();
    
    Class<?> clazz() default Object.class;
    
    Class<?>[] clazzArray() default {};
    
    public abstract H003_Annotations ann();
    
    String[] v2() default {"a", "b", "c"};
}