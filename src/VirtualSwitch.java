import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

public class VirtualSwitch {
    DatagramSocket socket = new DatagramSocket(0);
    List<String> Ports;
    Map<String, String> switchTable = new HashMap<String, String>();

    public VirtualSwitch(int Port) throws SocketException{
        this.socket = new DatagramSocket(Port);
    }

    //Frame arrives at switch and then:
    // 1. The switch uses destination MAC addr in frame header to search table
    //2. if found, forward frame out the associated port 
    //3. If not found, flood frame out all ports except the one it arrived on

    public void receiveFrame(String Frame, String Port){
        FrameParser fp = new FrameParser();
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
    }


    public void sendFrame(String Frame, String outPort){
    }

    public void flood(String Frame, String ignorePort){
        List<String> outgoingPorts = this.Ports;
        outgoingPorts.remove(ignorePort);
        for (String port: outgoingPorts){
            sendFrame(Frame, port);
        }
    }

}
