class E004_GenericsMethodDeclaration {
    <T> T simpleGenericReturn() {
        return null;
    }
    
    <T extends Number> T genericExtendsReturn() {
        return null;
    }
    
    <T> void simpleGenericParameter(T arg) {
    }
    
    <T extends Number> void genericExtendsParameter(T arg) {
    }
    
    <T extends Exception> void genericThrows() throws T {
    }
    
    <T extends Number & java.util.RandomAccess> void genericExtendsMoreParameter(T arg) {
    }
    
    <T, U> void genericMoreParameter(T arg0, U arg1) {
    }
    
    <T extends Number, U extends java.util.RandomAccess> void genericExtendsMoreParameter(T arg0, U arg1) {
    }
}