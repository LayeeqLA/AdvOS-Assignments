import com.sun.nio.sctp.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class SCTPServer
{
	// Port number to open server for clients to connect
	// Client should connect to same port number that server opens
	static int PORT = 1234; 

	// Size of ByteBuffer to accept incoming messages
	static int MAX_MSG_SIZE = 4096;

	public static void main(String[] args) throws Exception
	{
		InetSocketAddress addr = new InetSocketAddress(PORT); // Get address from port number
		SctpServerChannel ssc = SctpServerChannel.open();//Open server channel
		ssc.bind(addr);//Bind server channel to address

		// Loop to allow all clients to connect
		while(true)
		{
			SctpChannel sc = ssc.accept(); // Wait for incoming connection from client
			System.out.println("Client connected");
			Thread.sleep(3000);

			ByteBuffer buf = ByteBuffer.allocateDirect(MAX_MSG_SIZE); 
			sc.receive(buf, null, null); // Messages are received over SCTP using ByteBuffer
			System.out.println("Message received from client: " + Message.fromByteBuffer(buf).message);


			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0); // MessageInfo for SCTP layer
			Message msg = new Message("I am fine!");
			sc.send(msg.toByteBuffer(), messageInfo); // Messages are sent over SCTP using ByteBuffer

			System.out.println("\t Message sent to client: " + msg.message);
		}
	}
}

