package mvc;

import com.OutgoingData;
import com.incListener;
import com.oracle.webservices.internal.api.message.PropertySet;
import com.outSender;
import cues.Cue;
import cues.OutputMapping;
import osc.OSCMessage;
import com.OSC_Sender;
import devices.DeviceList;
import devices.DeviceToCalibrate;
import devices.RemoteDevice;
import util.DeviceType;
import util.ThreadListener;
import util.PropertyChanges;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static util.OSCCommand.GET_SETTINGS;
import static util.OSCAddress.*;
import static util.PortNumbers.OSC_SEND_PORT;
import static util.algorithms.Algorithm.PERCENT_LEVELS;

public class Model implements ThreadListener {

    private DeviceList senderDevices = new DeviceList();
    private DeviceList receiverDevices = new DeviceList();
    private ArrayList<Cue> cueList = new ArrayList<>();
    private PropertyChangeSupport modelPropertyChangeSupport = new PropertyChangeSupport(this);

    private BlockingQueue<OSCMessage> incomingQueue = new ArrayBlockingQueue<>(1);
    private BlockingQueue<OutgoingData> outgoingQueue = new ArrayBlockingQueue<>(1);
    private incListener incoming = new incListener(incomingQueue);
    private outSender outgoing = new outSender(outgoingQueue);
    public boolean running = false;
    private boolean paused = false;

    private Cue cue = new Cue();
    private int data = 0;

    public Model() throws UnknownHostException {
        boolean inputCalibrated = false;
        boolean remoteDeviceSaved = false;


        // Add blank cues
        cueList.add(new Cue(1.0, "Test"));
        cueList.add(new Cue(2.0, "Test2"));

        // Add test devices
        senderDevices.getDevices().add(new RemoteDevice(
                InetAddress.getByName("192.168.2.2"),
                "FFFFFFFFFFFF",
                "Test")
        );
        senderDevices.getDeviceUsingMac("FFFFFFFFFFFF").setDeviceType(DeviceType.SENDER);
        senderDevices.getDeviceUsingMac("FFFFFFFFFFFF").setAddressToSendTo(InetAddress.getByName("192.168.2.10"));
        senderDevices.getDeviceUsingMac("FFFFFFFFFFFF").setPortToSendTo(8001);
        senderDevices.getDeviceUsingMac("FFFFFFFFFFFF").addAnalogInputs(6);

        receiverDevices.getDevices().add(new RemoteDevice(
                InetAddress.getByName("192.168.2.10"),
                "FFFFFFFFFFF1",
                "Ion")
        );
        receiverDevices.getDeviceUsingMac("FFFFFFFFFFF1").setDeviceType(DeviceType.RECEIVER);
        receiverDevices.getDeviceUsingMac("FFFFFFFFFFF1").setReceivePort(8001);
    }

    public void start() throws InterruptedException, UnknownHostException, SocketException {

        // Set main thread status properties
        running = true;
        paused = false;

        // Create threads to listen for incoming messages and to handle outgoing messages
        Thread incomingReader = new Thread(incoming);
        Thread outgoingSender = new Thread(outgoing);

        // Set message threads as daemons for easy cleanup
        incomingReader.setDaemon(true);
        outgoingSender.setDaemon(true);

        // Start message threads
        incomingReader.start();
        outgoingSender.start();

        /*
            Code for determining local IP. Not sure if we need it.
         */

        /*
        ipStatus = "Checking local connection...";

        Enumeration<NetworkInterface> iterNetwork;
        Enumeration<InetAddress> iterAddress;
        NetworkInterface network;
        InetAddress address;

        iterNetwork = NetworkInterface.getNetworkInterfaces();

        while(iterNetwork.hasMoreElements()) {
            network = iterNetwork.nextElement();
            if(!network.isUp()) {
                continue;
            }
            if(network.isLoopback()) {
                continue;
            }

            iterAddress = network.getInetAddresses();

            while(iterAddress.hasMoreElements()) {
                address = iterAddress.nextElement();

                if (address.isSiteLocalAddress()) {
                    ipStatus = "Local address: " + address.getHostAddress();
                }
            }
        }
        */

        // Wait a beat, then add Model as a listener for the incoming message thread
        Thread.sleep(500);
        incoming.addListener(this);
        stop(); //Stopping incListener class so it doesn't waste resources.
    }

    /**
     * This method pauses the incoming message thread. Used by main to trigger model
     * stop when Stop button is pressed. Also used for resetting levels in Model.
     */

    public void stop() {
        //Stop the incoming message thread. Update status variables.
        if(running) {
            incoming.stop();
        }
        paused = true;
    }

    public void resume() {
        //Restart incoming message thread. Update status variables.
        paused = false;
        incoming.resume();
    }

    @Override
    public void incomingDataUpdated() { //TODO this is what gets called whenever we have a new OSC message

        System.out.println("In IncomingDataUpdated (Model).");
        // Create a new OSC Message

        OSCMessage message = new OSCMessage();

        // If there's data on the incoming message queue, grab it.
        if (incomingQueue.peek() != null) {
            try {
                message = incomingQueue.take();
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(message.getAddress() + " " + message.getArguments());

            // If we are running and not paused:
            if (running && !paused) {
                OSCMessage finalMessage = message;

                // get the mappings for the current cue.
                for (OutputMapping mapping : this.cue.getOutputMappings()) {

                    // For each mapping, get the data argument based on the input number
                    // Create an instance of OutgoingData based on the mapping information
                    // for destination IP, destination port, OSC address, algorithm, and data.
                    // Then, add the outgoing data to the outgoing message queue.
                    //TODO for some reason the message gets /Testing instead of /Testingdevice so it gets it incorrect
                    if (finalMessage.getAddress().equals("/hub/" + mapping.getDeviceName())) {  //TODO if this osc message is intended for us to use (check if it is the correct device
                        this.data = (Integer) finalMessage.getArguments().get(mapping.getInput()); //TODO from the current OutputMapping, get the input number and retrieve the data corresponding to it (/hub/device/ # # # # # #) get the # correspondingto input number
                        OutgoingData out = new OutgoingData(mapping.getOutputAddress().getIPaddress(),
                                mapping.getOutputAddress().getPortNumber(),
                                mapping.getOutputAddress().getUrl(),
                                mapping.getOutputAddress().getAlgorithm(),
                                data);
                        mapping.updateDisplay(out);  //TODO for some strange reason, this is not occurring....
                        System.out.println("Updating mapping!!!");  //TODO RM
                        try {
                            this.outgoingQueue.put(out);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    void addPropertyChangeListener(PropertyChangeListener listener) {
        modelPropertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        modelPropertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void setSenderDevices(ArrayList<RemoteDevice> devices) throws IOException {
        senderDevices.setDevices(devices);
        updateSenderDeviceData();
    }

    public void deviceScanFailed() {
        /* Device scan has not found any devices from DeviceDiscoveryQuery*/
        modelPropertyChangeSupport.firePropertyChange( PropertyChanges.SCAN_FAILED.toString(), 0, 1);
    }

    public void deviceScanCompleted() {
        /* Network scan is finished and irrelevant of whether it was successful or not, we need to re
        * activate the scan button.*/
        modelPropertyChangeSupport.firePropertyChange(PropertyChanges.SCAN_FINISHED.toString(), 0, 1);
    }

    DeviceList getSenderDevices() { return senderDevices; }

    DeviceList getReceiverDevices() { return receiverDevices; }

    ArrayList<Cue> getCueList() {
        return cueList;
    }

    boolean addCue(Cue newCue) {
        if (!cueList.contains(newCue)) { //TODO check whether this is actually necessary or not
            cueList.add(newCue);

            //Fire property change so CueListController and Playback are both aware of shared changes
            modelPropertyChangeSupport.firePropertyChange(PropertyChanges.UPDATED_CUE_LIST.toString(), 0, 1);
            return true;
        }
        return false;
    }

    boolean deleteCue(Cue toDelete) {
        Boolean removeSuccessful = cueList.remove(toDelete);
        if (removeSuccessful) {

            //Fire property change so CueListController and Playback are both aware of shared changes
            modelPropertyChangeSupport.firePropertyChange(PropertyChanges.UPDATED_CUE_LIST.toString(), 0, 1);
            return true;
        }
        return false;
    }

    boolean updateCueNumber(Double oldNumber, Double newNumber) {
        //Updates a cue which is already present in the cuelist

        for (Cue cue : cueList) {
            if (cue.getCueNumber().equals(oldNumber)) {
                cue.setCueNumber(newNumber);
                return true;
            }
        }
        return false;
    }

    boolean updateCueDescription(Double cueNumber, String newDescription) {
        //Updates a cue which is already present in the cuelist

        for (Cue cue : cueList) {
            if (cue.getCueNumber().equals(cueNumber)) {
                cue.setCueDescription(newDescription);
                return true;
            }
        }
        return false;
    }

    boolean cueExists(Double cueNumber) {
        for (Cue cue : cueList) {
            if (cue.getCueNumber().equals(cueNumber)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fires property change to set up view for a cue to run in playback, and then
     * creates a thread (listener) for the cue.
     */

    public void goCue(Cue cue) {
        /*
            Fire property change so that the FlowPane is reset from running the previous cue.
         */

        modelPropertyChangeSupport.firePropertyChange(PropertyChanges.CLEAR_PANE.toString(), 0, 1);

        /*
            Fire property changes for each output mapping so their respective InputDisplays
            can get added to the FlowPane.
         */

        for (OutputMapping mapping : cue.getOutputMappings()) {
            System.out.println("Sending property change for updating mapping"); //TODO RM
            modelPropertyChangeSupport.firePropertyChange(PropertyChanges.UPDATE_VIEW.toString(), 0, mapping.getInputDisplay());
        }
        outgoingQueue.clear();
        resume();
        this.cue = cue;
    }

    /*
        Stops and clears info from current incoming queue to set up for a new one.
        Then, appears to put a bunch of mapping info into the outgoingQueue?
     */
    public boolean resetLevels() {
        System.out.println("Resetting Levels.");
        if(this.cue!=null) {
            System.out.println("Cue not null.");
            if(incoming.size() > 0 ) stop();
            incomingQueue.clear();
            System.out.println("Incoming queue cleared.");
            outgoingQueue.clear();
            System.out.println("Outgoing queue cleared.");
            if(cue.getOutputMappings().size() > 0) {
                System.out.println("Resetting mapping levels.");
                this.cue.getOutputMappings().forEach(mapping -> {
                    System.out.println(mapping.getOutputAddress().getUrl());
                    OutgoingData out = new OutgoingData(
                            mapping.getOutputAddress().getIPaddress(),
                            mapping.getOutputAddress().getPortNumber(),
                            mapping.getOutputAddress().getUrl(),
                            PERCENT_LEVELS,
                            0
                    );
                    try {
                        outgoingQueue.put(out);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        return true;
    }

    private void updateSenderDeviceData() throws IOException {
        for(RemoteDevice device : senderDevices.getDevices()) {
            List<Object> args = new ArrayList<>();
            args.add(GET_SETTINGS.getValue());
            OSC_Sender.sendMessage(device.getIpAddress(), OSC_SEND_PORT.getValue(), SETUP.toString(), args);
        }
    }



    public void parseIncomingOSCMessage(OSCMessage message) throws UnknownHostException {
        if(message.matches("/device_setup")) {
            System.out.println("Got message addressed to: /device_setup");
            String senderMACAddress = (String) message.getArguments().get(0);

            RemoteDevice temp = new RemoteDevice();

            temp.setMacAddress(senderMACAddress);

            temp.setDeviceName((String)message.getArguments().get(1));

            String address = message.getArguments().get(2) + "." +
                    message.getArguments().get(3) + "." +
                    message.getArguments().get(4) + "." +
                    message.getArguments().get(5);

            temp.setAddressToSendTo(InetAddress.getByName(address));

            temp.setPortToSendTo((int) message.getArguments().get(6));

            switch ((int)message.getArguments().get(7)) {
                case 1:
                    temp.setDeviceType(DeviceType.SENDER);
                    System.out.println("Device is a SENDER.");
                    break;
                case 2:
                    temp.setDeviceType(DeviceType.RECEIVER);
                    System.out.println("Device is a RECEIVER.");
                    break;
                case 3:
                    temp.setDeviceType(DeviceType.MIXED);
                    System.out.println("Device type is MIXED.");
                    break;
                default:
                    temp.setDeviceType(DeviceType.SENDER);
                    System.out.println("Device defaults to SENDER.");
                    break;
            }

            temp.addAnalogInputs((int) message.getArguments().get(8));

            for(int i = 0; i < temp.getNumberOfInputs(); i++) {

                temp.getAnalogInputs().get(i).setMinValue((int) message.getArguments().get((i*3) + 9));
                temp.getAnalogInputs().get(i).setMaxValue((int) message.getArguments().get((i*3) + 10));
                temp.getAnalogInputs().get(i).setFilterWeight((int) message.getArguments().get((i*3) + 11));
            }

            if (senderDevices.getDevices().contains(temp)) {
                int index = senderDevices.getDevices().indexOf(temp);
                temp.setIpAddress(senderDevices.getDevices().get(index).getIpAddress());
                senderDevices.getDevices().set(index, temp);
                System.out.println("Updated device (" +  senderDevices.getDevices().get(index).getMacAddress() + ") with new data.");
                System.out.println(senderDevices.getDevices().get(index).getNumberOfInputs() + " inputs.");
            }
            modelPropertyChangeSupport.firePropertyChange(PropertyChanges.UPDATED_DEV_DATA.toString(), false, true);
        }

        if(message.matches("/calibrate")) {
            String senderMACAddress = (String)message.getArguments().get(0);
            System.out.println("Got calibration message from: " + senderMACAddress);
            System.out.println("Input number: " + message.getArguments().get(1));
            System.out.println("Calibration Type: " + message.getArguments().get(2));
            System.out.println("Calibrated value: " + message.getArguments().get(3));

            RemoteDevice temp = new RemoteDevice();

            temp.setMacAddress(senderMACAddress);

            if(senderDevices.getDevices().contains(temp)) {
                int index = senderDevices.getDevices().indexOf(temp);
                switch((int) message.getArguments().get(2)) {
                    case 0:
                        System.out.println("Updating minimum value.");
                        senderDevices.getDevices().get(index).getAnalogInput((Integer) message.getArguments().get(1)).setMinValue((int)message.getArguments().get(3));
                        break;
                    case 1:
                        System.out.println("Updating maximum value.");
                        senderDevices.getDevices().get(index).getAnalogInput((Integer) message.getArguments().get(1)).setMaxValue((int)message.getArguments().get(3));
                        break;
                }
                modelPropertyChangeSupport.firePropertyChange(PropertyChanges.UPDATED_INPUT_DATA.toString(), false, true);
            }
        }

        /*if(message.matches("/calibrate/high")) { //TODO I think this is dead code
            String senderMACAddress = (String)message.getArguments().get(0);
            for (RemoteDevice dev : senderDevices.getDevices()) {
                if (dev.getMacAddress().equals(senderMACAddress)) {
                    dev.getAnalogInput((Integer) message.getArguments().get(1)).setMaxValue((Integer) message.getArguments().get(2));
                    modelPropertyChangeSupport.firePropertyChange(PropertyChanges.INPUT_CALIBRATED.toString(), false, true);
                }
            }
        }*/

        if(message.matches("/saved")) {
            System.out.println("Got a saved confirmation.");
            if((boolean)message.getArguments().get(0)) {
                modelPropertyChangeSupport.firePropertyChange(PropertyChanges.REMOTE_DEV_SAVED.toString(), false, true);
            }
        }
    }

    public void sendCalibrationCommand(DeviceToCalibrate deviceToCalibrate) throws IOException {
        System.out.println("Sending command.");
        List<Object>args = new ArrayList<>();
        args.add(deviceToCalibrate.getInputNumber());
        args.add(deviceToCalibrate.getSettingToCalibrate().getValue());
        OSC_Sender.sendMessage(deviceToCalibrate.getDevice().getIpAddress(), OSC_SEND_PORT.getValue(), CALIBRATE.toString(), args);
    }

    public void sendUpdateFirmwareCommand(RemoteDevice device) throws IOException {
        List<Object> args = new ArrayList<>();
        args.add(device.getDeviceName());
        args.add(device.getDeviceName().length());
        byte[] bytes = device.getAddressToSendTo().getAddress();
        for (byte aByte : bytes) {
            args.add((int) aByte);
        }
        args.add(device.getPortToSendTo());
        for(int i = 0; i < 6; i++) {
            args.add(device.getAnalogInputs().get(i).getMinValue());
            args.add(device.getAnalogInputs().get(i).getMaxValue());
            args.add(device.getAnalogInputs().get(i).getFilterWeight());
        }
        OSC_Sender.sendMessage(device.getIpAddress(),OSC_SEND_PORT.getValue(), SAVE_SETTINGS.toString(), args);
    }

}
