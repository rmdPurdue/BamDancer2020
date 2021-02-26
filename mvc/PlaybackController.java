package mvc;

import cues.Cue;
import cues.InputDisplay;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Alert.AlertType;
import javafx.util.converter.DoubleStringConverter;
import util.ErrorMessages;
import util.PropertyChanges;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * @author Rich Dionne
 * @author Hannah Eckert
 * @project BaMDancer
 * @package mvc
 * @date 7/4/2019
 */
public class PlaybackController implements Initializable, PropertyChangeListener {

    @FXML public TableView<Cue> cueListTableView;
    @FXML private TableColumn<Cue, Double> cueListNumberColumn;
    @FXML private TableColumn<Cue, String> cueListLabelColumn;

    @FXML private Button newCueButton;
    @FXML private Button copyCueButton;
    @FXML private Button deleteCueButton;

    @FXML public Button goButton;
    @FXML public Button stopButton;

    @FXML private FlowPane inputDisplayPane;

    private Model model;
    private String errMessage = "";
    private Cue cue = new Cue();  //TODO what is the purpose of this? Does not appear in use

    /**
     * CustomDoubleStringConverter class ensures that we can catch NumberFormatExceptions and notify
     * the user of an error if the user tries to enter a non-number for a cue number.
     * Code sourced from: https://stackoverflow.com/questions/56376182/javafx-exception-handling-in-an-editable-textfieldtablecell
     */

    public static class CustomDoubleStringConverter extends DoubleStringConverter {
        private final DoubleStringConverter converter = new DoubleStringConverter();

        @Override
        public String toString(Double object) {
            try {
                return converter.toString(object);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Data Entry Error");
                alert.setHeaderText("Error");
                alert.setContentText("This should never happen. But if it does, a NumberFormatException has occurred in CustomDoubleStringConverter's toString method.");
                alert.showAndWait();
            }
            return null;
        }

        @Override
        public Double fromString(String string) {
            try {
                return converter.fromString(string);
            } catch (NumberFormatException e) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("Data Entry Error");
                alert.setHeaderText("Error");
                alert.setContentText("Please only enter numbers in this field. A cue number with any letters in it will not be accepted.");
                alert.showAndWait();
            }
            return -1.0; //TODO ask Rich if there is anything better we could do, b/c obviously this isn't perfect.
        }
    }

    public void setModel(Model model) {
        this.model = model;
        setCueList();
        model.addPropertyChangeListener(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cueListNumberColumn.setCellValueFactory(new PropertyValueFactory<>("cueNumber"));
        cueListNumberColumn.setCellFactory(TextFieldTableCell.forTableColumn(new CustomDoubleStringConverter()));

        //Error check new cue number if it is edited

        cueListNumberColumn.setOnEditCommit(e -> {
                String newText = String.valueOf(e.getNewValue());
                if (isCueNumberValid(newText) && errMessage.equals("")) {
                    e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getNewValue());
                    model.updateCueNumber(e.getOldValue(), e.getNewValue()); //Not checking return value b/c we already know this cue is in the cueList
                }
                else {
                    //Do not update table because new value is invalid; show error message to user

                    e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getOldValue());
                    showErrorAlert(this.errMessage);
                    this.errMessage = "";
                }

                setCueList();
        });

        cueListLabelColumn.setCellValueFactory(new PropertyValueFactory<>("cueDescription"));
        cueListLabelColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        //Error check new cue description if it is edited

        cueListLabelColumn.setOnEditCommit(e -> {
            if (errCheckField("Cue Description", e.getNewValue(),"", "")) {
                //User has entered an improper value; show err message

                showErrorAlert(this.errMessage);
                this.errMessage = "";
                e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueDescription(e.getOldValue());
            }
            else {
                e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueDescription(e.getNewValue());
                model.updateCueDescription(e.getTableView().getItems().get(e.getTablePosition().getRow()).getCueNumber(), e.getNewValue());
            }
            setCueList();
        });

        cueListTableView.setPlaceholder(new Label("No cues saved."));
        cueListTableView.getSortOrder().add(cueListNumberColumn);
    }

    /**
     * Error checks that the cue number is not already one existing in our cue list.
     * @param newText -- new cue number in string form
     * @return
     */

    private boolean isCueNumberValid(String newText) {
        if(!(newText == null || newText.length() == 0)) {
            Double temp;
            try {
                temp = Double.parseDouble(newText);
            } catch (NumberFormatException e) {
                this.errMessage = this.errMessage.concat("This field will only accept numbers. Please enter a number.");
                return false;
            }
            if(model.cueExists(temp)) {  //Note that it is impossible to get here with temp being null
                //Error if cueExists already

                this.errMessage = this.errMessage.concat("The cue number you selected is already in use; please choose a different number.");
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves ArrayList of cues from the model, sets the TableView
     * with them, and then sorts. Effectively refreshes the table.
     */

    private void setCueList() {
        cueListTableView.getItems().clear();
        cueListTableView.getItems().addAll(model.getCueList());
        cueListTableView.sort();
        cueListTableView.refresh();
    }

    /**
     * Focuses (visually) on a cue passed in.
     * @param cue
     */

    private void focusCue(Cue cue) {
        if(cue != null) {
            cueListTableView.getSelectionModel().select(model.getCueList().indexOf(cue));
            cueListTableView.getFocusModel().focus(model.getCueList().indexOf(cue));
        }
    }

    /**
     * Property change handler for adding an InputDisplay to the FlowPane for the
     * playback controller, and for clearing the FlowPane for the running of a new
     * cue.
     */

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        System.out.println("Playback Controller got property change.");
        String property = e.getPropertyName();
        InputDisplay updatedInfo;
        if (property.equals(PropertyChanges.UPDATE_VIEW.toString())) {
            updatedInfo = (InputDisplay) e.getNewValue();
            inputDisplayPane.getChildren().add(updatedInfo.getDisplay());
        }
        else if (property.equals(PropertyChanges.CLEAR_PANE.toString())) {
            inputDisplayPane.getChildren().clear();
        }
    }

    /**
     * Displays an error message to the user indicating all errors which have been found in
     * the input they provided for a field.
     * @param error -- Error message as maintained by this class
     */

    private void showErrorAlert(String error) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Data Entry Error");
        alert.setHeaderText("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }

    /**
     * Generic error checking which can be used for any field. Regex optional. We show all error messages in the
     * same error popup where possible
     * @param fieldName -- Field name to help user identify which field the error occurred on
     * @param fieldVal
     * @param regex -- Optional regex expression
     * @param expectedFormat -- Required if you use regex; string to aid the user in knowing what their input should look like
     * @return -- true if any error occurs
     */

    private Boolean errCheckField(String fieldName, String fieldVal, String regex, String expectedFormat) {
        /* This function checks response length, and  formatting. All errors found will be added onto a
           running list of errors for the user. Returns True if field is invalid in some way.
         */

        Boolean failedCondition = false;

        if (fieldVal.isEmpty()) {
            this.errMessage = this.errMessage.concat(ErrorMessages.BLANK_INVALID.getErrForField(fieldName));
            failedCondition = true;
        }
        if (fieldVal.length() >= 256) {
            this.errMessage = this.errMessage.concat(ErrorMessages.LENGTH_EXCEEDED.getErrForField(fieldName));
            failedCondition = true;
        }
        if (!regex.isEmpty() && !Pattern.matches(regex, fieldVal)) {
            this.errMessage = this.errMessage.concat(ErrorMessages.BAD_FORMAT.getErrForField(fieldName)).concat("The expected format is " + expectedFormat + ".\n\n");
            failedCondition = true;
        }

        return failedCondition;
    }
}