import java.io.*;
import java.net.Socket;
@SuppressWarnings("unused")

public class ClientHandler extends Thread {   //EXTENDING THE THREAD
    private Socket socket;        //DECLARATION 
    private ObjectInputStream in;
    private ObjectOutputStream out; //OBJECT I/O STREAM

    int clientID; // the other node's ID  CLIENT &
    int serverID; // this node's ID                   SERVER NODE ID'S
    private Node parentNode;    //  LINK TO PARENT ROUTER LOGIC

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

    public void receiveMessage() {    // RECIEVING ROUTING UPDATES
        try {
            if (in.available() > 0) {  // CHECKING FOR INCOMING DATA
                Object obj = in.readObject();   // READING INCOMING OBJECT
                if (obj instanceof Message) {
                    parentNode.receiveUpdate((Message) obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

