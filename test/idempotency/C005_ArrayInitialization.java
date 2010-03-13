class C005_ArrayInitialization {
    {
        String[] names = {"Reinier", "Roel"};
        String[] names2 = new String[] {"Reinier", "Roel"};
        String[] names3 = new java.lang.String[] {"Reinier", "Roel"};
        int[] sized = new int[0];
        int[][] sizedTwoDimensions = new int[0][0];
        int[][] sizedTwoDimensions2 = new int[0][];
        int[][][] sizedThreeDimensions = new int[0][][];
        int[][] empty = {{}};
        int[][] ints = new int[][] {{}};
        int[] singleInts = new int[] {};
        int more[] = {};
        int[] more2[] = {{}};
    }
}