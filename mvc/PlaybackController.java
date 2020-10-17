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
    private Cue cue = new Cue();

    public void setModel(Model model) {
        this.model = model;
        setCueList(null);
        model.addPropertyChangeListener(this);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cueListNumberColumn.setCellValueFactory(new PropertyValueFactory<>("cueNumber"));
        cueListNumberColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

        cueListNumberColumn.setOnEditCommit(e -> {
            if(!model.cueExists(e.getNewValue())) {
                isCueNumberValid(String.valueOf(e.getNewValue()));  //TODO Return value not used
                e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getNewValue());
                setCueList(null);
            } else {
                isCueNumberValid(String.valueOf(e.getNewValue()));
                e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueNumber(e.getOldValue());
                // TODO: find out why this isn't opening the editable cell.
                cueListTableView.edit(0, cueListNumberColumn);
            }
        });

        cueListLabelColumn.setCellValueFactory(new PropertyValueFactory<>("cueDescription"));
        cueListLabelColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        cueListLabelColumn.setOnEditCommit(e -> e.getTableView().getItems().get(e.getTablePosition().getRow()).setCueDescription(e.getNewValue()));

        cueListTableView.setRowFactory(tv -> {
            TableRow<Cue> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    //TODO DEAD CODE????
                }
            });
            return row;
        });

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

    private void setCueList(Cue cue) {
        cueListTableView.getItems().clear();
        cueListTableView.getItems().addAll(model.getCueList());
        cueListTableView.sort();
        cueListTableView.refresh();
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