import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.*;

public class Node extends Thread {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public int nodeID;
    public HashSet<Integer> neighbourIds = new HashSet<>();
    public HashMap<Integer, Integer> linkCost;
    public HashMap<Integer, Integer> linkBandwidth;
    public int[][] distanceTable;
    public int[] distanceVector;
    public int[] bottleneckBandwidthTable;
    public ArrayList<Integer> dynamicLinks;
    private int numNodes;

    private ServerSocket ss;
    public boolean changed = true;
    public boolean converged = false;
    public int round = 0;

    public ArrayList<ClientHandler> ClientPeers = new ArrayList<>();
    public ArrayList<ClientHandler> ServerPeers = new ArrayList<>();

    // Updated forwardingTable type
    public HashMap<String, Pair<Integer, Integer>> forwardingTable;

    private int port;

    private Instant instant = Instant.now();
    private long start = instant.getEpochSecond();

    public Node(int nodeID, HashMap<Integer, Integer> linkCost, HashMap<Integer, Integer> linkBandwidth,
                int numNodes, ArrayList<Integer> dynamicLinks) {
        this.nodeID = nodeID;
        this.linkCost = linkCost;
        this.linkBandwidth = linkBandwidth;
        this.numNodes = numNodes;
        this.dynamicLinks = dynamicLinks;

        distanceTable = new int[numNodes][numNodes];
        for (int i = 0; i < numNodes; i++) {
            Arrays.fill(distanceTable[i], 999); // initialize to infinity
        }
        distanceTable[nodeID][nodeID] = 0;

        distanceVector = new int[numNodes];
        Arrays.fill(distanceVector, 999);

        linkCost.forEach((k, v) -> {
            distanceTable[k][k] = v;
            distanceVector[k] = v;
        });
        distanceVector[nodeID] = 0;

        bottleneckBandwidthTable = new int[numNodes];

        port = 12345 + nodeID;
    }

    private static int getRandomNumberInRange(int min, int max) {
        if (min >= max) throw new IllegalArgumentException("max must be greater than min");
        return new Random().nextInt((max - min) + 1) + min;
    }

    public void setConverged(boolean converged) {
        this.converged = converged;
    }

    @Override
    public void run() {
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        scheduleOperation();
    }

    public synchronized void scheduleOperation() {
        Runnable updater = () -> {
            try {
                sendReceiveMessages();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(updater, 4, 1, SECONDS);
    }

    protected void sendReceiveMessages() throws IOException, ClassNotFoundException {
        // Dynamic link updates
        for (int neighbourId : dynamicLinks) {
            if (getRandomNumberInRange(1, 2) == 1) {
                changed = true;
                int newCost = getRandomNumberInRange(1, 10);
                linkCost.put(neighbourId, newCost);
                distanceTable[neighbourId][neighbourId] = newCost;
                distanceVector[neighbourId] = Arrays.stream(distanceTable[neighbourId]).min().getAsInt();
            }
        }

        sendUpdate();
        receiveMessages();
    }

    public void establishConnections() throws IOException {
        // Connect to lower ID neighbors as client
        for (Map.Entry<Integer, Integer> entry : linkCost.entrySet()) {
            int k = entry.getKey();
            if (k < nodeID) {
                Socket socket = new Socket("localhost", 12345 + k);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                ClientHandler t = new ClientHandler(socket, in, out, nodeID, k, this);
                ClientPeers.add(t);
                t.start();
            }
        }

        // Accept connections from higher ID neighbors as server
        for (Map.Entry<Integer, Integer> entry : linkCost.entrySet()) {
            int k = entry.getKey();
            if (k > nodeID) {
                Socket s = ss.accept();
                ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());

                ClientHandler t = new ClientHandler(s, in, out, k, nodeID, this);
                ServerPeers.add(t);
                t.start();
            }
        }
    }

    public void receiveUpdate(Message m) {
        this.neighbourIds.add(m.senderNodeID);
        for (int i = 0; i < numNodes; i++) {
            if (i != nodeID)
                distanceTable[i][m.senderNodeID] = m.senderDistanceVector[i] + linkCost.get(m.senderNodeID);
        }

        for (int i = 0; i < numNodes; i++) {
            int temp = Arrays.stream(distanceTable[i]).min().getAsInt();
            if (temp != distanceVector[i]) {
                distanceVector[i] = temp;
                changed = true;
            }
        }
    }

    public void receiveMessages() throws IOException, ClassNotFoundException {
        changed = false;
        for (ClientHandler handler : ClientPeers) handler.receiveMessage();
        for (ClientHandler handler : ServerPeers) handler.receiveMessage();

        instant = Instant.now();
        
        long time = instant.getEpochSecond() - start;
System.out.println("Node " + nodeID + " at simulation time: " + time + " seconds.");

        if (!converged) round++;
    }

    public boolean sendUpdate() throws IOException {
        for (ClientHandler handler : ClientPeers) {
            Message msg = changed ? new Message(nodeID, handler.serverID, distanceVector, true)
                    : new Message(nodeID, handler.serverID, null, false);
            handler.sendMessage(msg);
        }

        for (ClientHandler handler : ServerPeers) {
            Message msg = changed ? new Message(nodeID, handler.clientID, distanceVector, true)
                    : new Message(nodeID, handler.clientID, null, false);
            handler.sendMessage(msg);
        }

        return changed;
    }
}
