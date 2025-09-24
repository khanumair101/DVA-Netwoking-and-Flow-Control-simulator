import java.io.*;
import java.net.Socket;
@SuppressWarnings("unused")

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    int clientID; // the other node's ID
    int serverID; // this node's ID
    private Node parentNode;

    public ClientHandler(Socket socket, ObjectInputStream in, ObjectOutputStream out,
                         int clientID, int serverID, Node parentNode) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.clientID = clientID;
        this.serverID = serverID;
        this.parentNode = parentNode;
    }

    @Override
    public void run() {
        // Thread continuously listening for incoming messages
        try {
            while (true) {
                receiveMessage();
                Thread.sleep(1000); // small delay to prevent busy wait
            }
        } catch (Exception e) {
            System.out.println("ClientHandler error for node " + parentNode.nodeID);
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        try {
            out.reset();
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessage() {
        try {
            if (in.available() > 0) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    parentNode.receiveUpdate((Message) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
