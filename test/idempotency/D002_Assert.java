class D002_Assert {
    {
        assert 1 < 5;
        assert false: "This assert will fail";
        
        class Local {
            {
                assert 1 < 2;
            }
        }
    }
}

class D002_OtherAssert {
    class Inner {
        {
            assert 1 < 2;
        }
    }
}