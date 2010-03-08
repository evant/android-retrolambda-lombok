class C007_ThisLiteral {
    class Inner {
        {
            java.lang.String str = "";
            Object a = this;
            Object b = C007_ThisLiteral.this;
            Object c = C007_ThisLiteral.Inner.this;
        }
    }
}