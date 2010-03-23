class LesserThanPlusComma {
    void test(boolean foo, String bar) {
    }
    {
        int a = 10;
        int b = 20;
        test(a < b, "c");
    }
}