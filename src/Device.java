import java.util.ArrayList;
import java.util.List;

public class Device {

    public String id;
    public int port;
    public String ip;
    public List<String> virtualIps;
    public String gateway;

    public Device(String id, String ip, int port) {
        this.id = id;
        this.port = port;
        this.ip = ip;
        this.virtualIps = new ArrayList<>();
        this.gateway = null;
    }

}
