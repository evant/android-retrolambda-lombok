class I001_ComplexAsserts1 {
    public void test() {
        while (1 > System.currentTimeMillis()) {
            if (System.currentTimeMillis() == 1) {
                assert true: "Testing asserts";
            }
        }
    }
}