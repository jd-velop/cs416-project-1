package src;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

public class VirtualSwitch {
    List<String> Ports;
    Map<String, String> switchTable = new HashMap<String, String>();

    //Frame arrives at switch and then:
    // 1. The switch uses destination MAC addr in frame header to search table
    //2. if found, forward frame out the associated port 
    //3. If not found, flood frame out all ports except the one it arrived on

    public void receiveFrame(String Port) throws IOException{
        FrameParser fp = new FrameParser();
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(Port));
        byte[] buffer = new byte[1500];
       
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String Frame = new String(buffer).trim();
    

        List<String> frameParts = fp.parseFrame(Frame);
        String sourceMAC = frameParts.get(0);
        String destMAC = frameParts.get(1);
        String msg = frameParts.get(2);
        
        

        boolean sourceInTable = switchTable.containsKey(sourceMAC);
        if (!sourceInTable){
            //sourceMAC not found, we now learn the correct port
            switchTable.put(sourceMAC, Port);
            System.out.println(switchTable.toString());
        }
        
        boolean destInTable = switchTable.containsKey(destMAC);
        if (!destInTable){
           flood(Frame, Port);
        } else {
            String outPort = switchTable.get(destMAC); 
            sendFrame(Frame, outPort);
        }
        socket.close();
    }

    public void sendFrame(String Frame, String outPort) throws IOException{

        String[] parts = outPort.split(":");
        String ipString = parts[0];
        int portNumber = Integer.parseInt(parts[1]);


        byte[] buffer = Frame.getBytes();
        DatagramSocket sock = new DatagramSocket(portNumber);
        InetAddress ip = InetAddress.getByName(ipString);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, ip, portNumber);    
        sock.send(packet);
        sock.close();

    }

    public void flood(String Frame, String ignorePort){
        List<String> outgoingPorts = this.Ports;
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

