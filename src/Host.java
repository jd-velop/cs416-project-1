import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("id improperly specified");
            return;
        }
        String hostID = args[0];
        try {
            //TODO: Change the file directory between "Config.txt" and "src/Config.txt" if error
            Parser.parse("Config.txt");
            Device myDevice = Parser.devices.get(hostID);
            if (myDevice == null) {
                System.out.println("Device ID " + hostID + " not found in config file");
                return;
            }
            List<String> neighborID = Parser.links.get(hostID);
            if (neighborID.isEmpty()) {
                System.out.println("No neighbor found for host " + hostID);
                return;
            }
            Device neighbor = Parser.devices.get(neighborID.get(0)); // assuming only one neighbor for host

            // create thread pool of 2 threads
            ExecutorService es = Executors.newFixedThreadPool(2);

            //Creating socket
            DatagramSocket hostSocket = new DatagramSocket(myDevice.port);

            // Start send and receive threads
            es.execute(new SendPacket(myDevice.id, neighbor.ip, neighbor.port, es, hostSocket));
            es.execute(new ReceivePacket(myDevice.port, es, hostSocket));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static class SendPacket implements Runnable {

        private String id;
        private String ip;
        private int port;
        private ExecutorService es;
        private DatagramSocket hostSocket;

        public SendPacket(String id, String ip, int port, ExecutorService es, DatagramSocket hostSocket) {
            this.id = id;
            this.ip = ip;
            this.port = port;
            this.es = es;
            this.hostSocket = hostSocket;
        }

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter the Address of the recipient and the message\nyou would like to send (ex: A:HelloWorld!)\nTo quit the device, type 'quit': ");
                String message = scanner.nextLine();
                if (message.trim().toLowerCase().equals("quit")) {
                    System.out.println("\nShutting down device\n");
                    scanner.close();
                    hostSocket.close();
                    es.shutdownNow(); // signals other thread to stop, too.
                    break;
                } else {
                    String[] messageArray = message.split(":", 2);
                    String sendID = messageArray[0];
                    String sendMessage = messageArray[1];

                    byte[] buffer = (id + ":" + sendID + ":" + sendMessage).getBytes();
                    try {
                        DatagramSocket socket = new DatagramSocket();
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), port);
                        socket.send(packet);
                        socket.close();
                    } catch (IOException e) {
                        System.err.println("Error sending packet: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static class ReceivePacket implements Runnable {

        private int port;
        private ExecutorService es;
        private DatagramSocket hostSocket;

        public ReceivePacket(int port, ExecutorService es, DatagramSocket hostSocket) {
            this.port = port;
            this.es = es;
            this.hostSocket = hostSocket;
        }

        @Override
        public void run() {
            try  {

                byte[] buffer = new byte[1024];
                while (!es.isShutdown()) { // keep running until quit
                    DatagramPacket dataRequest = new DatagramPacket(buffer, buffer.length);
                    System.out.println("If this prints, then the problem is below");
                    hostSocket.receive(dataRequest);
                    System.out.println("If this doesn't print, then we are stuck waiting to receive packet");
                    String received = new String(dataRequest.getData(), 0, dataRequest.getLength());
                    System.out.println("\nReceived: " + received);
                }
            } catch (IOException e) {
                System.out.println("Socket closed");
                //Prints error
//                System.err.println("Error receiving packet: " + e.getMessage());
//                e.printStackTrace();
            }
        }
    }


}
