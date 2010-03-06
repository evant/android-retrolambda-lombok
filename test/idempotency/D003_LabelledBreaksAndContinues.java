class D003_LabelledBreaksAndContinues {
    {
        label1:
        do {
            continue label1;
        } while (System.currentTimeMillis() > 0);
        label2:
        do {
            break label2;
        } while (System.currentTimeMillis() < 0);
    }
}