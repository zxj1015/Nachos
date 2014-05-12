package nachos.threads;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.PriorityScheduler.PriorityQueue;


/**
 * A scheduler that chooses threads using a lottery.
 * 
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 * 
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 * 
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking the
 * maximum).
 */
public class LotteryScheduler  extends Scheduler{
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}
	public LotteryQueue newThreadQueue(boolean transferPriority) {
		// implement me
		return new LotteryQueue(transferPriority);   
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
	 * Allocate a new lottery thread queue.
	 * 
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer tickets from
	 *            waiting threads to the owning thread.
	 * @return a new lottery thread queue.
	 */
	public static final int priorityDefault = 1;
	public static final int priorityMinimum = 1;
	public static final int priorityMaximum = Integer.MAX_VALUE;	


	protected LotteryThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new LotteryThreadState(thread);

		return (LotteryThreadState) thread.schedulingState;
	}
	
	protected class LotteryQueue extends ThreadQueue{

		private boolean transferPriority;
		KThread Hoodler=null;
		protected Queue<KThread> waitQueue = new LinkedList<KThread>();
		int totalLottery=0;
		LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;;
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
	//
	//		print();
			if(Hoodler!=null)
			{
				getThreadState(Hoodler).myHoldQueue.remove(this);
				if(this.transferPriority)
				getThreadState(Hoodler).dPriority-=this.totalLottery;
			}
			Hoodler=null;
			if(!this.waitQueue.isEmpty())
			{
				Hoodler=findNext();
				waitQueue.remove(Hoodler);
			}
			if(Hoodler!=null)
			{
				totalLottery-=getThreadState(Hoodler).getEffectivePriority();
				if(this.transferPriority)
				getThreadState(Hoodler).dPriority+=totalLottery;
				getThreadState(Hoodler).myHoldQueue.add(this);
				getThreadState(Hoodler).mywaitQueue=null;
			}
	//		print();
			return Hoodler;
		}
		public void print()
		{
			Lib.assertTrue(Machine.interrupt().disabled());
			KThread thread=null;
			int i=0;
			for(Iterator<KThread>it=this.waitQueue.iterator();it.hasNext();)
			{
				if(i%5==0) System.out.println();
				++i;
				thread=it.next();
				System.out.print(thread.toString()+":"+getThreadState(thread).getEffectivePriority()+"("+getThreadState(thread).dPriority+")	");
			}
			System.out.println();
		}
		
		KThread findNext()
		{
			Lib.assertTrue(totalLottery!=0, "totalLottery can't be zero");
		//	System.out.println(totalLottery);
			KThread thread=null;
	//		int temptotal=totalLottery;
			int lottery=0;
			Random r=new Random(100);
			while((lottery=(int) (r.nextDouble()*totalLottery))==totalLottery){System.out.print("123");}		
	//		System.out.println(lottery);
			for(Iterator<KThread> it=waitQueue.iterator();it.hasNext();)
			{
				thread=it.next();
				lottery-=getThreadState(thread).getEffectivePriority();
				if(lottery<0) 
				{
					return thread;
				}
			}
//			System.out.println("next is null!!!!");
			return null;	
		}		
	}
	protected class LotteryThreadState 
	{

		public LotteryThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
		}
		public int getPriority() {
			return this.priority;
		}
		public int getEffectivePriority() {
				return priority+dPriority;
		}
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			if(mywaitQueue==null)
			{
				this.priority=priority;
				return ;
			}
			int ini=this.mywaitQueue.totalLottery;
			mywaitQueue.totalLottery-=this.priority;
			this.priority=priority;
			mywaitQueue.totalLottery+=this.priority;
			totalHasChanged(ini,mywaitQueue.totalLottery);

			return ;
			// implement me
		}
		public void waitForAccess(LotteryQueue waitQueue) {
			
			mywaitQueue=waitQueue;		
			mywaitQueue.waitQueue.add(thread);

			int ini=mywaitQueue.totalLottery;
			mywaitQueue.totalLottery+=this.getEffectivePriority();
			totalHasChanged(ini,mywaitQueue.totalLottery);
		}
		private void totalHasChanged(int ini,int total) {
				if(!mywaitQueue.transferPriority) return ;
		
				if(mywaitQueue.Hoodler==null) return ;
				int ori=getThreadState(this.mywaitQueue.Hoodler).getEffectivePriority();
				getThreadState(mywaitQueue.Hoodler).dPriority+=total-ini;
				if(getThreadState(mywaitQueue.Hoodler).mywaitQueue!=null)
				{
					int temp=getThreadState(mywaitQueue.Hoodler).mywaitQueue.totalLottery;
					getThreadState(mywaitQueue.Hoodler).mywaitQueue.totalLottery+=getThreadState(mywaitQueue.Hoodler).getEffectivePriority()-ori;
					getThreadState(mywaitQueue.Hoodler).totalHasChanged(temp,getThreadState(mywaitQueue.Hoodler).mywaitQueue.totalLottery);
				}
		}
		public void acquire(LotteryQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			
			Lib.assertTrue(waitQueue.waitQueue.isEmpty(),"The waiting queue is not empty");
			waitQueue.Hoodler=this.thread;
			getThreadState(this.thread).myHoldQueue.add(waitQueue);
			return ;
			// implement me
		}	
		protected KThread thread;
		private int priority=priorityDefault;
		private int dPriority=0;
		private LotteryQueue mywaitQueue=null;
		private LinkedList<LotteryQueue> myHoldQueue= new LinkedList<LotteryQueue>();
	}
}
