class I002_ComplexAsserts2 {
    interface Inner1 {
        class Inner2 {
            public void innerMethod() {
                if (1 == System.currentTimeMillis()) assert true: "InnerInner assert";
            }
        }
    }
    
    static class Inner3 {
        static {
            assert true: "StaticInstance";
        }
    }
}