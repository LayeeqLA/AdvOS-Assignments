package code;

import java.io.IOException;

import com.sun.nio.sctp.MessageInfo;

public class SnapshotStarter implements Runnable {

    public static int snapshotDelay;
    private static boolean notFirstSnapshot;
    private LocalState localState;
    private Node currentNode;

    public SnapshotStarter(LocalState localState, Node currentNode) {
        this.localState = localState;
        this.currentNode = currentNode;
    }

    @Override
    public void run() {
        // MUST BE USED ONLY FOR ROOT NODE
        assert currentNode.getParent() == null;

        try {
            if (notFirstSnapshot) {
                System.out.println("---ROOT IS WAITING FOR A snapshotDelay PERIOD---");
                Thread.sleep(snapshotDelay);
            } else {
                notFirstSnapshot = true;
            }
            synchronized (localState) {
                localState.setSnapshotActive(currentNode.getId(), null, currentNode.getNeighborIds());
                currentNode.writeLocalState(localState.getClock());
                // send marker message to all neighbors
                Message markerMessage = new Message(currentNode.getId(), Message.MessageType.MARKER);
                for (Node neighbor : currentNode.getNeighbors()) {
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                    neighbor.getChannel().send(markerMessage.toByteBuffer(), messageInfo);
                    System.out.println("Marker Message sent to pid " + neighbor.getId());
                }
            }
        } catch (IOException | InterruptedException | ClassNotFoundException e) {
            System.err.println("===ERROR IN SNAPSHOT STARTER===");
            e.printStackTrace();
        }
    }

}
