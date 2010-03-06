class H005_SimpleConstructors {
    H005_SimpleConstructors(String in) {
    }
    
    H005_SimpleConstructors() {
        this("");
    }
    
    static class Inner extends H005_SimpleConstructors {
        Inner() {
            super("");
        }
    }
}