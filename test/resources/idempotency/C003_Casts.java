class C003_Casts {
    {
        Object foo;
        Object foo2 = (Object) null;
        Object foo3 = (java.lang.Object) null;
        Object foo4 = (java.util.List<?>) null;
        Object foo5 = (String[]) null;
        Object foo6 = (java.lang.String[]) null;
        Object foo7 = (java.util.List<?>[]) null;
        Object foo8 = (Object & Integer) null;
    }
}