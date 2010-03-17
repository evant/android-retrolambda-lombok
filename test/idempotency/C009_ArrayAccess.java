class C009_ArrayAccess {
    {
        int[] x = new int[10];
        x[9] = 5;
        int y = x[5];
        int[][] z = new int[2][1];
        z[0][0] = 1;
        ((z)[0])[0] = 1;
        int b = ((z)[0])[0];
        int c = z[0][0];
    }
}