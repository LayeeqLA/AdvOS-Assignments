package code;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

import code.Message.MessageType;

public class SocketService implements Runnable {

    Node selfNode;
    int connectionCount;
    List<Thread> clientThreads;
    Map<Integer, Integer> receivedInfo;

    public SocketService(Node selfNode, int connectionCount) {
        this.selfNode = selfNode;
        this.connectionCount = connectionCount;
    }

    @Override
    public void run() {
        clientThreads = new ArrayList<>();
        receivedInfo = new ConcurrentHashMap<>();

        // SETUP SERVER
        InetSocketAddress addr = new InetSocketAddress(selfNode.getPort()); // Get address from port number
        SctpServerChannel ssc;
        try {
            ssc = SctpServerChannel.open(); // Open server channel
            ssc.bind(addr); // Bind server channel to address
            System.out.println("Started SERVER on nodeId: " + selfNode.getId() + " on port: " + selfNode.getPort());

            for (int i = 0; i < connectionCount; i++) {
                SctpChannel clientConnection = ssc.accept();
                Thread clientThread = new Thread(new ClientHandler(clientConnection, receivedInfo));
                clientThread.start();
                clientThreads.add(clientThread);
            }

            for (Thread clientThread : clientThreads) {
                clientThread.join();
            }

            printReceivedInformation();
            ssc.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("xxxxx---SOCKET SERVICE ERROR---xxxxx");
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void printReceivedInformation() {
        System.out.println("*****RECEIVED INFORMATION*****");
        for (Entry<Integer, Integer> info : receivedInfo.entrySet()) {
            System.out.println(info.getKey() + " -> " + info.getValue());
        }
    }

    private class ClientHandler implements Runnable {

        private final SctpChannel channel;
        private Map<Integer, Integer> receivedData;

        public ClientHandler(SctpChannel channel, Map<Integer, Integer> receivedData) {
            this.channel = channel;
            this.receivedData = receivedData;
        }

        @Override
        public void run() {
            try {
                int recvCount = 0;
                int pid = -1;
                while (recvCount < Constants.MAX_MESSAGES) {
                    // keep listening and receive incoming messages
                    ByteBuffer buf = ByteBuffer.allocateDirect(Constants.MAX_MSG_SIZE);
                    channel.receive(buf, null, null);
                    recvCount++;
                    Message message = Message.fromByteBuffer(buf);
                    message.print();
                    if (pid == -1) {
                        pid = message.getSender();
                    } else {
                        assert pid == message.getSender();
                        // SHOULD RECEIVE SAME SENDER PID ON A SINGLE THREAD
                    }
                    if (message.getmType() == MessageType.DATA) {
                        receivedData.merge(message.getSender(), message.getData(), Integer::sum);
                    } else {
                        System.out.println(message.getmType() + " unexpected!");
                    }
                }

                if (pid == 0) {
                    System.out.println("Waiting for TERMINATE SINGAL from node 0");
                    ByteBuffer buf = ByteBuffer.allocateDirect(Constants.MAX_MSG_SIZE);
                    channel.receive(buf, null, null);
                    Message message = Message.fromByteBuffer(buf);
                    assert message.getmType() == MessageType.TERMINATE;
                }

            } catch (IOException | ClassNotFoundException e) {
                System.err.println("xxxxx---CLIENT HANDLER ERROR---xxxxx");
                System.err.println("THREAD: " + Thread.currentThread().getName());
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }

}
