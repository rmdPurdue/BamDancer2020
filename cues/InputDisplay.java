package cues;

import com.OutgoingData;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * The <code>InputDisplay</code> class sets up a Vbox which can be displayed to
 * show the current relevant information pertaining to a cue.
 *
 * This class is used when a new cue is created, and InputDisplays created will be
 * arranged in a FlowPane when their cue is running.
 *
 * @author Hannah Eckert (eckerth@purdue.edu)
 *
 */

public class InputDisplay {
    private VBox display;

    /* Immutable Labels */

    private Label device;
    private Label input;
    private Label sensorVal;
    private Label destination;
    private Label oscAddress;
    private Label algorithm;
    private Label outputValue;

    /* Dynamic Labels */

    private Label deviceName1;   //Static-->From OutputMapping
    private Label deviceInput1;  //Dynamic-->From OutgoingData
    private Label sensorValue1;  //Dynamic-->From OutgoingData
    private Label destination1;  //Static-->From OutputMapping
    private Label oscAddress1;   //Static-->From OutputMapping
    private Label algorithm1;    //Static-->From OutputMapping
    private Label outputValue1;  //Dynamic-->From OutgoingData

    /* GridPane for labels */

    private GridPane gpane;

    public InputDisplay() {}

    /**
     * Constructor for <code>/InputDisplay</code> initializes all fields which do not
     * change as a cue is run, and creates spaces for the dynamic fields to be filled
     * in later.
     *
     * @param outputMapping
     */
    public InputDisplay(OutputMapping outputMapping) {

        /* Initialise static field labels */

        this.device = new Label("Device:");
        device.setStyle("-fx-font-weight: bold");
        this.input = new Label("Input:");
        input.setStyle("-fx-font-weight: bold");
        this.sensorVal = new Label("Sensor Value:");
        sensorVal.setStyle("-fx-font-weight: bold");
        this.destination = new Label("Destination:");
        destination.setStyle("-fx-font-weight: bold");
        this.oscAddress = new Label("OSC Address:");
        oscAddress.setStyle("-fx-font-weight: bold");
        this.algorithm = new Label("Algorithm:");
        algorithm.setStyle("-fx-font-weight: bold");
        this.outputValue = new Label("Output Value:");
        outputValue.setStyle("-fx-font-weight: bold");

        /* Set up fields which will be initialised with values later */

        this.deviceName1 = new Label();
        this.deviceInput1 = new Label();
        this.sensorValue1 = new Label();
        this.destination1 = new Label();
        this.oscAddress1 = new Label();
        this.algorithm1 = new Label();
        this.outputValue1 = new Label();

        /* Set text for fields which are static throughout playback of a cue*/

        setAllStatic(outputMapping);

        /* Set up gridPane with static labels */

        gpane = new GridPane();
        gpane.getColumnConstraints().add(new ColumnConstraints(110)); //TODO check
        gpane.add(device, 0, 0);
        gpane.add(input, 0, 1);
        gpane.add(sensorVal, 0, 2);
        gpane.add(destination, 0, 3);
        gpane.add(oscAddress, 0, 4);
        gpane.add(algorithm, 0, 5);
        gpane.add(outputValue, 0, 6);

        /* Set up gridPane with all dynamic labels */

        gpane.add(deviceName1, 1, 0);
        gpane.add(deviceInput1, 1, 1);
        gpane.add(sensorValue1, 1, 2);
        gpane.add(destination1, 1, 3);
        gpane.add(oscAddress1, 1, 4);
        gpane.add(algorithm1, 1, 5);
        gpane.add(outputValue1, 1, 6);

        /* TODO Set up Vbox containing gridPane */

        display = new VBox(gpane);
        display.setPrefWidth(250);
        display.setPrefHeight(168);
    }

    /* Note that all of the following require that the field passed has already been toString()ed if necessary */

    private void setDeviceName(String name) { this.deviceName1.setText(name);}
    private void setDeviceInput(String input) {
        this.deviceInput1.setText(input);
    }
    private void setSensorValue(String sensorValue) {
        this.sensorValue1.setText(sensorValue);
    }
    private void setDestination(String dest) {
        this.destination1.setText(dest);
    }
    private void setOSCAddress(String oscAddress) {
        this.oscAddress1.setText(oscAddress);
    }
    private void setAlgorithm(String algorithm) {
        this.algorithm1.setText(algorithm);
    }
    private void setOutputValue(String outputValue) {
        this.outputValue1.setText(outputValue);
    }

    /* Used to make the display (VBox) a child of the FlowPane */

    public VBox getDisplay() {return display;}

    /**
     * This function sets all fields which change when a new OutgoingData is created for a cue
     */
    public void setAllDynamic(OutgoingData outgoingData) {
        setSensorValue(Integer.toString(outgoingData.getRawData()));
        setOutputValue(Float.toString(outgoingData.getData()));
    }

    /**
     * This function sets all fields which do not change as each new OutgoingData is created
     */
    private void setAllStatic(OutputMapping outputMapping) {
        setDeviceName(outputMapping.getDeviceName());
        setDeviceInput(Integer.toString(outputMapping.getInput()));
        setOSCAddress(outputMapping.getOutputAddress().getUrl());
        setAlgorithm(outputMapping.getOutputAddress().getAlgorithm().getValue());
        setDestination(outputMapping.getOutputAddress().getRemoteDevice().getDeviceName());
    }
}