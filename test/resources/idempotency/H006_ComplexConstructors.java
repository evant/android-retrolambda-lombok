class H006_ComplexConstructors<T> {
    public <V> H006_ComplexConstructors(T t, V v) {
    }
    
    {
        H006_ComplexConstructors<? extends Number> x = new <String>H006_ComplexConstructors<Integer>(0, "");
    }
    
    public H006_ComplexConstructors() {
        <String>this(null, "");
    }
    
    static class Inner1 {
        class InnerInner {
            <C> InnerInner(C x) {
            }
        }
    }
    
    static class Inner2 extends Inner1.InnerInner {
        Inner2() {
            new Inner1().<String>super("");
        }
    }
    
    class Inner3 {
        {
            H006_ComplexConstructors<Integer> instance = new <String>H006_ComplexConstructors<Integer>(0, "");
            Object o = instance.new Inner3().new InnerInner3();
        }
        
        class InnerInner3 {
        }
    }
}