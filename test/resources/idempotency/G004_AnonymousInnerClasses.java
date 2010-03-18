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
}