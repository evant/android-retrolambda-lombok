class H007_SuperAndThisQualifier {
    int x;
    
    void test() {
    }
    
    static class Inner extends H007_SuperAndThisQualifier {
        int y;
        
        void test() {
            super.test();
            this.y = super.x;
        }
        
        void test2() {
            this.test();
        }
        
        class InnerInner {
            {
                Inner.this.test();
                Inner.super.test();
            }
            
            void test() {
            }
        }
    }
}