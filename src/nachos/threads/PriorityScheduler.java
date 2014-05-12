package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
		{
			Machine.interrupt().restore(intStatus);
			return false;
		}
		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
		{
			Machine.interrupt().restore(intStatus); 
			return false;
		}
		setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			

		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
	//		print();
			return ;
		}
		
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
		//	System.out.println("------------------------------------------before nextThread");
	//		print();

			if(Hoodler!=null)
			{
				getThreadState(Hoodler).myHoldQueue.remove(this);
				int temp=0;
				PriorityQueue  tempQueue=null;
				int max=0;
				PriorityQueue maxQueue=null;
				for(Iterator<PriorityQueue> it=getThreadState(Hoodler).myHoldQueue.iterator();it.hasNext();)
				{
					tempQueue=it.next();
					temp=tempQueue.MaxPriority;
					if(temp>max)
					{
						max=temp;
						maxQueue=tempQueue;
					}
				}
				if(max!=getThreadState(Hoodler).dPriority) getThreadState(Hoodler).dPriority=max;
				getThreadState(Hoodler).maxFrom=maxQueue;
			}
			Hoodler=null;
			if(!waitQueue.isEmpty())
			{
				Hoodler=findNext();
				waitQueue.remove(Hoodler);
			}

			int max=0;
			max=findMax();
			this.MaxPriority=max;
			if(Hoodler!=null)
			{
				if(getThreadState(Hoodler).dPriority<max)
				{
					if(this.transferPriority)
					getThreadState(Hoodler).dPriority=max;
					getThreadState(Hoodler).maxFrom=this;	
				}
				getThreadState(Hoodler).myHoldQueue.add(this);
				getThreadState(Hoodler).mywaitQueue=null;
			}
	//		System.out.println("--------------------------------------------after nextThread");
	//		print();
			return Hoodler;
			// implement me
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			
				if(findNext()!=null)
					return getThreadState(findNext());
				return null;
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			LinkedList<Queue<KThread>> tempQueue=new LinkedList<Queue<KThread>>();
			for(int i=0;i<=7;++i)
			{
				tempQueue.add(i, new LinkedList<KThread>());
			}
			KThread thread=null;
			for(Iterator<KThread> it=waitQueue.iterator();it.hasNext();)
			{
				thread=it.next();
				tempQueue.get(getThreadState(thread).getEffectivePriority()).add(thread);
			}
			System.out.println("Queue donation:"+this.transferPriority);
			for(int i=0;i<=7;++i)
			{
				Queue<KThread> list=tempQueue.get(i);		
				System.out.println("The Thread's Priority of "+i+"is:");
				for(Iterator<KThread> it=list.iterator();it.hasNext();)
				{
					KThread tempthread=it.next();
					System.out.print(tempthread.toString()+"["+getThreadState(tempthread).getPriority()+",d"+getThreadState(tempthread).dPriority+"]"+"      ");
				}
				System.out.println();
			}
			// implement me (if you want)
		}
		
		private KThread findNext()
		{
			KThread thread=null;
			KThread maxThread=null;
			int max=-1;
			for(Iterator<KThread> it=waitQueue.iterator();it.hasNext();)
			{
				thread=it.next();
				if(getThreadState(thread).getEffectivePriority()>max)
				{
					max=getThreadState(thread).getEffectivePriority();
					maxThread=thread;
				}
			}
			return maxThread; 
		}
		private int findMax()
		{
			KThread thread=null;
			int max=0;
			for(Iterator<KThread> it=waitQueue.iterator();it.hasNext();)
			{
				thread=it.next();
				if(getThreadState(thread).getEffectivePriority()>max)
				{
					max=getThreadState(thread).getEffectivePriority();
				}
			}
			return max;
		}
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		protected Queue<KThread> waitQueue = new LinkedList<KThread>();
		
		protected KThread Hoodler;
		private int MaxPriority=0;
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			if(dPriority>priority)
				return dPriority;
			else
				return priority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			if(mywaitQueue==null)
			{
				this.priority=priority;
				return ;
			}
			
			if(priority<this.getEffectivePriority())
			{
					if(this.priority<=this.dPriority)
					{	this.priority=priority; return;}
					else
					{
						this.priority=priority;

						int i=mywaitQueue.findMax();
						if(i!=mywaitQueue.MaxPriority)
						{
							mywaitQueue.MaxPriority=i;
							maxHasChanged(i);
						}
					
					}
			}
			else if(priority==this.getEffectivePriority())
			{
				this.priority=priority;
				return ;
			}
			else
			{
					this.priority=priority;
					int nowMax=mywaitQueue.MaxPriority;
					nowMax=mywaitQueue.findMax();
					if(mywaitQueue.MaxPriority!=nowMax)
					{
						mywaitQueue.MaxPriority=nowMax;
						maxHasChanged(nowMax);
					}
			}
					return ;
			// implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			mywaitQueue=waitQueue;		
			mywaitQueue.waitQueue.add(thread);

			if(this.getEffectivePriority()>mywaitQueue.MaxPriority)
			{
					mywaitQueue.MaxPriority=this.getEffectivePriority();
					maxHasChanged(this.getEffectivePriority());
			}
			
			// implement me
		}
		private int maxHasChanged(int max)
		{
			if(!mywaitQueue.transferPriority) return 0;
		//	System.out.println("**********************************************");
			if(mywaitQueue.Hoodler==null) return 0;
	//		System.out.println("-------------------------------------------------------------");
			if(getThreadState(mywaitQueue.Hoodler).getEffectivePriority()>max)
			{
			//		if(getThreadState(mywaitQueue.Hoodler).maxFrom==null)	return 0;
					if(!getThreadState(mywaitQueue.Hoodler).maxFrom.equals(mywaitQueue)) return 0;
					else
					{
						 int tempmax=max;
						 int temp=-1;
						 PriorityQueue tempQueue=null;
						 PriorityQueue tempMaxQueue=null;
						 for(Iterator<PriorityQueue> it=getThreadState(mywaitQueue.Hoodler).myHoldQueue.iterator();it.hasNext();)
						 {
							 tempQueue=it.next();
							 temp=tempQueue.MaxPriority;
							 if(temp>tempmax)
							 {
								 tempmax=temp;
								 tempMaxQueue=tempQueue;
							 }
						 }
						if(tempmax!=max)
						{
								getThreadState(mywaitQueue.Hoodler).maxFrom=tempMaxQueue;
						}
						int inip=getThreadState(mywaitQueue.Hoodler).getEffectivePriority();
						getThreadState(mywaitQueue.Hoodler).dPriority=tempmax;
						if(getThreadState(mywaitQueue.Hoodler).mywaitQueue!=null)
						{							
							int ini=getThreadState(mywaitQueue.Hoodler).mywaitQueue.MaxPriority;
							int i=getThreadState(mywaitQueue.Hoodler).mywaitQueue.findMax();
							getThreadState(mywaitQueue.Hoodler).mywaitQueue.MaxPriority=i;
							if(ini!=i)
								getThreadState(mywaitQueue.Hoodler).maxHasChanged(i);
						}
						return -1;
					}
			}
			else if(getThreadState(mywaitQueue.Hoodler).getEffectivePriority()==mywaitQueue.MaxPriority)
			{
				return 0;
			}
			else
			{
	//			System.out.println("-----------------------------");
				int ini=getThreadState(mywaitQueue.Hoodler).getEffectivePriority();
				getThreadState(mywaitQueue.Hoodler).dPriority=mywaitQueue.MaxPriority;

				if(getThreadState(mywaitQueue.Hoodler).mywaitQueue!=null
						&&getThreadState(mywaitQueue.Hoodler).getEffectivePriority()>getThreadState(mywaitQueue.Hoodler).mywaitQueue.MaxPriority)
				{
						getThreadState(mywaitQueue.Hoodler).mywaitQueue.MaxPriority=getThreadState(mywaitQueue.Hoodler).getEffectivePriority();
						getThreadState(mywaitQueue.Hoodler).maxHasChanged(getThreadState(mywaitQueue.Hoodler).getEffectivePriority());
					
				}	
				getThreadState(mywaitQueue.Hoodler).maxFrom=mywaitQueue;
				return 1;
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire   
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			
		 Lib.assertTrue(waitQueue.waitQueue.isEmpty(),"浼樺厛绾ч槦鍒椾笉涓虹┖");
			waitQueue.Hoodler=this.thread;
			getThreadState(this.thread).myHoldQueue.add(waitQueue);
			if(this.maxFrom==null) 
				this.maxFrom=waitQueue;
			return ;
			// implement me
		}

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		
		protected int dPriority=0;
	
		private PriorityQueue mywaitQueue=null;
		private PriorityQueue maxFrom=null;
		private LinkedList<PriorityQueue> myHoldQueue= new LinkedList<PriorityQueue>();
	}
}
