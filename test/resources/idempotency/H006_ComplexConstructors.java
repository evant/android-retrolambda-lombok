class H006_ComplexConstructors<T> {
    public <V> H006_ComplexConstructors(T t, V v) {
    }
    
    {
        H006_ComplexConstructors<? extends Number> x = new <String>H006_ComplexConstructors<Integer>(0, "");
    }
    
    public H006_ComplexConstructors() {
        <String>this(null, "");
        int x = 5;
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
            int x = 5;
        }
    }
    
    class Inner3 {
        Inner3(int x) {
            System.out.println(x);
        }
        
        {
            H006_ComplexConstructors<Integer> instance = new <String>H006_ComplexConstructors<Integer>(0, "");
            Object o = instance.new Inner3(5).new <String>InnerInner3("hey");
        }
        
        class InnerInner3 {
            <D> InnerInner3(D in) {
            }
        }
    }
}