import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;

public class Router {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("id improperly specified");
            return;
        }
        String routerID = args[0];
        try {   // check that such a switch id exists in config file
            Parser.parse("Config.txt");
            Device myDevice = Parser.devices.get(routerID);
            if (myDevice == null) {
                System.out.println("Device ID " + routerID + "not found in config file");
                return;
            }

            // learn neighbors (IP, port) using parser
            List<String> neighborPorts = new LinkedList<>();
            List<String> neighborIDs = Parser.links.get(routerID);

            Router r = new Router(neighborPorts);
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

    public Router(List<String> Ports){
        this.Ports = Ports;
    }

        // i.   Virtual source MAC address
        // ii.  Virtual destination MAC address
        // iii. Virtual source IP address
        // iv.  Virtual destination IP address
        // v.   Short message
    public void receiveFrame(DatagramSocket socket) throws IOException {
        // when receiving a frame, extract the five elements of the virtual frame and print them out
        FrameParser fp = new FrameParser();
        byte[] buffer = new byte[1500];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        


    }
        // when receiving a packet, perform a lookup in the forwarding table and determine the outgoing port

        // router table will be hard coded
        // router table contains one entry for each subnet. 
        // Virtual exit port is specified for a directly connected subnet. 
        // For a non-directly connected subnet, the next-hop router's virtual IP is specified.
        // For example, R1's table: 
        // Subnet prefix:           net1,       net2,       net3
        // Next-hop or exit port:   left port,  right port, net2.R2

        // re-write the source and destination MAC addresses of the virtual frames.
        // To determine the new destination MAC address, the router should extract the ID from the next-hop virtual IP listed in the forwarding table.
        // For example, extract 'R2' from 'net2.R2' and use R2 as the new destination MAC address before sending the virtual frame to R2.

        // NOTE: every time a router receives a virtual frame, it should print out all five elements of the frame; every time a router forwards a virtual frame out, it should print out all five elements of the frame as well/
}