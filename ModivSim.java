import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.*;

public class ModivSim {
    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static HashMap<String, ArrayList<PathStruct>> paths = new HashMap<>();
    static ArrayList<Node> nodes = new ArrayList<>();
    private static int numNodes;
    private static boolean[] visited;
    private static Stack<Integer> path = new Stack<>();
    static int round = 0;

    public static Node readNode(File filePath, int nodeCount) throws IOException {
        String content;
        HashMap<Integer, Integer> linkCost = new HashMap<>();
        HashMap<Integer, Integer> linkBandwidth = new HashMap<>();
        ArrayList<Integer> dynamicLinks = new ArrayList<>();

        content = new String(Files.readAllBytes(Paths.get(filePath.toURI())));
        String[] tokens = content.split(",[(]");

        int nodeId = Integer.parseInt(tokens[0]);

        for (int i = 1; i < tokens.length; i++) {
            String neighbour = tokens[i].replaceAll("[()]", "");
            String[] ntokens = neighbour.split(",");
            int nId = Integer.parseInt(ntokens[0]);
            if (ntokens[1].equals("x")) {
                dynamicLinks.add(nId);
                linkCost.put(nId, getRandomNumberInRange(1, 10));
            } else {
                linkCost.put(nId, Integer.parseInt(ntokens[1]));
            }
            linkBandwidth.put(nId, Integer.parseInt(ntokens[2]));
        }

        Node ans = new Node(nodeId, linkCost, linkBandwidth, nodeCount, dynamicLinks);
        nodes.add(ans);
        return ans;
    }

    public static synchronized void scheduleOperation() {
        final Runnable updater = ModivSim::checkConvergence;
        scheduler.scheduleAtFixedRate(updater, 5, 1, SECONDS);
    }

    private static void checkConvergence() {
        boolean converged = true;
        for (Node node : nodes) {
            if (node.changed) converged = false;
        }

        if (converged) {
            System.out.println("Simulation converged.");

            for (int i = 0; i < numNodes; i++) {
                nodes.get(i).setConverged(true);
                for (int j = 0; j < numNodes; j++) {
                    visited = new boolean[numNodes];
                    path = new Stack<>();
                    ArrayList<PathStruct> pathStructs = new ArrayList<>();
                    printAllPaths(i, j, 0, pathStructs);
                    paths.put(i + "-" + j, pathStructs);
                }
            }

            populateForwardingTables();

            for (Node node : nodes) {
                System.out.println(node.forwardingTable);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("./src/FlowRoutingFolder/forwardingTable.txt"))) {
                int size = nodes.size();
                for (int i = 0; i < size; i++) {
                    writer.write(nodes.get(i).forwardingTable.toString());
                    if (i != size - 1) writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            scheduler.shutdown();
        } else {
            round++;
        }
    }

    private static void populateForwardingTables() {
        for (Node node : nodes) {
            HashMap<String, Pair<Integer, Integer>> forwardingTable = new HashMap<>();

            for (int i = 0; i < numNodes; i++) {
                if (node.nodeID != i) {
                    ArrayList<PathStruct> allPaths = paths.get(node.nodeID + "-" + i);
                    Collections.sort(allPaths, Comparator.comparingInt(PathStruct::getCost));
                    PathStruct minPath = allPaths.get(0);

                    int firstHop = (minPath.path.size() == 1) ? i : minPath.path.get(1);
                    int secondHop = 0;
                    for (int j = 1; j < allPaths.size(); j++) {
                        PathStruct secondMinPath = allPaths.get(j);
                        secondHop = (secondMinPath.path.size() == 1) ? i : secondMinPath.path.get(1);
                        if (firstHop != secondHop) break;
                    }

                    forwardingTable.put(String.valueOf(i), new Pair<>(firstHop, secondHop));
                }
            }
            node.forwardingTable = forwardingTable;
        }
    }

    private static void printAllPaths(int src, int dest, int currentCost, ArrayList<PathStruct> pathStructs) {
        if (src == dest) {
            Stack<Integer> newPath = new Stack<>();
            newPath.addAll(path);
            pathStructs.add(new PathStruct(newPath, currentCost));
            System.out.println(newPath + " cost = " + currentCost);
        } else {
            visited[src] = true;
            path.push(src);
            for (int adj : nodes.get(src).neighbourIds) {
                if (!visited[adj]) {
                    printAllPaths(adj, dest, nodes.get(src).distanceTable[adj][adj] + currentCost, pathStructs);
                }
            }
            visited[src] = false;
            path.pop();
        }
    }

    private static int getRandomNumberInRange(int min, int max) {
        if (min >= max) throw new IllegalArgumentException("max must be greater than min");
        return new Random().nextInt((max - min) + 1) + min;
    }

    public static void main(String[] args) throws IOException {
        File nodeFolder = new File("./src/nodeFolder/");
        int nodeCount = nodeFolder.listFiles().length;
        numNodes = nodeCount;
        visited = new boolean[numNodes];

        for (int i = 0; i < nodeCount; i++) {
            Thread t = readNode(nodeFolder.listFiles()[i], nodeCount);
            t.start();
        }

        nodes.parallelStream().forEach(node -> {
            try {
                node.establishConnections();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        scheduleOperation();
    }
}
