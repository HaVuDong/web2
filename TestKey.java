ngpublic class TestKey {
    public static void main(String[] args) throws Exception {
        try {
            new javax.crypto.spec.SecretKeySpec("".getBytes(), "HmacSHA256");
            System.out.println("SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
