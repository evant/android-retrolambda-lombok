class D002_Switch {
    {
        int i = 0;
        switch (i) {
        }
        switch (i) {
        default:
            ;
        }
        switch (i) {
        case 1:
            ;
        default:
            ;
        }
        switch (i) {
        default:
            ;
        case 1:
            ;
        }
        switch (i) {
        case 1:
            {
                ;
            }
        default:
            {
            }
        }
        switch (i) {
        case 1:
            {
                break;
            }
        default:
            {
                break;
            }
        }
        switch (i) {
        case 1:
            int j = 3;
        }
        switch (i) {
        case 1:
            {
                int j = 3;
            }
        }
    }
}