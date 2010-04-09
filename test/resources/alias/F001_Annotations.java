class F001_Annotations {
    {
        @SuppressWarnings("all")
        int i = 5;
        @SuppressWarnings({"all", "non-existent"})
        int more = 5;
        @SuppressWarnings(value = "all")
        int single = 5;
        @SuppressWarnings(value = {"all", "non-existent"})
        int full = 5;
    }
    
    @Override
    public String toString() {
        return null;
    }
    
    @SuppressWarnings("serial")
    @Override()
    public int hashCode() {
        return 0;
    }
}