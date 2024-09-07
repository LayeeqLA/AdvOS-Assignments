// Class to create Producer thread
class Producer extends Thread
{
	static int iterations = 5;
	ProducerConsumer pc;
	public Producer(ProducerConsumer pc)
	{
		this.pc = pc;
	}

	// Run method is called at the beginning of a thread
	public void run()
	{
		try{
			for(int i = 0; i < iterations; i++)
			{
				Thread.sleep(1000);
				pc.produce();
				pc.produce();
			}
		}
		catch(Exception e)
		{
		}
	}
}
