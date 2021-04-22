package mvc;

import cues.Cue;
import cues.InputDisplay;
import cues.OutputMapping;
import devices.AnalogInput;
import devices.OutputAddress;
import devices.RemoteDevice;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.util.converter.DoubleStringConverter;
import util.ErrorMessages;
import util.PropertyChanges;
import util.algorithms.Algorithm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.net.URL;
import java.util.Optional;
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
    @FXML private Button addMappingButton;

    @FXML public Button goButton;
    @FXML public Button stopButton;

    @FXML private FlowPane inputDisplayPane;

    private Model model;
    private String errMessage = "";
    private String oscUrlRegex = "^\\/[_A-Za-z0-9]*$";
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

        //Set up Add/Delete/Duplicate Cue Buttons

        newCueButton.setOnAction(event -> addCueDialog()); //TODO check if necessary to disable any btns (I dont thinkso)
        deleteCueButton.setOnAction(event -> deleteCueDialog());
        copyCueButton.setOnAction(event -> duplicateCueDialog());
        addMappingButton.setOnAction(event -> addMappingDialog());

    }

    /**
     * Duplicates the currently selected cue, requiring the user to select a valid cue number to give to this
     * duplicate. (No two cues may have the same cue number).
     */

    private void duplicateCueDialog() {
        if (cueListTableView.getSelectionModel().getSelectedItem() != null) {
            Dialog<Cue> dupCueDialog = new Dialog<>();
            dupCueDialog.setTitle("Duplicate the Selected Cue");
            dupCueDialog.setHeaderText("Please provide the cue number you wish the duplicate to have.");

            Label dupCueNumberLabel = new Label("Cue Number: ");
            TextField dupCueNumberTextField = new TextField(); //TODO gonna need to remember to turn this into double & check list

            // Add all fields to a gridpane for display

            GridPane pane = new GridPane();
            pane.setPadding(new Insets(10, 10, 10, 10));
            pane.setVgap(5);
            pane.setHgap(5);

            pane.add(dupCueNumberLabel, 0, 0);
            pane.add(dupCueNumberTextField, 1, 0);

            dupCueDialog.getDialogPane().setContent(pane);
            dupCueDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            //Error check field when OK clicked; force user to fix field if incorrect

            final Button btnOK = (Button) dupCueDialog.getDialogPane().lookupButton(ButtonType.OK);
            btnOK.addEventFilter(ActionEvent.ACTION, event -> {
                /* If any fields have errors, consume event. */

                if (!isCueNumberValid(dupCueNumberTextField.getText())) {
                    event.consume();
                    showErrorAlert(this.errMessage);
                    this.errMessage = "";
                }
            });

            //Create new cue if dupCueDialog was closed with a successful OK

            dupCueDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    Double cueNum;
                    try {
                        cueNum = Double.parseDouble(dupCueNumberTextField.getText());
                    } catch (NumberFormatException e) {
                        System.out.println("THIS SHOULD NEVER HAPPEN! See setResultConverter in cueDialog of PlaybackController");
                        e.printStackTrace();
                        return null;
                    }
                    Cue cue = new Cue(cueNum, cueListTableView.getSelectionModel().getSelectedItem().getCueDescription()); //TODO assuming no output mappings
                    return cue;
                }
                return null;
            });

            Optional<Cue> result = dupCueDialog.showAndWait();

            //Use results from valid OK of dialog to create a new cue in the model and refresh our Playback Cue List

            result.ifPresent(cue -> {
                model.addCue(cue); //TODO this function has a boolean return val to say if was successful or not (if you need)
                setCueList();
            });
        }
        else {
            Alert alert = new Alert(AlertType.INFORMATION,
                    "If you would like to duplicate a cue from the list first select it, then press Duplicate.",
                    ButtonType.OK);
            alert.showAndWait();
        }
    }

    /**
     * Deletes the currently selected cue. Requires the user to state that they do intend this action before
     * it will complete.
     */

    private void deleteCueDialog() {
        if (cueListTableView.getSelectionModel().getSelectedItem() == null) {
            Alert alert = new Alert(AlertType.INFORMATION,
                    "If you would like to delete a cue from the list first select it, then press Delete.",
                    ButtonType.OK);
            alert.showAndWait();
        }
        else {
            Alert alert = new Alert(AlertType.CONFIRMATION,
                    "Are you sure that you would like to delete this cue? This action cannot be undone.",
                    ButtonType.YES);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    model.getCueList().remove(cueListTableView.getSelectionModel().getSelectedItem());
                    setCueList(); //refresh table
                    //TODO may have to do something potentially to handle the deletion of output mappings??
                }
            });
        }
    }

    /**
     * Dialog for user to enter output mapping information for a cue they have created.
     */

    private void addMappingDialog() {
        Cue cue = cueListTableView.getSelectionModel().getSelectedItem();
        if (cue == null) {
            Alert alert = new Alert(AlertType.INFORMATION,
                    "If you would like to add an output mapping to a cue from the list first select it, then press Add Output Mapping.",
                    ButtonType.OK);
            alert.showAndWait();
        }
        else {
            Dialog<OutputMapping> mappingDialog = new Dialog<>();
            mappingDialog.setTitle("Add Output Mapping");
            mappingDialog.setHeaderText("Add an output mapping.");

            Label deviceToMapLabel = new Label("Device to Map: " + cue.getCueDescription());
            Label deviceInputLabel = new Label("Input to Map:");
            Label destinationDeviceLabel = new Label("Destination Device:");
            Label oscLabel = new Label("Message URL Address:");
            Label algorithmLabel = new Label("Data Algorithm to Use:");

            TextField oscAddress = new TextField();

            ComboBox<RemoteDevice> devicesToMap = new ComboBox<>(model.getSenderDevices().getDevices());

            ComboBox<AnalogInput> deviceInputs = new ComboBox<>();

            ComboBox<RemoteDevice> destinationDevices = new ComboBox<>(model.getReceiverDevices().getDevices());

            ComboBox<String> algorithms = new ComboBox<>(Algorithm.getValues());

            devicesToMap.getSelectionModel().selectFirst();
            deviceInputs.setItems(devicesToMap.getSelectionModel().getSelectedItem().getAnalogInputs());
            deviceInputs.getSelectionModel().selectFirst();
            destinationDevices.getSelectionModel().selectFirst();
            algorithms.getSelectionModel().selectFirst();

            GridPane pane = new GridPane();
            pane.setPadding(new Insets(10, 10, 10, 10));
            pane.setVgap(5);
            pane.setHgap(5);

            pane.add(deviceToMapLabel, 0, 0);
            pane.add(deviceInputLabel, 0, 1);
            pane.add(deviceInputs, 1, 1);
            pane.add(destinationDeviceLabel, 0, 2);
            pane.add(destinationDevices, 1, 2);
            pane.add(oscLabel, 0, 3);
            pane.add(oscAddress, 1, 3);
            pane.add(algorithmLabel, 0, 4);
            pane.add(algorithms, 1, 4);

            mappingDialog.getDialogPane().setContent(pane);

            mappingDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            //Error check fields when OK clicked; force user to fix incorrect fields

            final Button btnOK  = (Button) mappingDialog.getDialogPane().lookupButton(ButtonType.OK);
            btnOK.addEventFilter(ActionEvent.ACTION, event -> {
                /* If any fields have errors, consume event. */

                if (errCheckField(oscLabel.getText(), oscAddress.getText(),oscUrlRegex, "a string beginning with a / and containing only letters, numbers, or underscores")) {
                    event.consume();
                    showErrorAlert(this.errMessage);
                    this.errMessage = "";
                }
            });

            mappingDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    //TODO need to error check the String field at least to make sure it doesnt exceed length expectations!!!
                    return new OutputMapping(devicesToMap.getSelectionModel().getSelectedItem(),
                            Integer.valueOf(deviceInputs.getSelectionModel().getSelectedItem().toString()),
                            new OutputAddress(model.getReceiverDevices().getDeviceUsingMac(destinationDevices.getSelectionModel().getSelectedItem().getMacAddress()),
                                    oscAddress.getText(),
                                    Algorithm.get(algorithms.getSelectionModel().getSelectedItem())));
                }
                return null;
            });

            Optional<OutputMapping> result = mappingDialog.showAndWait();
            result.ifPresent(outputMapping -> {
                cue.addOutputMapping(outputMapping);
            });
        }
    }

    /**
     * Adds a new Cue with output mappings to the cueList in the model. Duplicate cues may not be added this way;
     * (if the user attempts to add a cue with an existing cue number, they will be notified to change it).
     */

    private void addCueDialog() {

        Dialog<Cue> cueDialog = new Dialog<>();
        cueDialog.setTitle("New Cue");
        cueDialog.setHeaderText("Create a new cue.");

        Label cueNumberLabel = new Label("Cue Number: ");
        Label cueDescriptionLabel = new Label("Cue Description: ");

        TextField cueDescriptionTextField = new TextField();
        TextField cueNumberTextField = new TextField();

        // Add all fields to a gridpane for display

        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10,10,10,10));
        pane.setVgap(5);
        pane.setHgap(5);

        pane.add(cueNumberLabel,0,0);
        pane.add(cueNumberTextField,1,0);
        pane.add(cueDescriptionLabel,0,1);
        pane.add(cueDescriptionTextField,1,1);

        cueDialog.getDialogPane().setContent(pane);

        cueDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        //Error check fields when OK clicked; force user to fix incorrect fields; if info good, show output mapping dialog

        final Button btnOK  = (Button) cueDialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOK.addEventFilter(ActionEvent.ACTION, event -> {
            /* If any fields have errors, consume event. */

            if (!isCueNumberValid(cueNumberTextField.getText()) ||
                    errCheckField("Cue Description", cueDescriptionTextField.getText(),"", "")) {
                event.consume();
                showErrorAlert(this.errMessage);
                this.errMessage = "";
            }
        });

        //Create new cue if cueDialog was closed with a successful OK

        cueDialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                Double cueNum;
                try {
                    cueNum = Double.parseDouble(cueNumberTextField.getText());
                } catch (NumberFormatException e) {
                    System.out.println("THIS SHOULD NEVER HAPPEN! See setResultConverter in cueDialog of PlaybackController");
                    e.printStackTrace();
                    return null;
                }
                Cue cue = new Cue(cueNum, cueDescriptionTextField.getText()); //TODO assuming no output mappings

                return cue;
            }
            return null;
        });

        Optional<Cue> result = cueDialog.showAndWait();

        //Use results from valid OK of dialog to create a new cue in the model and refresh our Playback Cue List

        result.ifPresent(cue -> {
            model.addCue(cue); //TODO this function has a boolean return val to say if was successful or not (if you need)
            setCueList();
        });
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
                this.errMessage = this.errMessage.concat("Error on Cue Number: This field will only accept numbers. Please enter a number.");
                return false;
            }
            if(model.cueExists(temp)) {  //Note that it is impossible to get here with temp being null
                //Error if cueExists already

                this.errMessage = this.errMessage.concat("Error on Cue Number: The cue number you selected is already in use; please choose a different number.");
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

    private void focusCue(Cue cue) { //TODO is this in use??
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
     * same error popup where possible //TODO note that this is the same function as is in DeviceSetupController!!! Duplicate code!
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