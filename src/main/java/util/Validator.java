package util;

public class Validator {
    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
