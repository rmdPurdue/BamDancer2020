package util;

public enum ErrorMessages implements ErrorInterface {
    /* Enum to hold error messages for common UI error states.*/

    BLANK_INVALID("This field is required."),
    LENGTH_EXCEEDED("The information you entered is too long. We cannot use this information."),
    BAD_FORMAT("The information you have entered is not in the expected format.");

    private String error;
    ErrorMessages(String error) {
        this.error = error;
    }

    @Override
    public String getErrForField(String field) {
        return "Error on " + field + this.error + "\n\n";
    }
}
