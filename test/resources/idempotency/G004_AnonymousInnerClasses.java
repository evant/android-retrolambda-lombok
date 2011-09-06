class G004_AnonymousInnerClasses {
    {
        Object o = new Object() {
        };
        new java.lang.Object() {
            int x = 10;
            
            void test() {
            }
        }.test();
        int y = new java.lang.Object() {
            int go() {
                return 10;
            }
        }.go();
    }
    
    public void testMethod() {
        Object o = new Object() {
        };
        int y = 10;
        Object o2 = new Object();
    }
    
    public G004_AnonymousInnerClasses() {
        Object o = new Object() {
        };
        int y = 10;
        Object o2 = new Object();
    }
    
    static {
        Object o = new Object() {
        };
        int y = 10;
        Object o2 = new Object();
    }
    
    private final Object testObject = new Object() {
    };
}