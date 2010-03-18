import static java.util.Collections.*;
import java.util.*;

class H008_SimpleMethods {
    private int x;
    
    public int test(int z) {
        this.x = z;
        return -z;
    }
    
    private String name = "Test";
    
    public void set() {
        int y = test(01);
        unmodifiableSet(new HashSet<Integer>());
    }
}