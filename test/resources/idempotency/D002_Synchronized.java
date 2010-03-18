class D002_Synchronized {
    {
        synchronized (this) {
        }
        int i = 0;
        synchronized (this) {
            i++;
        }
        synchronized (new int[0]) {
            i++;
        }
        synchronized (new int[0]) {
            synchronized (new int[0]) {
                i++;
            }
        }
    }
}