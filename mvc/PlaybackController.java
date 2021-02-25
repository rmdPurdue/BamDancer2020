package mvc;

import com.OutgoingData;
import com.sun.corba.se.impl.orbutil.ObjectUtility;
import cues.Cue;
import cues.InputDisplay;
import cues.OutputMapping;
import devices.AnalogInput;
import devices.OutputAddress;
import devices.RemoteDevice;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.converter.DoubleStringConverter;
import util.DialogType;
import util.ErrorMessages;
import util.PropertyChanges;
import util.algorithms.Algorithm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static util.DialogType.DELETE_CUE;
import static util.DialogType.DELETE_MAPPING;

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

    public void setModel(Model model) {
        this.model = model;
        setCueList();
        model.addPropertyChangeListener(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cueListNumberColumn.setCellValueFactory(new PropertyValueFactory<>("cueNumber"));
        cueListNumberColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

        //Error check new cue number if it is edited

        cueListNumberColumn.setOnEditCommit(e -> {
                String newText = String.valueOf(e.getNewValue());
                genericErrCheck("Cue Number", newText, "", "");
                if (isCueNumberValid(newText) && errMessage.equals("")) {
                    e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getNewValue());
                }
                else {
                    //Do not update table because new value is invalid; show error message to user

                    e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getOldValue());
                    showErrorAlert(this.errMessage);
                    this.errMessage = "";
                } //TODO if/else is untested & may not work yet???

                //TODO do we need to ensure that the model knows in some way that we have altered one of it's cues?
                setCueList();
        });


        cueListLabelColumn.setCellValueFactory(new PropertyValueFactory<>("cueDescription"));
        cueListLabelColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        cueListLabelColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueDescription(e.getNewValue()));
        //TODO cueListLabelColumn needs err checking onEditCommit!

        cueListTableView.setPlaceholder(new Label("No cues saved."));
        cueListTableView.getSortOrder().add(cueListNumberColumn);
    }

    /**
     * Error checks that the cue number is not already one existing in our cue list.
     * @param newText
     * @return
     */

    private boolean isCueNumberValid(String newText) {
        if(!(newText == null || newText.length() == 0)) {  //Slightly redundant b/c we already did genericErrorCheck, but necessary here
            System.out.println("Getting into isCueNumberValid"); //TODO RM
            Double temp;
            try {
                temp = Double.parseDouble(newText);
            } catch (NumberFormatException e) {
                System.out.println("We caught the number exception"); //TODO RM
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
     * Checks an edited field's compliance to formatting requirements as stated formally in
     * ErrorMessages.
     * @param fieldName -- Used to notify the user which field they should fix before the edit is accepted
     * @param fieldVal
     * @param regex -- An optional field only necessary if specific format is required
     * @param expectedFormat -- An optional field used to indicate to the user how they should meet our regex expectations
     * @return -- True if any error(s) present in the field
     */

    private Boolean genericErrCheck(String fieldName, String fieldVal, String regex, String expectedFormat) {

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
}