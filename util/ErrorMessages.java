package util;

public enum ErrorMessages implements ErrorInterface {
    /* Enum to hold error messages for common UI error states.*/
    BLANK_INVALID("This field is required."),
    LENGTH_EXCEEDED("The information you entered is too long. We cannot use this information."),
    PORT_IN_USE("The port you have selected is reserved, please choose another.");

    private String error = null;
    ErrorMessages(String error) {
        this.error = error;
    }

    @Override
    public String getErrForField(String field) {
        return "Error on " + field + ": " + this.error;
    }
}
