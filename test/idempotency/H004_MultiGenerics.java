class H004_MultiGenerics<T> {
    public <V> H004_MultiGenerics(T t, V v) {
    }
    
    {
        H004_MultiGenerics<? extends Number> x = new <String>H004_MultiGenerics<Integer>(0, "");
    }
    
    class Inner<V> {
    }
    
    class Inner2 extends H004_MultiGenerics<String>.Inner<Integer> {
    }
}