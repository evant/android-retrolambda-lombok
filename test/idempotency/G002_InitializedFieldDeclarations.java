import java.util.Collections;

class G002_InitializedFieldDeclaration {
    public int publicI = 1 * 4;
    
    private static final long loadTime = System.currentTimeMillis();
    
    String[] names = {"Reinier", "Roel"};
    
    String[] lastNames = new String[] {"Zwitserloot", "Spilker"};
    
    String empty[] = {};
    
    String[] doubleEmpty[] = {{}};
    
    Collection<String> emptyCollection = Collections.emptyList();
    
    @SuppressWarnings("all")
    Collection<? extends Number> emptyCollection = Collections.emptyList();
}