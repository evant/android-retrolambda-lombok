public interface H001_Interfaces extends java.util.RandomAccess, java.io.Serializable {
    int field = 0;
    
    void x();
}

interface H001_Interfaces2 {
}

interface H001_Interfaces3 {
    interface InnerInterface {
        static interface InnerInnerInterface {
        }
    }
    
    class InnerClass {
        interface InnerClassInnerInterface {
        }
    }
}