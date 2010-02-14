class D002_Synchronized {
    {
        synchronized (this) {
        }
        int i = 0;
        synchronized (this) {
            i++;
        }
        synchronized (new int[]) {
            i++;
        }
        synchronized (new int[]) {
            synchronized (new int[]) {
                i++;
            }
        }
    }
}