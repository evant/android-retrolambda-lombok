class C007_ThisAndSuper {
    class Inner {
        {
            java.lang.String str = "";
            Object a = this;
            Object b = C007_ThisAndSuper.this;
            Object c = C007_ThisAndSuper.Inner.this;
        }
    }
}