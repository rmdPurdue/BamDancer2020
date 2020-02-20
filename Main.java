import com.DeviceDiscoveryQuery;
import com.DiscoveryQueryListener;
import cues.Cue;
import javafx.application.Application;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mvc.*;
import osc.OSCListener;
import osc.OSCPortIn;
import util.CountdownTimer;
import devices.DeviceToCalibrate;
import devices.RemoteDevice;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application implements PropertyChangeListener {

    private Model model;
    private DeviceSetupController deviceSetupController;
    private Controller controller;
    private ExecutorService executor;
    private DiscoveryQueryListener discoveryQueryListener;
    private DeviceDiscoveryQuery deviceDiscoveryQuery;
    private CountdownTimer countdownTimer;
    private ProcessingService service;
    private StopService stopService;
    private MessageWrapper messageWrapper;

    @Override
    public void start(Stage primaryStage) throws Exception{
        /*
            Establish stage and primary controller
         */

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/mvc/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        primaryStage.setTitle("BaMDancer");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();

        /*
           Create a new data model.
         */

        model = new Model();

        /*
           Establish background threads for application services
         */

        executor = Executors.newFixedThreadPool(5);
        deviceDiscoveryQuery = new DeviceDiscoveryQuery(5);
        discoveryQueryListener = new DiscoveryQueryListener();
        countdownTimer = new CountdownTimer(5);

        /*
            Hook up controllers to model.
        */
        hookupConnections();

        /*
           Start listening for OSC messages here.
         */

        startListeningOSC();  //TODO this call already starts a reciever at port 8001, so then when we create an incListener in the Model, it finds taht the port is already occupied

        /*
           Begin start and stop services which will be used to start, stop, and pause the model
           when necessary.
         */
        service = new ProcessingService();
        stopService = new StopService();

        BooleanBinding serviceRunning = service.runningProperty().or(stopService.runningProperty());  //TODO never used
        messageWrapper = new MessageWrapper("Not Started.", "Not Connected.", false);
        //view.messagesLabel.textProperty().bind(messageWrapper.runStatusProperty());
        //view.ipConnectedLabel.textProperty().bind(service.valueProperty());
        //view.cancelButton.disableProperty().bind(messageWrapper.runningProperty().not().or(serviceRunning));

        service.messageProperty().addListener((ObservableValue<? extends String> observableValue, String oldValue, String newValue) -> messageWrapper.runStatus.set(newValue));

        stopService.messageProperty().addListener((ObservableValue<? extends String> observableValue, String oldValue, String newValue) -> messageWrapper.runStatus.set(newValue));
        //service.start(); //TODO NEED to have a STOP Somewhere too! Also, if you put it here, it will run for all screens
        //TODO if you run the line above, it will crash due to this.reciever getting set to nullptr in incListener when the model tries to instantiate incoming message queue
        //TODO so need to check first if it is getting a valid port, and if not, why is it getting set to null??
        service.setOnSucceeded(event -> System.out.println("Succeeded"));  //TODO this will never print b/c we never did service.start

        // Add close application handler to kill all threads
    }

    private void hookupConnections() throws IOException {

        /*
            Get controllers for different views.
         */

        deviceSetupController = controller.setDeviceLoader(new FXMLLoader(getClass().getResource("/mvc/deviceSetup.fxml")));
        CueListController cueListController = controller.setCueLoader(new FXMLLoader(getClass().getResource("/mvc/cueList.fxml")));
        PlaybackController playbackController = controller.setPlaybackLoader(new FXMLLoader(getClass().getResource("/mvc/playback.fxml")));

        /*
           Connect controllers to the model.
         */

        controller.setModel(model);
        deviceSetupController.setModel(model);
        cueListController.setModel(model);
        playbackController.setModel(model);

        /*
            Add property change listeners for talk back from controllers and discovery query.
         */

        deviceSetupController.addPropertyChangeListener(this);
        deviceDiscoveryQuery.addPropertyChangeListener(this);
        /*
            Bind scan network dialog box progress bar to query timeout progress.
         */

        deviceSetupController.getProgressBar().progressProperty().bind(deviceDiscoveryQuery.getPercentTimeElapsed().divide(10));

        /*
            Connect playback go button to model.
         */

        playbackController.goButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (playbackController.cueListTableView.getSelectionModel().getSelectedItems().size() != 0) {
                    if (model.resetLevels()) {
                        System.out.println("Right before goCue"); //TODO RM
                        model.goCue(new Cue(playbackController.cueListTableView.getSelectionModel().getSelectedItem())); //TODO For focused cue, goes & updates the output mapping to reflect any changes
                    }
                    System.out.println("Sending Cue.");
                    if (playbackController.cueListTableView.getSelectionModel().getTableView().getItems().size() > playbackController.cueListTableView.getSelectionModel().getSelectedIndex() + 1) {  //TODO If there is another cue below the current, focus on it
                        playbackController.cueListTableView.getSelectionModel().select(playbackController.cueListTableView.getSelectionModel().getSelectedIndex() + 1);
                    } else {
                        playbackController.cueListTableView.getSelectionModel().clearSelection();
                    }
                }
            }
        });

        /*
            Connect stop button to model
         */
        //TODO written by hannah; not super sure if it works or not....
        playbackController.stopButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                //try {
                    model.stopDisplay();  //TODO This function will stop the listener and therefore pause the playback controller's display
                //} catch (CloneNotSupportedException e) { //TODO not sure if this is the right exception...
                  //  e.printStackTrace();
                //}
            }
        });
    }

    private void startListeningOSC() throws SocketException {  //TODO tell RIch about the problem!!
        // Create an OSC receiver object on port 8001
        OSCPortIn receiver = new OSCPortIn(8001);

        // Create an OSC listener, connect to model method for parsing the message
        OSCListener listener = (time, message) -> {
            System.out.println("Received message addressed to: " + message.getAddress());
            System.out.println("Message length: " + message.getArguments().size());
            model.parseIncomingOSCMessage(message);
        };

        // Add listener for "/device_setup" messages
        receiver.addListener("/device_setup", listener);

        // Add listener for "/calibrate/low" messages
        receiver.addListener("/calibrate", listener);

        // Add listener for "/saved" messages
        receiver.addListener("/saved", listener);

        // Start listener thread
        receiver.startListening();
    }

    private void startNetworkDiscoveryScan() {
        System.out.println("Got here.");

        // Start the discovery listener thread
        executor.submit(discoveryQueryListener);

        // Start the network discovery scan thread
        executor.submit(deviceDiscoveryQuery);
    }

    private void stopNetworkDiscoveryScan() {
        // Stop the discovery scan thread
        deviceDiscoveryQuery.stopDiscovery();

        // Stop the discovery listener thread
        discoveryQueryListener.stopDiscoveryListening();
    }

    private void cleanUpAfterNetworkScan() throws IOException {

        System.out.println("Discovery Query timeout received.");

        // Stop listener for discovery responses
        discoveryQueryListener.stopDiscoveryListening();

        // Update model with found devices
        model.setSenderDevices(discoveryQueryListener.getDiscoveredDevices());

        // refresh main display
        deviceSetupController.updateDeviceTable();
    }

    private void sendSettingsToDevice(RemoteDevice device) {
        System.out.println("Remote Device to send changes to: " + device.getDeviceName());
        System.out.println("MAC Address of remote device to send changes to: " + device.getMacAddress());
        try {
            model.sendUpdateFirmwareCommand(device);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void calibrate(DeviceToCalibrate deviceToCalibrate) {

        // Start a timer
        executor.submit(countdownTimer);
        try {

            // send calibration command to remote device
            model.sendCalibrationCommand(deviceToCalibrate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

        System.out.println("heard property change.");
        // Property change listener setup

        String property = propertyChangeEvent.getPropertyName();
        Object value = propertyChangeEvent.getNewValue();

        if(property.equals("startScanning")) startNetworkDiscoveryScan();

        if(property.equals("stopScanning")) stopNetworkDiscoveryScan();

        if(property.equals("scanComplete")) try {
            cleanUpAfterNetworkScan();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(property.equals("saveDeviceSettings")) sendSettingsToDevice((RemoteDevice) value);

        if(property.equals("calibrate")) {
            calibrate((DeviceToCalibrate)value);
        }

    }

    /**
     * This class is created to extend javafx.concurrent.Service to provide a
     * thread for the model to run on. This CANNOT fully stop the model! It will
     * only pause it! Stop the model using StopService.
     */

    private class ProcessingService extends Service<String> {
        @Override
        protected void succeeded() {
            messageWrapper.setRunStatus("Running.");
            messageWrapper.setRunningProperty(true);
        }

        @Override
        protected void failed() {
            messageWrapper.setRunStatus("Could not start.");
            messageWrapper.setRunningProperty(false);
        }

        @Override
        protected void cancelled() {
            messageWrapper.setRunStatus("Cancelled");
            messageWrapper.setRunningProperty(false);
        }

        @Override
        protected Task<String> createTask() {
            return new Task<String>() {
                @Override
                protected String call() throws Exception {
                    if(!model.running) {
                        //System.out.println("Model is running, about to call model.start"); //TODO RM
                        model.start();
                    } else {
                        model.resume();
                        Thread.sleep(1000);
                    }
                    //return model.ipStatus;
                    return null;
                }
            };
        }

    }

    /**
     * This class is used to stop the model completely when necessary.
     * Note that when it "succeeds" the model is cancelled, and when
     * it is "cancelled" the model is set back to run.
     */

    public class StopService extends Service<String> {
        @Override
        protected void succeeded() {
            messageWrapper.setRunStatus("Stopped.");
            messageWrapper.setRunningProperty(false);
        }

        @Override
        protected void cancelled() {
            messageWrapper.setRunStatus("Stopping cancelled.");
            messageWrapper.setRunningProperty(true);
        }

        @Override
        protected Task<String> createTask() {
            return new Task<String>() {
                @Override
                protected String call() throws Exception {
                    //updateMessage("Stopping...");
                    model.stop();
                    Thread.sleep(1000);
                    //updateMessage("Stopped.");
                    //return model.ipStatus;
                    return null;
                }
            };
        }
    }

    /**
     * This class keeps track of the state of the task for either the ProcessingService
     * or the StopService (will have these two instances). Along with other info.
     */

    private class MessageWrapper {
        StringProperty runStatus = new SimpleStringProperty();
        StringProperty ipStatus = new SimpleStringProperty();
        BooleanProperty runningProperty = new SimpleBooleanProperty(Boolean.FALSE);

        public MessageWrapper(String runStatusString, String ipStatusString, Boolean running) {
            this.runStatus.setValue(runStatusString);
            this.ipStatus.setValue(ipStatusString);
            this.runningProperty.setValue(running);
        }

        public String getRunStatus() {
            return runStatus.get();
        }

        public StringProperty runStatusProperty() {
            return runStatus;
        }

        public void setRunStatus(String runStatus) {
            this.runStatus.set(runStatus);
        }

        public String getIpStatus() {
            return ipStatus.get();
        }

        public StringProperty ipStatusProperty() {
            return ipStatus;
        }

        public void setIpStatus(String ipStatus) {
            this.ipStatus.set(ipStatus);
        }

        public boolean isRunningProperty() {
            return runningProperty.get();
        }

        public BooleanProperty runningProperty() {
            return runningProperty;
        }

        public void setRunningProperty(boolean runningProperty) {
            this.runningProperty.set(runningProperty);
        }
    }


}
