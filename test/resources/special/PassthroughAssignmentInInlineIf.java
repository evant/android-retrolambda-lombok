class PassthroughAssignmentInInlineIf {
    {
        int x = 10;
        int y = (10 == x) ? x = 20 : x;
    }
}