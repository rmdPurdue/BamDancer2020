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
import util.PropertyChanges;
import util.algorithms.Algorithm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

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

    @FXML private Label cueNumberDisplayLabel;
    @FXML private Label cueDescriptionDisplayLabel;

    @FXML private FlowPane inputDisplayPane;

    @FXML private TextField cueNumberTextField;
    @FXML private TextField cueLabelTextField;

    private Model model;
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

        cueListNumberColumn.setOnEditCommit(e -> { //TODO I believe this does not work & was an attempt at err checking
            if(!model.cueExists(e.getNewValue())) {

                // If we have changed the cue number such that it is different from any in the cueList, do the following

                isCueNumberValid(String.valueOf(e.getNewValue()));  //TODO Return value not used; this needs to be adapted to err check properly
                e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getNewValue());
                //TODO do we need to ensure that the model knows in some way that we have altered one of it's cues??
                setCueList();
            } else {

                //??

                isCueNumberValid(String.valueOf(e.getNewValue()));
                e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getOldValue());
                // TODO: find out why this isn't opening the editable cell.
                cueListTableView.edit(0, cueListNumberColumn);
            }
        });

        cueListLabelColumn.setCellValueFactory(new PropertyValueFactory<>("cueDescription"));
        cueListLabelColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        cueListLabelColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueDescription(e.getNewValue()));
        //TODO cueListLabelColumn needs err checking onEditCommit!

        /*cueListTableView.setRowFactory(tv -> {
            TableRow<Cue> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    //
                }
            });
            return row;
        });*/ //TODO this appears to be dead code or a partially implemented feature

        cueListTableView.setPlaceholder(new Label("No cues saved."));
        cueListTableView.getSortOrder().add(cueListNumberColumn);
    }

    private boolean isCueNumberValid(String newText) {
        if(!(newText == null || newText.length() == 0)) {
            Cue temp = new Cue(Double.parseDouble(newText), cueLabelTextField.getText());
            if(model.getCueList().contains(temp)) {
                //errorLabel.setText("Cue number already exists. Choose another number.");
                //errorLabel.setVisible(true);
                cueNumberTextField.setStyle("-fx-text-fill: red;");
                newCueButton.setDisable(true);
            } else {
                //errorLabel.setVisible(false);
                cueNumberTextField.setStyle("-fx-text-fill: black;");
                newCueButton.setDisable(false);
            }
        }
        return false;
    }


    /**
     * Retrieves ArrayList of cues from the model, sets the TODO (TableView)
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
     * @author Hannah Eckert
     *
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
}