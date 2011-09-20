@SuppressWarnings("all")
public enum H002_Enums implements java.util.RandomAccess {
    A,
    B,
    C
}

enum SemiComplexEnum {
    SCE_A(10),
    SCE_B(20),
    SCE_C(30 + 10);
    
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
    RCE_A(10) {
        void bar() {
        }
    },
    RCE_B,
    RCE_C {
        void baz() {
        }
    };
    
    ReallyComplexEnum() {
    }
    
    ReallyComplexEnum(int x) {
    }
}

enum AnnsOnEnumValues {
    AOEV_A,
    @Deprecated
    @SuppressWarnings("all")
    AOEV_B,
    @Deprecated
    AOEV_C
}