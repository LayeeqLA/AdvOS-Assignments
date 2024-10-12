package code;

import java.io.IOException;

import com.sun.nio.sctp.MessageInfo;

public class SnapshotService implements Runnable {

    // private static boolean firstSendCompleted = false;
    public static int snapshotDelay;
    private LocalState localState;
    private Node currentNode;
    private boolean root;

    public SnapshotService(LocalState localState, Node currentNode) {
        this.localState = localState;
        this.currentNode = currentNode;
        this.root = currentNode.getParent() == null;
    }

    @Override
    public void run() {
        try {
            if (root) {
                System.out.println("---ROOT IS WAITING FOR A snapshotDelay PERIOD---");
                Thread.sleep(snapshotDelay);
            }
            synchronized (localState) {
                assert !localState.isSnapshotActive(); // should not be active
                // send marker message to all neighbors
                Message markerMessage = new Message(currentNode.getId(), Message.MessageType.MARKER);
                for (Node neighbor : currentNode.getNeighbors()) {
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                    neighbor.getChannel().send(markerMessage.toByteBuffer(), messageInfo);
                    System.out.println("Marker Message sent to pid " + neighbor.getId());
                }
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println("===ERROR IN SNAPSHOT SERVICE===");
            e.printStackTrace();
        }
    }

}
