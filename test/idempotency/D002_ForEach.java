class D002_ForEach {
    {
        String x;
        for (String name : java.util.Collections.emptyList()) {
            x = name;
        }
        String[][] allNames = {{"Reinier", "Roel"}};
        for (String[] names : allNames) {
            for (String name : names) {
                x = name[0];
            }
        }
        for (String names[] : allNames) {
            for (String name : names) {
                x = name[0];
            }
        }
        for (String name : java.util.Collections.emptyList()) x = name;
    }
}