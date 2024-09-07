// Class to produce and consume synchronously
class ProducerConsumer 
{
	// Variable to keep track of items that can be consumed
	static int remaining = 0;

	// Producer produces
	// Only one thread gets access to any "synchronized" method in the same instance of this class
	public synchronized void produce() throws Exception
	{
		remaining++;
		// Notify notifies any thread that called wait() 
		// The thread(s) that were notified will not wake up instantly and can only get the lock once this thread gives up the lock
		notifyAll(); 
		System.out.println("Produced new item. Remaining items = " + remaining);
	}

	// Consumer consumes
	public synchronized void consume() throws Exception
	{
		// Wait gives up the lock held by synchronized
		// The lock is reacquired once the waiting process is notified
		while(remaining <= 0) wait();
		remaining--;
		System.out.println("Consumed item. Remaining items = " + remaining);
	}
}
