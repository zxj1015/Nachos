package nachos.threads;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		
		boolean flag=Machine.interrupt().disable();
		speakerNum++;
		if(hasSpoken||speakerNum!=1)
		{
			waitToSpeak.waitForAccess(KThread.currentThread());
			KThread.sleep();
		}
		hasSpoken=true;
		this.word=word;
		KThread thread=null;
		if((thread=waitToListen.nextThread())!=null)
			thread.ready();
		speakerNum--;
	//	System.out.println("speak:"+word+" "+KThread.currentThread().toString());
		Machine.interrupt().setStatus(flag);

		return ;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		boolean flag=Machine.interrupt().disable();
		waiterNum++;
		if(!hasSpoken||waiterNum!=1)
		{
			waitToListen.waitForAccess(KThread.currentThread());
			KThread.sleep();
		}
		int listenWord=0;
		hasSpoken=false;
		listenWord=this.word;
		KThread thread=null;
		if((thread=waitToSpeak.nextThread())!=null)
			thread.ready();	
		waiterNum--;
		Machine.interrupt().restore(flag);
	//	System.out.println("listen:"+this.word+" "+KThread.currentThread().toString());
		return listenWord;
		
	}
	private ThreadQueue waitToSpeak=ThreadedKernel.scheduler.newThreadQueue(true);
	private ThreadQueue waitToListen=ThreadedKernel.scheduler.newThreadQueue(true);
	int word=0;
	boolean hasSpoken=false;
	int waiterNum=0;
	int speakerNum=0;
}
