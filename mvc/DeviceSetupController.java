package mvc;

import devices.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import util.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static util.DeviceType.MIXED;
import static util.DeviceType.RECEIVER;
import static util.DeviceType.SENDER;
import static util.OSCCommand.MAXIMUM;
import static util.OSCCommand.MINIMUM;

/**
 * Created by pujamittal on 2/7/19.
 */
public class DeviceSetupController implements Initializable, PropertyChangeListener {

    private RemoteDevice device = new RemoteDevice();
    private RemoteDevice selectedDevice;
    private DeviceList senderDeviceList = new DeviceList();
    private DeviceList receiverDeviceList = new DeviceList();
    private DeviceList devices = new DeviceList();
    private String errMessage = "";
    private String ipRegex = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";

    @FXML private TableView<RemoteDevice> deviceTableView;
    @FXML private TableColumn<RemoteDevice, String> deviceNameColumn;
    @FXML private TableColumn<RemoteDevice, String> deviceTypeColumn;

    @FXML private TextField deviceNameTextField;
    @FXML private TextField portNumberTextField;
    @FXML private Spinner<Integer> portNumberSpinner = new Spinner<Integer>();
    @FXML private TextField hubIPAddressTextField;
    @FXML private TextField ipAddressTextField;
    @FXML private Label macAddress;
    @FXML private ComboBox<DeviceType> deviceTypeComboBox;
    @FXML private TextField rxPortTextField;
    @FXML private Spinner<Integer> rxPortSpinner = new Spinner<Integer>();

    @FXML private Button editNameButton;
    @FXML private Button editRxIpButton;
    @FXML private Button editRxPortButton;
    @FXML private Button editTypeButton;
    @FXML private Button editIPAddressButton;
    @FXML private Button editPortButton;

    @FXML private Button acceptNameButton;
    @FXML private Button acceptRxIpButton;
    @FXML private Button acceptRxPortButton;
    @FXML private Button acceptTypeButton;
    @FXML private Button acceptIPAddressButton;
    @FXML private Button acceptPortButton;

    @FXML private TableView<AnalogInput> inputSettingsTableView;
    @FXML private TableColumn<AnalogInput, Integer> inputNumberColumn;
    @FXML private TableColumn<AnalogInput, Integer> minValueColumn;
    @FXML private TableColumn<AnalogInput, Integer> maxValueColumn;
    @FXML private TableColumn<AnalogInput, Integer> filterWeightColumn;
    @FXML private TableColumn<AnalogInput, Void> calibrateColumn;

    @FXML private Button saveButton;
    @FXML private Button beginScanButton;
    @FXML private Button cancelScanButton;
    @FXML private Button addDeviceButton;

    @FXML private ProgressBar scanProgress;

    private PropertyChangeSupport controllerPropertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        controllerPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void setModel(Model model) {
        this.senderDeviceList = model.getSenderDevices();
        this.receiverDeviceList = model.getReceiverDevices();
        if(!senderDeviceList.getDevices().isEmpty()) deviceTableView.getItems().addAll(senderDeviceList.getDevices());
        if(!receiverDeviceList.getDevices().isEmpty()) deviceTableView.getItems().addAll(receiverDeviceList.getDevices());
        model.addPropertyChangeListener(this);
    }

    public void initialize(URL location, ResourceBundle resources) {
        // TODO: We need to add ability to update receiver port numbers.
        // TODO: We need to add the ability to manually add devices.

        beginScanButton.setOnAction(event -> {
            controllerPropertyChangeSupport.firePropertyChange(PropertyChanges.START_SCAN.toString(), true, false);
            beginScanButton.setDisable(true);
            cancelScanButton.setDisable(false);
        });

        cancelScanButton.setOnAction(event -> controllerPropertyChangeSupport.firePropertyChange(PropertyChanges.STOP_SCAN.toString(), false, true));
        cancelScanButton.setDisable(true);

        addDeviceButton.setOnAction(event -> addDeviceDialog());

        inputSettingsTableView.setEditable(true);

        deviceNameTextField.setDisable(true);
        ipAddressTextField.setDisable(true);
        rxPortTextField.setDisable(true);

        //Set up spinners and disable

        SpinnerValueFactory<Integer> rxPortValFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8003,8999, 8003);
        rxPortSpinner.setValueFactory(rxPortValFactory);
        rxPortSpinner.setEditable(true);
        rxPortSpinner.setVisible(false);

        SpinnerValueFactory<Integer> portNumberValFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(9003, 9999, 9003);
        portNumberSpinner.setValueFactory(portNumberValFactory);
        portNumberSpinner.setEditable(true);
        portNumberSpinner.setVisible(false);

        deviceTypeComboBox.setItems(FXCollections.observableArrayList(SENDER,RECEIVER,MIXED));
        deviceTypeComboBox.setDisable(true);
        hubIPAddressTextField.setDisable(true);
        portNumberTextField.setDisable(true);

        acceptNameButton.setVisible(false);
        acceptIPAddressButton.setVisible(false);
        acceptRxIpButton.setVisible(false);
        acceptTypeButton.setVisible(false);
        acceptRxPortButton.setVisible(false);
        acceptPortButton.setVisible(false);

        editNameButton.setOnAction(e -> {
            deviceNameTextField.setDisable(false);
            acceptNameButton.setVisible(true);
            editNameButton.setDisable(true);
        });

        acceptNameButton.setOnAction(e -> {
            deviceNameTextField.setDisable(true);
            acceptNameButton.setVisible(false);
            editNameButton.setDisable(false);
            device.setDeviceName(deviceNameTextField.getText());
        });

        acceptNameButton.addEventFilter(ActionEvent.ACTION, event -> {
            /* If Name has errors, consume event. */

            if (errCheckField("Device Name: ", deviceNameTextField.getText(), "", "")) {
                event.consume();
                showErrorAlert(this.errMessage);
                this.errMessage = "";
            }
        });

        editIPAddressButton.setOnAction(e -> {
            hubIPAddressTextField.setDisable(false);
            acceptIPAddressButton.setVisible(true);
            editIPAddressButton.setDisable(true);
        });

        acceptIPAddressButton.setOnAction(e -> {
            hubIPAddressTextField.setDisable(true);
            acceptIPAddressButton.setVisible(false);
            editIPAddressButton.setDisable(false);
            //TODO err check
            try {
                device.setAddressToSendTo(InetAddress.getByName(hubIPAddressTextField.getText().replaceAll("^/+","")));
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }
        });

        acceptIPAddressButton.addEventFilter(ActionEvent.ACTION, event -> {
            /* If IP has errors, consume event. */

            if (errCheckField("Destination IP Address: ", hubIPAddressTextField.getText(), '/' + ipRegex, "/123.456.789.101")) {
                event.consume();
                showErrorAlert(this.errMessage);
                this.errMessage = "";
            }
        });

        editRxIpButton.setOnAction(e -> {
            ipAddressTextField.setDisable(false);
            acceptRxIpButton.setVisible(true);
            editRxIpButton.setDisable(true);
        });

        acceptRxIpButton.setOnAction(e -> {
            ipAddressTextField.setDisable(true);
            acceptRxIpButton.setVisible(false);
            editRxIpButton.setDisable(false);
            //TODO err check
            try {
                device.setIpAddress(InetAddress.getByName(ipAddressTextField.getText().replaceAll("^/+", "")));
            } catch (UnknownHostException e2) {
                e2.printStackTrace();
            }
        });

        acceptRxIpButton.addEventFilter(ActionEvent.ACTION, event -> {
            /* If IP has errors, consume event. */

            if (errCheckField("Device IP Address: ", ipAddressTextField.getText(), "/" + ipRegex, "123.456.789.101")) {
                event.consume();
                showErrorAlert(this.errMessage);
                this.errMessage = "";
            }
        });

        editRxPortButton.setOnAction(e -> {
            rxPortTextField.setVisible(false);
            rxPortSpinner.setVisible(true);
            acceptRxPortButton.setVisible(true);
            editRxPortButton.setDisable(true);
        });

        acceptRxPortButton.setOnAction(e -> {
            rxPortTextField.setVisible(true);
            rxPortSpinner.setVisible(false);
            acceptRxPortButton.setVisible(false);
            editRxPortButton.setDisable(false);
            device.setReceivePort(rxPortSpinner.getValue());
            rxPortTextField.setText(String.valueOf(rxPortSpinner.getValue()));
        });

        editTypeButton.setOnAction(e -> {
            deviceTypeComboBox.setDisable(false);
            acceptTypeButton.setVisible(true);
            editTypeButton.setDisable(true);
        });

        acceptTypeButton.setOnAction(e -> {
            deviceTypeComboBox.setDisable(true);
            acceptTypeButton.setVisible(false);
            editTypeButton.setDisable(false);
            if (device.getDeviceType() == SENDER) {
                senderDeviceList.removeDevice(device);
                receiverDeviceList.addDevice(device);
            }
            else if (device.getDeviceType() == RECEIVER) {
                receiverDeviceList.removeDevice(device);
                senderDeviceList.addDevice(device);
            }
            device.setDeviceType(deviceTypeComboBox.getSelectionModel().getSelectedItem());
            changeDeviceTypeDialog(device);

            //Update table of senders / recievers to reflect new info

            updateDeviceTable();

            //Ensure that no old analog input info is visible

            inputSettingsTableView.getItems().clear();
            inputSettingsTableView.setDisable(true);

            //Clearing editable fields and stating that no device is selected.

            selectedDevice = null;
            setDevice(selectedDevice);
            deviceNameTextField.setText("");
            ipAddressTextField.setText("");
            rxPortTextField.setText("");  //Spinners are already set invisible at this point
            portNumberTextField.setText("");
            hubIPAddressTextField.setText("");

        });

        editPortButton.setOnAction(e -> {
            portNumberTextField.setVisible(false);
            portNumberSpinner.setVisible(true);
            acceptPortButton.setVisible(true);
            editPortButton.setDisable(true);
        });

        acceptPortButton.setOnAction(e -> {
            portNumberTextField.setVisible(true);
            portNumberSpinner.setVisible(false);
            acceptPortButton.setVisible(false);
            editPortButton.setDisable(false);
            device.setPortToSendTo(portNumberSpinner.getValue());
            portNumberTextField.setText(String.valueOf(portNumberSpinner.getValue()));
        });

        saveButton.setOnAction(event -> controllerPropertyChangeSupport.firePropertyChange(PropertyChanges.SAVE_DEVICE.toString(), null, device));

        inputNumberColumn.setCellValueFactory(new PropertyValueFactory<>("inputNumber"));
        minValueColumn.setCellValueFactory(new PropertyValueFactory<>("minValue"));
        maxValueColumn.setCellValueFactory(new PropertyValueFactory<>("maxValue"));
        filterWeightColumn.setCellValueFactory(new PropertyValueFactory<>("filterWeight"));
        filterWeightColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        filterWeightColumn.setOnEditCommit(
                t -> t.getTableView().getItems().get(
                        t.getTablePosition().getRow()).setFilterWeight(t.getNewValue())
        );
        filterWeightColumn.setEditable(true);

        Callback<TableColumn<AnalogInput, Void>, TableCell<AnalogInput, Void>> cellFactory = new Callback<TableColumn<AnalogInput, Void>, TableCell<AnalogInput, Void>>() {
            @Override
            public TableCell<AnalogInput, Void> call(final TableColumn<AnalogInput, Void> param) {
                final AtomicReference<TableCell<AnalogInput, Void>> cell = new AtomicReference<>(new TableCell<AnalogInput, Void>() {
                    private final Button minBtn = new Button("Minimum");

                    {
                        minBtn.setOnAction((ActionEvent event) -> {
                            AnalogInput input = getTableView().getItems().get(getIndex());
                            openCalibrationAlertDialog(input, device, MINIMUM);
                        });
                    }

                    private final Button maxBtn = new Button("Maximum");

                    {
                        maxBtn.setOnAction((ActionEvent event) -> {
                            AnalogInput input = getTableView().getItems().get(getIndex());
                            openCalibrationAlertDialog(input, device, MAXIMUM);
                        });
                    }

                    HBox btnPane = new HBox(minBtn, maxBtn);

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnPane);
                        }
                    }
                });
                return cell.get();
            }
        };
        calibrateColumn.setCellFactory(cellFactory);

        /*
         * end large section
         */

        deviceNameColumn.setCellValueFactory(new PropertyValueFactory<>("deviceName"));
        deviceTypeColumn.setCellValueFactory(new PropertyValueFactory<>("deviceType"));

        deviceTableView.setRowFactory((TableView<RemoteDevice> tv) -> {
            TableRow<RemoteDevice> row = new TableRow<>();
            row.setOnMouseClicked((MouseEvent event) -> {
                //TODO: somehow the first click in this table isn't recognized.
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    selectedDevice = row.getItem();
                    setDevice(selectedDevice);
                }
            });
            return row;
        });

        if(senderDeviceList.getDevices().isEmpty() && receiverDeviceList.getDevices().isEmpty()) {
            deviceTableView.setPlaceholder(new Label("No devices found on network."));
        }
    }

    public void setDevice(RemoteDevice device) {
        // TODO: adjust this based on DeviceType
        this.device = device;
        if (device == null) {
            //This device is no longer in the device list

            inputSettingsTableView.getItems().clear();
            inputSettingsTableView.refresh();
            return;
        }
        switch(device.getDeviceType()) {
            case MIXED:
                //MIXED will fall through to sender b/c they are effectively the same here...
            case SENDER:
                deviceNameTextField.setText(device.getDeviceName());
                hubIPAddressTextField.setText(device.getAddressToSendTo().toString());
                portNumberTextField.setText(String.valueOf(device.getPortToSendTo()));
                macAddress.setText(device.getMacAddress());
                ipAddressTextField.setText(device.getIpAddress().toString());
                deviceTypeComboBox.getSelectionModel().select(0);
                rxPortTextField.setText("");

                /* Disable buttons which edit fields that sender does not have */

                editRxPortButton.setDisable(true);
                editIPAddressButton.setDisable(false);
                editPortButton.setDisable(false);

                /* Update table information for sender dev inputs / calibration etc. */

                inputSettingsTableView.getItems().clear();
                inputSettingsTableView.getItems().addAll(device.getAnalogInputs());
                inputSettingsTableView.refresh();
                inputSettingsTableView.setDisable(false);
                break;
            case RECEIVER:
                deviceNameTextField.setText(device.getDeviceName());
                macAddress.setText(device.getMacAddress());
                ipAddressTextField.setText(device.getIpAddress().toString());
                deviceTypeComboBox.getSelectionModel().select(1);
                hubIPAddressTextField.setText("");
                rxPortTextField.setText(String.valueOf(device.getReceivePort()));
                portNumberTextField.setText("");

                /* Disable buttons which edit fields that sender does not have */

                editIPAddressButton.setDisable(true);
                editPortButton.setDisable(true);
                editRxPortButton.setDisable(false);

                /* Disable table b/c receivers don't have inputs / calibration */

                inputSettingsTableView.getItems().clear();
                inputSettingsTableView.setDisable(true);
                break;
            default:
                break;
        }
    }

    public void updateDeviceTable() {
        /*
         *  If the Device List is not empty, clear the table and repopulate it with items
         *  from the Device List.
         */

        devices.setDevices(new ArrayList<>(senderDeviceList.getDevices()));
        devices.getDevices().addAll(new ArrayList<>(receiverDeviceList.getDevices()));

        //Clears list of sender and receiver devices which are connected

        deviceTableView.getItems().clear();
        deviceTableView.getItems().addAll(FXCollections.observableList(devices.getDevices()));
        deviceTableView.refresh();
    }

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

    private void changeDeviceTypeDialog(RemoteDevice device) {
        /* Dialog which handles collection of necessary information when the user changes the device type */

        Dialog<RemoteDevice> deviceDialog = new Dialog<>();
        DeviceType deviceType = device.getDeviceType();
        deviceDialog.setTitle("Change Type to " + deviceType.toString());
        deviceDialog.setHeaderText("To change this device's type to " + deviceType.toString() + ", please enter the following information.");

        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10,10,10,10));
        pane.setVgap(5);
        pane.setHgap(5);
        deviceDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        /* If device is a sender, gather sender's unique fields */

        Label destPortLabel = new Label("Destination Port: ");
        Label destIPLabel = new Label("Destination IP Address: ");
        Label numberOfAnalogInputsLabel = new Label("# of Analog Inputs: ");
        TextField destIPAddressTextField = new TextField("123.45.67.89");
        final Spinner<Integer> destPortSpinner = new Spinner<Integer>();
        SpinnerValueFactory<Integer> destPortValFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8003,8999, 8003);
        destPortSpinner.setValueFactory(destPortValFactory);
        destPortSpinner.setEditable(true);
        final Spinner<Integer> analogInputsSpinner = new Spinner<Integer>();
        SpinnerValueFactory<Integer> analogInputsValFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 1);
        analogInputsSpinner.setValueFactory(analogInputsValFactory);
        analogInputsSpinner.setEditable(true);

        if (deviceType == SENDER) {
            pane.add(destPortLabel, 0, 0);
            pane.add(destPortSpinner, 1, 0);
            pane.add(destIPLabel, 0, 1);
            pane.add(destIPAddressTextField, 1, 1);
            pane.add(numberOfAnalogInputsLabel, 0, 2);
            pane.add(analogInputsSpinner, 1, 2);

            /* Error check IP on OK button click */

            final Button btnOK = (Button) deviceDialog.getDialogPane().lookupButton(ButtonType.OK);
            btnOK.addEventFilter(ActionEvent.ACTION, event -> {
                /* If IP has errors, consume event. */

                if (errCheckField(destIPLabel.getText(), destIPAddressTextField.getText(), ipRegex, "123.456.789.101")) {
                    event.consume();
                    showErrorAlert(this.errMessage);
                    this.errMessage = "";
                }
            });
        }

        /* If device is receiver, gather the receiver's unique field */

        Label devicePortLabel = new Label("Device OSC Port: ");
        final Spinner<Integer> devicePortSpinner = new Spinner<Integer>();
        SpinnerValueFactory<Integer> devicePortValFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(9002,9999,9002);
        devicePortSpinner.setValueFactory(devicePortValFactory);
        devicePortSpinner.setEditable(true);
        if (deviceType == RECEIVER) {
            pane.add(devicePortLabel, 0, 0);
            pane.add(devicePortSpinner, 1, 0);
        }

        deviceDialog.getDialogPane().setContent(pane);

        /* Handle correctly entered information */

        deviceDialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                if (deviceType == SENDER) {
                    InetAddress destIp = null;
                    try {
                        destIp = InetAddress.getByName(destIPAddressTextField.getText());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    device.setAddressToSendTo(destIp);
                    device.setPortToSendTo(destPortSpinner.getValue());
                    device.addAnalogInputs(analogInputsSpinner.getValue());

                } else if (deviceType == RECEIVER) {
                    device.setReceivePort(devicePortSpinner.getValue());

                    /* Re-setting analog inputs to not break code if user ever changes this dev back to sender*/

                    device.clearAnalogInputs();
                }
            }
            return null;
        });
        deviceDialog.show();
    }

    private void addDeviceDialog() {
        /* NOTE that spinner values for port nums are hardcoded based on whichever port num is max in PortNumbers enum*/

        Dialog<RemoteDevice> deviceDialog = new Dialog<>();
        deviceDialog.setTitle("New Remote Device");
        deviceDialog.setHeaderText("Create a new remote device.");

        Label deviceNameLabel = new Label("Device Name: ");
        Label macAddressLabel = new Label("MAC Address: ");
        Label deviceIPAddressLabel = new Label("Device IP Address: ");
        Label deviceTypeLabel = new Label("Device Type: ");
        Label receivePortLabel = new Label("Incoming Message Port: ");
        Label sendPortLabel = new Label("Destination Port: ");
        Label sendIPAddress = new Label("Destination IP Address: ");
        Label numberOfAnalogInputsLabel = new Label("# of Analog Inputs: ");

        TextField deviceNameTextField = new TextField();
        TextField macAddressTextField = new TextField("FFFFFFFFFFFF");
        TextField deviceIPAddressTextField = new TextField("123.45.67.89");

        final Spinner<Integer> receivePortSpinner = new Spinner<Integer>();
        SpinnerValueFactory<Integer> receivePortValFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8003,8999, 8003);
        receivePortSpinner.setValueFactory(receivePortValFactory);
        receivePortSpinner.setEditable(true);

        TextField sendIPAddressTextField = new TextField("123.45.67.89");

        final Spinner<Integer> sendPortSpinner = new Spinner<Integer>();
        SpinnerValueFactory<Integer> sendPortValFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(9002,9999,9002);
        sendPortSpinner.setValueFactory(sendPortValFactory);
        sendPortSpinner.setEditable(true);

        final Spinner<Integer> analogInputsSpinner = new Spinner<Integer>();
        SpinnerValueFactory<Integer> analogInputsValFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 1);
        analogInputsSpinner.setValueFactory(analogInputsValFactory);
        analogInputsSpinner.setEditable(true);

        receivePortSpinner.setDisable(true);
        sendIPAddressTextField.setDisable(false);
        sendPortSpinner.setDisable(false);
        analogInputsSpinner.setDisable(false);

        ComboBox<DeviceType> deviceTypeComboBox = new ComboBox<>();
        deviceTypeComboBox.getItems().setAll(DeviceType.values());
        deviceTypeComboBox.getSelectionModel().selectFirst();
        deviceTypeComboBox.setOnAction(e -> {
            switch(deviceTypeComboBox.getSelectionModel().getSelectedItem()) {
                case SENDER:
                    Platform.runLater(() -> {
                        receivePortSpinner.setDisable(true);
                        sendIPAddressTextField.setDisable(false);
                        sendPortSpinner.setDisable(false);
                        analogInputsSpinner.setDisable(false);
                    });
                    break;
                case RECEIVER:
                    Platform.runLater(() -> {
                        receivePortSpinner.setDisable(false);
                        sendIPAddressTextField.setDisable(true);
                        sendPortSpinner.setDisable(true);
                        analogInputsSpinner.setDisable(true);
                    });
                    break;
                case MIXED:
                    Platform.runLater(() -> {
                        receivePortSpinner.setDisable(false);
                        sendIPAddressTextField.setDisable(false);
                        sendPortSpinner.setDisable(false);
                        analogInputsSpinner.setDisable(false);
                    });
                    break;
            }
        });

        GridPane pane = new GridPane();
        pane.setPadding(new Insets(10,10,10,10));
        pane.setVgap(5);
        pane.setHgap(5);

        pane.add(deviceTypeLabel,0,0);
        pane.add(deviceTypeComboBox,1,0);
        pane.add(deviceNameLabel,0,1);
        pane.add(deviceNameTextField,1,1);
        pane.add(macAddressLabel,0,2);
        pane.add(macAddressTextField,1,2);
        pane.add(deviceIPAddressLabel,0,3);
        pane.add(deviceIPAddressTextField,1,3);
        pane.add(receivePortLabel,0,4);
        pane.add(receivePortSpinner,1,4);
        pane.add(sendIPAddress,0,5);
        pane.add(sendIPAddressTextField,1,5);
        pane.add(sendPortLabel,0,6);
        pane.add(sendPortSpinner, 1, 6);
        pane.add(numberOfAnalogInputsLabel,0,7);
        pane.add(analogInputsSpinner,1,7);

        deviceDialog.getDialogPane().setContent(pane);

        deviceDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Button btnOK  = (Button) deviceDialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOK.addEventFilter(ActionEvent.ACTION, event -> {
            /* If any fields have errors, consume event. */

            Boolean invalidFields = false;
            if (errCheckField(deviceIPAddressLabel.getText(), deviceIPAddressTextField.getText(), ipRegex, "123.456.789.101")) {
                invalidFields = true;
            }
            if (errCheckField(sendIPAddress.getText(), sendIPAddressTextField.getText(), ipRegex, "123.456.789.101")) {
                invalidFields = true;
            }
            if (errCheckField(deviceNameLabel.getText(), deviceNameTextField.getText(), "", "")) {
                invalidFields = true;
            }
            if (errCheckField(macAddressLabel.getText(), macAddressTextField.getText(), "^([0-9A-F]{2}){6}$", "FFFFFFFFFFFF")) {
                invalidFields = true;
            }

            if (invalidFields) {
                event.consume();
                showErrorAlert(this.errMessage);
                this.errMessage = "";
            }
        });

        deviceDialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                RemoteDevice device = new RemoteDevice();

                InetAddress ip = null;
                InetAddress sendIp = null;
                try {
                    ip = InetAddress.getByName(deviceIPAddressTextField.getText());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                try {
                    sendIp = InetAddress.getByName(sendIPAddressTextField.getText());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                device.setDeviceName(deviceNameTextField.getText());
                device.setIpAddress(ip);
                device.setMacAddress(macAddressTextField.getText());
                device.setDeviceType(deviceTypeComboBox.getSelectionModel().getSelectedItem());

                switch (deviceTypeComboBox.getSelectionModel().getSelectedItem()) {
                    case SENDER:
                        device.addAnalogInputs(analogInputsSpinner.getValue());
                        device.setAddressToSendTo(sendIp);
                        device.setPortToSendTo(sendPortSpinner.getValue());
                        break;
                    case RECEIVER:
                        device.setReceivePort(receivePortSpinner.getValue());
                        break;
                    case MIXED:
                        device.addAnalogInputs(analogInputsSpinner.getValue());
                        device.setAddressToSendTo(sendIp);
                        device.setPortToSendTo(sendPortSpinner.getValue());
                        device.setReceivePort(receivePortSpinner.getValue());
                        break;
                    default:
                        break;
                }
                return device;
            }
            return null;
        });

        // TODO: Way to set parameters for inputs on sender devices?

        Optional<RemoteDevice> result = deviceDialog.showAndWait();

        result.ifPresent(device -> {
            switch(device.getDeviceType()) {
                case SENDER:
                    senderDeviceList.getDevices().add(device);
                    break;
                case RECEIVER:
                    receiverDeviceList.getDevices().add(device);
                    break;
                case MIXED:
                    senderDeviceList.getDevices().add(device);
                    receiverDeviceList.getDevices().add(device);
                    break;
            }
            updateDeviceTable();
        });

    }

    private void openCalibrationAlertDialog(AnalogInput input, RemoteDevice device, OSCCommand type) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Input Calibration");
        alert.setHeaderText("Calibrate Sensor on \"" + device.getDeviceName() + "\"");
        String rangeText = "";
        String calibrationNote = "";
        switch(type) {
            case MINIMUM:
                rangeText = "lowest";
                calibrationNote = "To calibrate low, simply hold the sensor without allowing it to bend.";
                break;
            case MAXIMUM:
                rangeText = "highest";
                calibrationNote = "To calibrate high, bend the sensor to the maximum that you expect it to bend to";
                break;
        }
        String alertText = "Set the sensor on input " + input.getInputNumber() + " to read its " + rangeText + " values. \n" +
                "When calibration begins, hold the sensor in place for at least five seconds.\n" + calibrationNote + "\n\nPress OK to begin.";
        alert.setContentText(alertText);

        Optional<ButtonType> result = alert.showAndWait();

        result.ifPresent(ButtonType -> {
            if(result.get() == javafx.scene.control.ButtonType.OK) {
                progressAlert(5);
                controllerPropertyChangeSupport.firePropertyChange(PropertyChanges.CALIBRATE.toString(), null, new DeviceToCalibrate(device, input.getInputNumber(), type));
            }
        });
    }

    private void showErrorAlert(String error) {
        /* Displays an error related to user-entered info (ie in Add Device etc...) */
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Data Entry Error");
        alert.setHeaderText("Error");
        alert.setContentText(error);
        alert.showAndWait();
    }

    private void showUpdatedInputDataAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Calibration Confirmation");
        alert.setHeaderText("Calibration complete.");
        alert.setContentText("Warning: calibration settings will not be saved when the device is powered down unless " +
                "the \"Update Firmware\" button is pressed.");
        alert.showAndWait();
    }

    private void progressAlert(int timeout) {
        Dialog dialog = new Dialog<>();
        dialog.setTitle("Calibrating...");
        ProgressIndicator progressIndicator = new ProgressIndicator();
        AnchorPane.setTopAnchor(progressIndicator,10.0);
        AnchorPane.setBottomAnchor(progressIndicator, 5.0);
        AnchorPane.setLeftAnchor(progressIndicator, 10.0);
        AnchorPane.setRightAnchor(progressIndicator, 10.0);

        AnchorPane pane = new AnchorPane();
        pane.getChildren().add(progressIndicator);

        dialog.getDialogPane().setContent(pane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
        dialog.show();

        Timeline display = new Timeline();
        display.setCycleCount(1);
        display.getKeyFrames().add(new KeyFrame(Duration.seconds(timeout), event -> dialog.close()));
        display.play();
    }

    public void displayScanFailedAlert() {
        Alert scanFailedAlert = new Alert(Alert.AlertType.WARNING, "Device scan failed. Some common reasons " +
                "for a device scan failure include the device not being connected to the local network and that the " +
                "device is connected to the local network but is unable to pair with our system. A green light on the sensor" +
                "indicates that it has properly connected to the local network. A flashing light during the device scan " +
                "indicates that the device has paired with our system." );
        scanFailedAlert.show();
    }

    public void displaySavedDeviceAlert() {
        Alert deviceSavedAlert = new Alert(Alert.AlertType.INFORMATION, "This device has been saved successfully!");
        deviceSavedAlert.show();
    }

    public ProgressBar getProgressBar() {
        return scanProgress;
    }

    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        String property = propertyChangeEvent.getPropertyName();

        if(property.equals(PropertyChanges.UPDATED_DEV_DATA.toString())) {
            updateDeviceTable();
        }
        if(property.equals(PropertyChanges.UPDATED_INPUT_DATA.toString())) {
            //This one has to do with device calibration
            setDevice(deviceTableView.getSelectionModel().getSelectedItem());
            Platform.runLater(this::showUpdatedInputDataAlert);
        }
        if(property.equals(PropertyChanges.REMOTE_DEV_SAVED.toString())) {
            if((boolean)propertyChangeEvent.getNewValue()) {
                updateDeviceTable();
                Platform.runLater(this::displaySavedDeviceAlert);
            }
        }
        if(property.equals(PropertyChanges.SCAN_FAILED.toString())) {
            Platform.runLater(this::displayScanFailedAlert);
        }
        if (property.equals(PropertyChanges.SCAN_FINISHED.toString())) {
            // Occurs after any scan, successful or not

            beginScanButton.setDisable(false);
            cancelScanButton.setDisable(true);

            //Ensure any selected devices which are no longer in the list are not accessible.

            if (selectedDevice != null && !senderDeviceList.isInDeviceList(selectedDevice)) {
                setDevice(null);
            }
        }

    }
}