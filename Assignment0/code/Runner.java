package code;

import com.sun.nio.sctp.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Runner {

    final static String CONFIG_PATH = "./config-local.txt"; // TODO: change back to config.txt
    static int MAX_MSG_SIZE = 4096;
    static int nodeCount = -1;
    static int nodeId;
    static int nodesConnected = 0;

    public static void main(String[] args) {
        System.out.println("Starting Service!");

        if (args.length != 1) {
            System.err.println("Need exactly one arg");
            System.exit(-1);
        }

        try {
            nodeId = Integer.parseInt(args[0]);
            System.out.println("PID for this host: " + nodeId);

            List<Node> nodes = processConfig();
            Node currentNode = nodes.get(nodeId);

            // SETUP SERVER
            InetSocketAddress addr = new InetSocketAddress(currentNode.getPort()); // Get address from port number
            SctpServerChannel ssc = SctpServerChannel.open();// Open server channel
            ssc.bind(addr);// Bind server channel to address

            System.out.println("Started SERVER on nodeId: " + nodeId + " on port: " + currentNode.getPort());

            // TEMP
            ssc.close();

        } catch (NumberFormatException | IOException e) {
            System.err.println("xxxxx---Processing error occured---xxxxx");
            System.err.println(e.getMessage());
            System.err.println(e.getStackTrace());
        }

    }

    public static List<Node> processConfig() throws IOException {
        ArrayList<Node> nodes = new ArrayList<>();
        List<String> allLines = Files.readAllLines(Paths.get(CONFIG_PATH));

        // FIX BOM encoding for UTF-16 and UTF-8 config files
        String firstLine = allLines.get(0);
        if (firstLine.codePointAt(0) == 0xfeff) {
            allLines.set(0, firstLine.substring(1, firstLine.length()));
        }
        
        for (String line : allLines) {
            // System.out.println(line);
            line = line.trim();
            if (line.startsWith("#") || "".equalsIgnoreCase(line)) {
                // ignore this line
                continue;
            }

            if (nodeCount <= 0) {
                // read number of nodes; parse as int
                nodeCount = Integer.parseInt(line.split(" ")[0]);
                System.out.println("TOTAL NODES: " + nodeCount);
            } else {
                // read node entry
                String nodeInfo[] = line.split(" ");
                nodes.add(new Node(Integer.parseInt(nodeInfo[0]), nodeInfo[1], Integer.parseInt(nodeInfo[2])));
            }
        }

        if (nodeCount != nodes.size()) {
            System.err.println("Config file node count mismatch");
            System.exit(-1);
        }

        return nodes;
    }
}