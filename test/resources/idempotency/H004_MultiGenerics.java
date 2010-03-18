class H004_MultiGenerics<T> {
    class Inner<V> {
    }
    
    class Inner2 extends H004_MultiGenerics<String>.Inner<Integer> {
    }
}