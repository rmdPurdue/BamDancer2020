package com;

import devices.AnalogInput;
import devices.RemoteDevice;
import util.DeviceType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static util.PortNumbers.UDP_RECEIVE_PORT;

public class DiscoveryQueryListener implements Runnable {

    private ArrayList<RemoteDevice> remoteDevices = new ArrayList<>();
    private AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket = null;
    private int receivePort;
    private Boolean discoveryListeningComplete;
    private int timeoutInSeconds = 5; //Hard coded; same as for DeviceDiscoveryQuery

    public DiscoveryQueryListener() {
        // Set the UDP listening port
        receivePort = UDP_RECEIVE_PORT.getValue();
    }

    public void stopDiscoveryListening() {

        // Set our running property to false
        running.set(false);

        // Stop this thread
        discoveryListeningComplete = true;
        Thread.currentThread().interrupt();
    }

    public ArrayList<RemoteDevice> getDiscoveredDevices() {
        return remoteDevices;
    }

    public void run() {

        running.set(true);
        discoveryListeningComplete = false;
        long startTimeInMillis = System.currentTimeMillis();

        // Open a receiver socket
        try {
            socket = new DatagramSocket(receivePort);
            //FIrst time running this is successful
        } catch (SocketException e) {
            e.printStackTrace();
        }

        double timeElapsed = System.currentTimeMillis() - startTimeInMillis;
        while(running.get()) {

            try {

                // Create an empty byte array to put incoming messages into
                byte[] buf = new byte[256];

                // Create a new UDP datagram packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                if((timeElapsed/1000) >= timeoutInSeconds) {
                    System.out.println("Discovery Listening timeout.");
                    stopDiscoveryListening();
                    socket.close();
                    return;
                }

                System.out.println("Message received.");

                // Get the IP address our received packet was sent from
                InetAddress address = packet.getAddress();
                System.out.println("IP Address of remote sender: " + address.toString());

                // Get the port our received packet was addressed to
                int port = packet.getPort();
                System.out.println("Port of remote sender: " + port);

                // Get the bytes that comprise the message in our received packet
                buf = packet.getData();

                // Convert the byte array to a String
                String data = new String(buf);

                // Create string variables to hold the macAddress of our remote device and its string name
                String macAddress;
                String deviceName;

                // If our data contains "::", it's likely a "real" message
                // Responses from remote devices should be a string name followed by "::" and then a mac address.
                System.out.printf("Dat: %s", data); //TODO RM
                if(data.contains("::")) {
                    // Split the data on "::"
                    String parts[] = data.split("::");

                    // Set the device name to the first half of the data
                    deviceName = parts[0];

                    // Set the MAC address to the second half of the data
                    macAddress = parts[1];

                    // Eliminate any extraneous bytes from the MAC Address
                    macAddress = macAddress.substring(0,12);
                } else {
                    // If the data isn't formatted as expected, throw an error
                    throw new IllegalArgumentException("String " + data + " does not contain '::'.");
                }

                RemoteDevice temp = new RemoteDevice();
                temp.setMacAddress(macAddress);
                temp.setDeviceName(deviceName);
                temp.setIpAddress(address);
                temp.setDeviceType(DeviceType.SENDER); //TODO dunno if this is a bad idea....??
                temp.setAddressToSendTo(address); //TODO also not sure if this is bad....
                AnalogInput analogInput = new AnalogInput(); //TODO not sure if its okay to use default constructor
                temp.addAnalogInputs(6);  //TODO this appears to be a good choice
                if(remoteDevices.contains(temp)) {
                    System.out.println("We know this device already.");
                    System.out.println("-----------------------------");
                    System.out.println();
                } else {
                    System.out.println("We've not seen this device.");

                    // Add this new device to our list
                    remoteDevices.add(temp); //TODO also need to add to sender/receiver list
                    System.out.println("Device added. Device list size: " + remoteDevices.size());
                    System.out.println("This device MAC address: " + remoteDevices.get(remoteDevices.size()-1).getMacAddress());

                    // Create an acknowledgement message
                    String message = "Found you!";

                    // Convert acknowledgement to a byte array
                    byte[] acknowledgeMessage = message.getBytes();

                    // Create a new UPD datagram with our acknowledgement message addressed to the device we just heard form
                    packet = new DatagramPacket(acknowledgeMessage, acknowledgeMessage.length, address, port);

                    // Send our acknowledgement message
                    socket.send(packet);

                    //System.out.println("-----------------------------");
                    //System.out.println();
                }

            } catch (IOException e) { //Not having an exception (tried blanket exception and got nothing )
                e.printStackTrace();
            }
        }

        // Close UDP socket
        socket.close();
    }
}
