import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class VirtualSwitch {
    private List<String> Ports;
    Map<String, String> switchTable = new HashMap<>();
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("id improperly specified");
            return;
        }
        String switchID = args[0];
        try {   // check that such a switch id exists in config file
            Parser.parse("Config.txt");
            Device myDevice = Parser.devices.get(switchID);
            if (myDevice == null) {
                System.out.println("Device ID " + switchID + "not found in config file");
                return;
            }

            // learn neighbors (IP, port) using parser
            List<String> neighborIDs = Parser.links.get(switchID);
            System.out.println(neighborIDs);
            // does this go against the project spec? should the switch know its neighbor IDs off-rip?
            List<String> neighborPorts = new LinkedList<>();
            if (neighborIDs != null) {
                for (String neighborID : neighborIDs) {
                    Device neighbor = Parser.devices.get(neighborID);
                    if (neighbor != null) {
                        neighborPorts.add(neighbor.ip + ":" + neighbor.port);
                    }
                }
            }
            System.out.println("Neighbors: " + neighborPorts); // debug print of connected neighbors

            VirtualSwitch vs = new VirtualSwitch(neighborPorts);
            System.out.println("Switch " + switchID + " running on port " + myDevice.port);

            // Create socket once, outside the loop
            DatagramSocket socket = new DatagramSocket(myDevice.port);
            while (true) {
                try {
                    vs.receiveFrame(socket);
                } catch (Exception e) {
                    System.err.println("Error receiving frame: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public VirtualSwitch(List<String> Ports){
        this.Ports = Ports;
    }
    //Frame arrives at switch and then:
    //1. The switch uses destination MAC address in frame header to search table
    //2. If found, forward frame out the associated port
    //3. else, flood frame out all ports except the one it arrived on

    public void receiveFrame(DatagramSocket socket) throws IOException{
        FrameParser fp = new FrameParser();
        byte[] buffer = new byte[1500]; // max Ethernet frame size

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String Frame = new String(buffer).trim();

        // Get the sender's address from the packet
        String senderAddress = packet.getAddress().getHostAddress() + ":" + packet.getPort();

        // Packet will be <sourceMAC>:<destMAC>:<msg>
        List<String> frameParts = fp.parseFrame(Frame);
        String sourceMAC = frameParts.get(0);
        String destMAC = frameParts.get(1);
        System.out.println(frameParts.toString());

        boolean sourceInTable = switchTable.containsKey(sourceMAC);
        if (!sourceInTable){
            // sourceMAC not found, we now learn the correct port (sender's address)
            switchTable.put(sourceMAC, senderAddress);
            System.out.println("Learning new addr" + sourceMAC + switchTable.toString());
        }

        boolean destInTable = switchTable.containsKey(destMAC);
        if (!destInTable){
            flood(Frame, senderAddress);
            System.out.println("Switch is flooding!");
        } else {
            String outPort = switchTable.get(destMAC);
            System.out.println("sending to" + outPort);
            System.out.println(switchTable.toString());
            sendFrame(Frame, outPort);
        }
    }

    public void sendFrame(String Frame, String outPort) throws IOException{

        String[] parts = outPort.split(":");
        String ipString = parts[0];
        int portNumber = Integer.parseInt(parts[1]);


        byte[] buffer = Frame.getBytes();
        DatagramSocket sock = new DatagramSocket(); // Use any available port for sending
        InetAddress ip = InetAddress.getByName(ipString);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, portNumber);
        sock.send(packet);
        sock.close();

    }

    public void flood(String Frame, String ignorePort){
        List<String> outgoingPorts = new ArrayList<>(this.Ports);
        outgoingPorts.remove(ignorePort);
        for (String port: outgoingPorts){
            try{
                sendFrame(Frame, port);
            } catch (Exception e) {
                System.out.println("Error" + e);
            }
        }
    }
}