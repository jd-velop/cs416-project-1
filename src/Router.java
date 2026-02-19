import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;

public class Router {
    private List<String> Ports;
    private Map<String, String> forwardingTable = new HashMap<>();
    private Map<String, String> neighborAddresses = new HashMap<>(); // deviceID -> ip:port
    private String routerID;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("id improperly specified");
            return;
        }
        String routerID = args[0];

        try {
            Parser.parse("Config.txt");
            Device myDevice = Parser.devices.get(routerID);
            if (myDevice == null) {
                System.out.println("Device ID " + routerID + " not found in config file");
                return;
            }

            // learn neighbors (IP, port) using parser
            List<String> neighborPorts = new LinkedList<>();
            List<String> neighborIDs = Parser.links.get(routerID);
            Map<String, String> neighborAddresses = new HashMap<>();
            if (neighborIDs != null) {
                for (String neighborID : neighborIDs) {
                    Device neighbor = Parser.devices.get(neighborID);
                    if (neighbor != null) {
                        String addr = neighbor.ip + ":" + neighbor.port;
                        neighborPorts.add(addr);
                        neighborAddresses.put(neighborID, addr);
                    }
                }
            }

            // Hard-coded forwarding table per router
            // Format: subnet -> neighbor device ID (directly connected) or "subnet.RouterID" (next-hop)
            Map<String, String> forwardingTable = new HashMap<>();
            if (routerID.equals("R1")) {
                forwardingTable.put("net1", "S1");       // directly connected via S1
                forwardingTable.put("net2", "R2");       // directly connected via R2
                forwardingTable.put("net3", "net2.R2");  // next-hop is R2
            } else if (routerID.equals("R2")) {
                forwardingTable.put("net2", "R1");       // directly connected via R1
                forwardingTable.put("net3", "S2");       // directly connected via S2
                forwardingTable.put("net1", "net2.R1");  // next-hop is R1
            }

            System.out.println("Forwarding table: " + forwardingTable);

            Router r = new Router(routerID, neighborPorts, forwardingTable, neighborAddresses);
            System.out.println("Router " + routerID + " running on port " + myDevice.port);

            DatagramSocket socket = new DatagramSocket(myDevice.port);
            while (true) {
                try {
                    r.receiveFrame(socket);
                } catch (Exception e) {
                    System.err.println("Error receiving frame: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Router(String routerID, List<String> Ports, Map<String, String> forwardingTable, Map<String, String> neighborAddresses) {
        this.routerID = routerID;
        this.Ports = Ports;
        this.forwardingTable = forwardingTable;
        this.neighborAddresses = neighborAddresses;
    }

    public void receiveFrame(DatagramSocket socket) throws IOException {
        FrameParser fp = new FrameParser();
        byte[] buffer = new byte[1500];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String Frame = new String(buffer).trim();

        // parse the frame
        List<String> frameParts = fp.parseFrame(Frame);

        String sourceMAC = frameParts.get(0);
        String destMAC = frameParts.get(1);
        String sourceIP = frameParts.get(2);
        String destinationIP = frameParts.get(3);
        String msg = frameParts.get(4);

        // print incoming frame
        System.out.println("Received frame:");
        System.out.println("  Source MAC: " + sourceMAC);
        System.out.println("  Dest MAC: " + destMAC);
        System.out.println("  Source IP: " + sourceIP);
        System.out.println("  Dest IP: " + destinationIP);
        System.out.println("  Message: " + msg);

        // extract subnet prefixes
        String srcSubnet = sourceIP.split("\\.")[0];
        String dstSubnet = destinationIP.split("\\.")[0];

        // if source and destination are on the same subnet, drop the frame
        if (srcSubnet.equals(dstSubnet)) {
            System.out.println("Same subnet (" + srcSubnet + "), dropping frame.");
            return;
        }

        // look up destination subnet in forwarding table
        String tableEntry = forwardingTable.get(dstSubnet);
        if (tableEntry == null) {
            System.out.println("No forwarding table entry for subnet " + dstSubnet + ", dropping frame.");
            return;
        }

        // rewrite MAC addresses
        String newSourceMAC = routerID;
        String newDestMAC;
        String exitDeviceID;

        if (tableEntry.contains(".")) {
            // next-hop entry (e.g. "net2.R2") – extract router ID after the dot
            exitDeviceID = tableEntry.split("\\.")[1];
            newDestMAC = exitDeviceID;
        } else {
            // directly connected subnet – exit device is a switch/neighbor
            exitDeviceID = tableEntry;
            // destination MAC is the final host ID (e.g. "A" from "net1.A")
            newDestMAC = destinationIP.split("\\.")[1];
        }

        // rebuild the frame with rewritten MACs
        String newFrame = newSourceMAC + ":" + newDestMAC + ":" + sourceIP + ":" + destinationIP + ":" + msg;

        // resolve exit device to real ip:port
        String outAddress = neighborAddresses.get(exitDeviceID);
        if (outAddress == null) {
            System.out.println("Cannot resolve address for device " + exitDeviceID + ", dropping frame.");
            return;
        }

        // print outgoing frame
        System.out.println("Forwarding frame:");
        System.out.println("  Source MAC: " + newSourceMAC);
        System.out.println("  Dest MAC: " + newDestMAC);
        System.out.println("  Source IP: " + sourceIP);
        System.out.println("  Dest IP: " + destinationIP);
        System.out.println("  Message: " + msg);
        System.out.println("  Out to: " + outAddress);

        sendFrame(socket, newFrame, outAddress);
    }

    public void sendFrame(DatagramSocket socket, String Frame, String outPort) throws IOException {
        String[] parts = outPort.split(":");
        String ipString = parts[0];
        int portNumber = Integer.parseInt(parts[1]);

        byte[] buffer = Frame.getBytes();
        InetAddress ip = InetAddress.getByName(ipString);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, portNumber);
        socket.send(packet);
    }
}