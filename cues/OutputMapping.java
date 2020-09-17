package cues;

import com.OutgoingData;
import devices.OutputAddress;
import devices.RemoteDevice;
import jdk.internal.util.xml.impl.Input;
import org.omg.CORBA.INITIALIZE;

import java.util.UUID;

public class OutputMapping {

    private RemoteDevice device;  //TODO probs should change this variable to remoteDevice for clarity; also redundant b/c remoteDevice is also in OUtputAddress
    private String deviceName;  //TODO note may not be in use anymore...
    private int input;
    private OutputAddress outputAddress;
    private UUID id; //TODO is number randomly assigned when we create a device (is unique) identifies a device
    private InputDisplay display; //TODO added by hannah

    OutputMapping() {
        id = UUID.randomUUID();
        display = new InputDisplay();
    }

    OutputMapping(OutputMapping source) {
        id = UUID.randomUUID();
        device = source.device;
        deviceName = source.deviceName;
        input = source.input;
        outputAddress = new OutputAddress(source.outputAddress);
        display = new InputDisplay(source);  //TODO added by hannah
    }

    public OutputMapping(RemoteDevice device, int inputName, OutputAddress outputAddress) {
        id = UUID.randomUUID();
        this.device = device;
        this.deviceName = device.getDeviceName();
        this.input = inputName;
        this.outputAddress = outputAddress;
        this.display = new InputDisplay(this);  //TODO added by hannah; check this!
    }

    public UUID getId() { return id; }

    public RemoteDevice getDevice() { return device; }

    public void setDevice(RemoteDevice device) { this.device = device; }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getInput() { return input; }

    public void setInput(int inputName) {
        this.input = input;
    }

    public OutputAddress getOutputAddress() {
        return outputAddress;
    }

    public void setOutputAddress(OutputAddress outputAddress) {
        this.outputAddress = outputAddress;
    }

    /* Sets all dynamic fields in the display (all others initialized when constructor is called) */

    public void updateDisplay(OutgoingData outgoingData) { display.setAllDynamic(outgoingData); }

    /* Getter to permit access to the InputDisplay for updating the view */

    public InputDisplay getInputDisplay() {return display;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OutputMapping that = (OutputMapping) o;

        return id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }

}
