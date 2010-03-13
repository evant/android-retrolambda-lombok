import java.util.HashMap;
import java.util.List;
import java.util.Map;

class H009_ComplexMethods {
    private int x;
    
    public void noargs() {
        this.x = 0;
        return;
    }
    
    public java.util.List<?> retVal() {
        return null;
    }
    
    public void twoArgs(List<?> one, java.lang.String two) {
    }
    
    @SuppressWarnings("all")
    public <V, T extends List<? extends V>> Map<V, T> complex(V in, List<T> in2, int x) {
        return new HashMap<V, T>();
    }
    
    public void testInvocations() {
        noargs();
        retVal();
        Object o = retVal();
        this.twoArgs(new java.util.ArrayList<String>(20), "");
        new H009_ComplexMethods().<Number, java.util.ArrayList<Integer>>complex((Number) Integer.valueOf(20), new java.util.ArrayList<java.util.ArrayList<Integer>>(), 10);
    }
    
    static class H009_ComplexMethodsInners extends H009_ComplexMethods {
        @Override
        public void noargs() {
        }
    }
}