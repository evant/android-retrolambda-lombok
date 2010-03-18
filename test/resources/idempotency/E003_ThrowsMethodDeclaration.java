abstract class E003_ThrowsMethodDeclaration {
    public int publicThrowable() throws Throwable {
        return 5;
    }
    
    int defaultThrowable() throws Throwable {
        return 5;
    }
    
    public abstract int publicAbstractThrowable() throws Throwable;
    
    abstract int defaultAbstractThrowable() throws Throwable;
    
    public int publicThrowables() throws NullPointerException, NumberFormatException {
        return 5;
    }
    
    int defaultThrowables() throws NullPointerException, NumberFormatException {
        return 5;
    }
    
    public abstract int publicAbstractThrowables() throws NullPointerException, NumberFormatException;
    
    abstract int defaultAbstractThrowables() throws NullPointerException, NumberFormatException;
}