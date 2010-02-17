class E004_ParameterMethodDeclarations {
    String[] oneParameter(int one) {
        return null;
    }
    
    String[][] oneParameter2(int one) {
        return null;
    }
    
    String[] twoParameters(int one, String two) {
        return null;
    }
    
    String[][] twoParameters2(int one, String two) {
        return null;
    }
    
    String[] varArgs(String... two) {
        return null;
    }
    
    String[][] varArgs2(String... two) {
        return null;
    }
    
    String[] varArgsArray(String[]... two) {
        return null;
    }
    
    String[][] varArgsArray2(String[]... two) {
        return null;
    }
    
    String[] simpleGenericParameter(int one, String two, java.util.List<String> three) {
        return null;
    }
    
    String[][] simpleGenericParameter2(int one, String two, java.util.List<String> three) {
        return null;
    }
    
    public String[] publicMethod(int one, String two, java.util.List<String> three, boolean isPublic) {
        return null;
    }
    
    public String[][] publicMethod2(int one, String two, java.util.List<String> three, boolean isPublic) {
        return null;
    }
    
    String[] typeArgumentExtends(java.util.List<? extends Number> list) {
        return null;
    }
    
    String[][] typeArgumentExtends2(java.util.List<? extends Number> list) {
        return null;
    }
    
    String[] typeArgumentSuper(Class<? super Number> clazz) {
        return null;
    }
    
    String[][] typeArgumentSuper2(Class<? super Number> clazz) {
        return null;
    }
    
    <R> R[] returnTypeParameter() {
        return null;
    }
    
    <R> R[][] returnTypeParameter2() {
        return null;
    }
    
    <T, R> R[] returnAndParameterTypeParameter(T arg) {
        return null;
    }
    
    <T, R> R[][] returnAndParameterTypeParameter2(T arg) {
        return null;
    }
    
    String[] throwsMethod() throws Exception {
        return null;
    }
    
    String[][] throwsMethod2() throws Exception {
        return null;
    }
    
    <T, R, E extends Exception> R[] throwsReturnsAndParameter(T arg) throws E, NullPointerException {
        return null;
    }
    
    <T, R, E extends Exception> R[][] throwsReturnsAndParameter2(T arg) throws E, NullPointerException {
        return null;
    }
}