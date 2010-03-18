class E002_ModifiersMethodDeclatations {
    public int publicInt() {
        return 5;
    }
    
    protected int protectedInt() {
        return 5;
    }
    
    int defaultInt() {
        return 5;
    }
    
    private int privateInt() {
        return 5;
    }
    
    public final int publicFinalInt() {
        return 5;
    }
    
    final int defaultFinalInt() {
        return 5;
    }
    
    public synchronized int publicSynchronizedInt() {
        return 5;
    }
    
    synchronized int defaultSynchronizedInt() {
        return 5;
    }
    
    public final synchronized int publicFinalSynchronizedInt() {
        return 5;
    }
    
    final synchronized int defaultFinalSynchronizedInt() {
        return 5;
    }
    
    public static int publicStaticInt() {
        return 5;
    }
    
    static int defaultStaticInt() {
        return 5;
    }
    
    public static final int publicStaticFinalInt() {
        return 5;
    }
    
    static final int defaultStaticFinalInt() {
        return 5;
    }
    
    public static final synchronized int publicStaticFinalSynchronizedInt() {
        return 5;
    }
    
    static final synchronized int defaultStaticFinalSynchronizedInt() {
        return 5;
    }
    
    public native int publicNativeInt();
    
    native int defaultNativeInt();
    
    public strictfp int publicStrictFpInt() {
        return 5;
    }
    
    strictfp int defaultStrictFpInt() {
        return 5;
    }
    
    public static final synchronized strictfp int publicLotsInt() {
        return 5;
    }
    
    static final synchronized strictfp int defaultLotsInt() {
        return 5;
    }
}