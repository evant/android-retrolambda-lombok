class D002_StandardFor {
    {
        for (int i = 0; i < 10; i++) {
            continue;
        }
        for (int i = 0, j = 0; i < 10; i++, j++) {
            break;
        }
        int a = 10;
        for (a = 10, "a".toString(), a++;;) {
        }
        for (;;) {
        }
    }
}