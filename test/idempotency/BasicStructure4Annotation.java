public @interface BasicStructure4Annotation {
    int value() default 10;
}

@interface ComplexAnnotation {
    int x = 10;
    
    String[] v1();
    
    Class<?> clazz() default Object.class;
    
    BasicStructure4Annotation ann();
}