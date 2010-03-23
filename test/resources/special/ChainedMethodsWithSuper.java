class ChainedMethodsWithSuper {
    ChainedMethodsWithSuper test1() {
        return this;
    }
    
    ChainedMethodsWithSuper test2() {
        return this;
    }
    
    ChainedMethodsWithSuper test3() {
        return this;
    }
    
    ChainedMethodsWithSuper x = this;
    
    static class Inner extends ChainedMethodsWithSuper {
        {
            Object y = super.test1().x.x;
            Object z = super.x.x;
        }
    }
}