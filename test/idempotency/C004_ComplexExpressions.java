class C004_ComplexExpressions {
    {
        int brace1 = 5 + (2 + 3);
        int brace2 = (5 + 2) * 3;
        int chained1 = 5 + 2 + 3;
        int chained2 = 5 * 2 + 3;
        int chained3 = 5 * -(2 + 3) + ~3 - (int) 1.1;
        double doubleCast = (double) (int) 1.1;
        int chainedAssign = chained1 = chained2 = chained3;
        int bracedAssign = 3 + (chained1 = 5);
        int chainedCast = ~(int) 1.5;
        int bracedCast = ((Object) "foo").hashCode();
        int chainedTernary = 1 == 2 ? 10 : 2 == 2 ? 1 : 2;
        int bracedTernary = 5 + (1 == 1 ? (2 == 3 ? 5 : 7) : 8);
        int postfixChained = ~brace1++;
        double postfixChained2 = (double) brace1++;
        int selfAssign = brace1;
    }
}