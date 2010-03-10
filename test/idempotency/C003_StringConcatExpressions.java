class C003_StringConcatExpressions {
    {
        String a = "foo" + "bar";
        String b = "" + "foo" + "bar" + "baz" + 10;
        String c = 10 + "" + "a" + "foo";
        String d = "foo" + "bar" + "baz";
    }
}