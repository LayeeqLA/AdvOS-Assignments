// Class to create Consumer thread
class Consumer extends Thread
{
	ProducerConsumer pc;
	static int iterations = 5;
	public Consumer(ProducerConsumer pc)
	{
		this.pc = pc;
	}

	// Run method is called at the beginning of a thread
	public void run()
	{
		try{
			for(int i = 0; i < iterations; i++)
			{
				pc.consume();
			}
		}
		catch(Exception e)
		{
		}
	}
}
