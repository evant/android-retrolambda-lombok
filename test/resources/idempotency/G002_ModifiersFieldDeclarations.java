class G002_ModifiersFieldDeclaration {
    public int publicI;
    
    protected int protectedI;
    
    private int privateI;
    
    final int finalI = 0;
    
    transient int transientI;
    
    volatile int volatileI;
    
    static int staticI;
    
    @SuppressWarnings("all")
    public static final transient int lots = -1;
}