class Main
{
	public static void main(String args[]) throws Exception
	{
		// Create an instance of producerconsumer
		ProducerConsumer pc = new ProducerConsumer();

		// Create threads for producer and consumer on the same instance of the producerconsumer
		Producer p = new Producer(pc);
		Consumer c1 = new Consumer(pc);
		Consumer c2 = new Consumer(pc);
		
		// Start all threads that will call the corresponding run method
		c1.start();
		c2.start();
		p.start();

		// Mandatory to join all threads to ensure they are not killed
		p.join();
		c1.join();
		c2.join();
	}
}
