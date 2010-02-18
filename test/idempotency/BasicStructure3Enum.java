public enum BasicStructure3Enum implements java.util.RandomAccess {
    A,
    B,
    C
}

enum SemiComplexEnum {
    A(10),
    B(20),
    C(30 + 10);
    
    private final int x;
    
    SemiComplexEnum(int x) {
        this.x = x;
    }
    
    public int foo() {
        return x;
    }
}

enum TrivialEnum {
}

enum ReallyComplexEnum {
    A(10) {
        void bar() {
        }
    },
    B,
    C {
        void baz() {
        }
    };
    
    ReallyComplexEnum() {
    }
    
    ReallyComplexEnum(int x) {
    }
}