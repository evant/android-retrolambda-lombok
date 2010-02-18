class D002_ForEach {
    {
        String x;
        for (Object o : java.util.Collections.emptyList()) {
        }
        String[][] allNames = {{"Reinier", "Roel"}};
        for (String[] names : allNames) {
            for (String name : names) {
                x = name;
            }
        }
        for (String names[] : allNames) {
            for (String name : names) {
                x = name;
            }
        }
        Object object;
        for (Object o : java.util.Collections.emptyList()) object = o;
    }
}