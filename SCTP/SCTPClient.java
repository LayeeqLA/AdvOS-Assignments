import com.sun.nio.sctp.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class SCTPClient
{
	// PORT to connect to server
	// Port number should be same as the port opened by the server
	static int PORT = 1234;

	// Size of ByteBuffer to accept incoming messages
	static int MAX_MSG_SIZE = 4096;

	public static void main(String[] args) throws Exception
	{
		// Get address of server using name and port number
		InetSocketAddress addr = new InetSocketAddress("dc02", PORT);

		System.out.println("Trying connection to server");
		Thread.sleep(3000);
		SctpChannel sc = SctpChannel.open(addr, 0, 0); // Connect to server using the address
		System.out.println("Connected to Server");
		
		MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
		Message msg = new Message("How are you?");
		sc.send(msg.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer

		System.out.println("\t Message sent to server: " + msg.message);
		
		ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE); // Messages are received over SCTP using ByteBuffer
		sc.receive(buf, null, null);

		Thread.sleep(3000);
		System.out.println("Message received from server: " + Message.fromByteBuffer(buf).message);
	}
}

