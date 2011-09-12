class J001_ExplicitDeclarations {
    public void method0() {
        int x;
    }
    public void method1() {
        int x, y;
    }
    public void method2() {
        class Local {
        }
    }
    public void method3() {
        int x, y;
        class Local {
        }
    }
    public void method4() {
        new Object() {
        }.toString();
    }
    public void method5() {
        for (int x = 0; x < 12; x++) {
        }
    }
    public void method6() {
        for (int x = 0, y = 2; x < 12; x++) {
        }
    }
    public void method7() {
        for (int x : new int[] {1, 2}) {
        }
    }
}