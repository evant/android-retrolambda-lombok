public @interface H003_Annotations {
    int value() default 10;
}

@interface ComplexAnnotation {
    int x = 10;
    
    String[] v1();
    
    Class<?> clazz() default Object.class;
    
    public abstract H003_Annotations ann();
    
    String[] v2() default {"a", "b", "c"};
}