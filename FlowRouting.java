import java.io.File;
import java.io.IOException;

public class FlowRouting {

    public static void main(String[] args) throws IOException {
        // Get all node files from the folder
        File nodeFolder = new File("./src/nodeFolder/");
        int nodeCount = nodeFolder.listFiles().length;

        // Read nodes and create Node objects
        for (File nodeFile : nodeFolder.listFiles()) {
            Node node = ModivSim.readNode(nodeFile, nodeCount);
            node.start(); // start the node thread
        }

        // Establish connections between nodes
        ModivSim.nodes.parallelStream().forEach(node -> {
            try {
                node.establishConnections();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Start convergence checking scheduler
        ModivSim.scheduleOperation();
    }
}
