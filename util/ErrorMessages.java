package util;

public enum ErrorMessages implements ErrorInterface {
    /* Enum to hold error messages for common UI error states.*/
    BLANK_INVALID("This field is required."),
    LENGTH_EXCEEDED("The information you entered is too long. We cannot use this information."),
    PORT_IN_USE("The port you have selected is reserved, please choose another."),
    BAD_FORMAT("The information you have entered is not in the expected format."),
    INVALID_IP("The IP address you have entered is invalid, ensure that you have entered the correct IP.");

    private String error = null;
    ErrorMessages(String error) {
        this.error = error;
    }

    @Override
    public String getErrForField(String field) {
        return "Error on " + field + ": " + this.error;
    }
}
