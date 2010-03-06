class H007_SuperQualifier {
    void test() {
    }
    
    static class Inner extends H007_SuperQualifier {
        void test() {
            super.test();
        }
    }
}