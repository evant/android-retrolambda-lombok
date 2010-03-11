class C003_StringConcatExpressions {
    {
        String a = "foo" + "bar";
        String b = "" + "foo" + "bar" + "baz" + 10;
        String b2 = ("" + "foo" + "bar" + "baz") + 10;
        String c = 10 + "" + "a" + "foo";
        String c2 = 10 + ("" + "a" + "foo");
        String c3 = "a" + ("b" + "c");
        String d = "foo" + "bar" + "baz";
        boolean e = "" + "a" + "foo" instanceof String;
        String f = new String("a" + "b" + "c");
        f += "d" + "e" + "f";
        String g = f += "d" + "e" + "f";
        boolean h = "d" + "e" + "f" == "g";
        String i = (String) "a" + "b" + "c";
        String j = "a" + "b" + 'c';
        String k = "a" + 'b' + "c";
        int l = 'a' + 'b' + 'c';
    }
}