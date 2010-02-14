class D002_Do {
    {
        do {
            continue;
        } while (System.currentTimeMillis() > 0);
        do {
            break;
        } while (System.currentTimeMillis() < 0);
        int x = 0;
        do {
            x++;
        } while (x < 10);
    }
}