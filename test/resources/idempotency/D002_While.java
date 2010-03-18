class D002_While {
    {
        while (System.currentTimeMillis() < 0) {
            continue;
        }
        while (System.currentTimeMillis() > 0) {
            break;
        }
        int i = 0;
        while (i < 10) i++;
        i = 0;
        while (i < 10) {
            i++;
        }
    }
}