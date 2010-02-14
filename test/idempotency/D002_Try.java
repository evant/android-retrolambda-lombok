class D002_Try {
    {
        try {
        } finally {
        }
        try {
        } catch (Exception e) {
        }
        try {
        } catch (NullPointerException e) {
        } catch (Exception e) {
        }
        try {
        } catch (NullPointerException e) {
        } catch (Exception e) {
        } finally {
        }
        int i = 0;
        try {
            i++;
        } finally {
            i++;
        }
        try {
            i++;
        } catch (Exception e) {
            i++;
        }
        try {
            i++;
        } catch (NullPointerException e) {
            i++;
        } catch (Exception e) {
            i++;
        }
        try {
            i++;
        } catch (NullPointerException e) {
            i++;
            throw e;
        } catch (Exception e) {
            i++;
        } finally {
            i++;
        }
    }
}