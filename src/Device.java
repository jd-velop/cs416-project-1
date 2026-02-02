import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Device {

    public String id;
    public int port;
    public String ip;

    public static void main(String[] args) throws SocketException {
        id = args[0];
        ip = "127.0.0.1";
        port = 3094;
        System.out.println();
        ExecutorService es = Executors.newFixedThreadPool(2);
        //needs a way to grab info from config file
        //Then send the info to the multithreading
        es.submit(new SendPacket(id, ip, port, es));
        es.submit(new ReceivePacket());
    }

    public static void getIPPort(String id) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    }


    public Device(String id, String ip, int port) {
        this.id = id;
        this.port = port;
        this.ip = ip;
    }


    public static class SendPacket implements Runnable {

        private String id;
        private String ip;
        private int port;
        private ExecutorService es;

        public SendPacket(String id, String ip, int port, ExecutorService es) {
            this.id = id;
            this.ip = ip;
            this.port = port;
            this.es = es;
        }

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter the Address of the recipient and the message\nyou would like to send (ex: A:HelloWorld!)\nTo quit the device, type 'quit': ");
                String message = scanner.nextLine();
                if (message.toLowerCase().equals("quit")) {
                    System.out.println("\nShutting down device\n");
                    es.shutdown();
                    break;
                } else {
                    String[] messageArray = message.split(":", 2);
                    String sendID = messageArray[0];
                    String sendMessage = messageArray[1];
                    System.out.println(sendID);
                    System.out.println(sendMessage);
                }
            }
        }
    }

    public static class ReceivePacket implements Runnable {


        public ReceivePacket() throws SocketException {
        }

        @Override
        public void run() {
            int port_Test = 1;
            try {
                DatagramSocket deviceSocket = new DatagramSocket(port_Test);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            DatagramPacket dataRequest = new DatagramPacket(new byte[1024], 1024);
        }


    }

}
