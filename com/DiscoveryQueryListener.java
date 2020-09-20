package com;

import devices.AnalogInput;
import devices.RemoteDevice;
import util.DeviceType;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static util.PortNumbers.UDP_RECEIVE_PORT;

public class DiscoveryQueryListener implements Runnable {

    private ArrayList<RemoteDevice> remoteDevices = new ArrayList<>();
    private AtomicBoolean running = new AtomicBoolean(false);
    private DatagramSocket socket = null;
    private int receivePort;
    private Boolean discoveryListeningComplete;
    private int timeoutInMilliSeconds = 5000; //Hard coded; same as for DeviceDiscoveryQuery

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
        socket.disconnect();
        //TODO Hannah, need to determine if there are any sockets left open and close them before returning!
    }

    public ArrayList<RemoteDevice> getDiscoveredDevices() {
        return remoteDevices;
    }

    public void run() {

        running.set(true);
        discoveryListeningComplete = false;

        // Open a receiver socket
        try {
            socket = new DatagramSocket(receivePort);
            //FIrst time running this is successful
        } catch (SocketException e) {
            e.printStackTrace();
        }

        while(running.get()) {

            try {

                // Create an empty byte array to put incoming messages into
                byte[] buf = new byte[256];

                // Create a new UDP datagram packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.setSoTimeout(timeoutInMilliSeconds);  //Sets timer on this socket
                socket.receive(packet);

                System.out.println("Message received.");

                // Get the IP address our received packet was sent from
                InetAddress address = packet.getAddress();
                //System.out.println("IP Address of remote sender: " + address.toString());

                // Get the port our received packet was addressed to
                int port = packet.getPort();
                //System.out.println("Port of remote sender: " + port);

                // Get the bytes that comprise the message in our received packet
                buf = packet.getData();

                // Convert the byte array to a String
                String data = new String(buf);

                // Create string variables to hold the macAddress of our remote device and its string name
                String macAddress;
                String deviceName;

                // If our data contains "::", it's likely a "real" message
                // Responses from remote devices should be a string name followed by "::" and then a mac address.
                //System.out.printf("Dat: %s", data);
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
                temp.setDeviceType(DeviceType.SENDER);
                temp.setAddressToSendTo(address);
                AnalogInput analogInput = new AnalogInput();
                temp.addAnalogInputs(6);
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
                }

            } catch (SocketTimeoutException ste) {
                System.out.println("Discovery Listening timeout.");
                stopDiscoveryListening();
                socket.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Close UDP socket
        socket.close();
    }
}
