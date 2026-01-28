import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Device {

    public static String id;
    public static int port;
    public static String ip;

    public static void main(String[] args) throws SocketException {
        id = args[0];
        ip = "127.0.0.1";
        port = 3094;
        System.out.println();
        ExecutorService es = Executors.newFixedThreadPool(2);
        //needs a way to grab info from config file
        //Then send the info to the multithreading
        es.submit(new SendPacket(id, ip, port));
        es.submit(new ReceivePacket());
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

        public SendPacket(String id, String ip, int port) {
            this.id = id;
            this.ip = ip;
            this.port = port;
        }

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the Address of the recipient and the message you would\nlike to send (ex: A:HelloWorld!):");
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

