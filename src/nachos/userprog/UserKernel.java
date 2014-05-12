package nachos.userprog;


import java.util.LinkedList;

import nachos.machine.*;
import nachos.threads.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		
		console = new SynchConsole(Machine.console());

	//	Lib.enableDebugFlags("ac");
		FreePhyPages.add(new node(0,Machine.processor().getNumPhysPages()));		
		freePagesNum=Machine.processor().getNumPhysPages();
		//	System.out.println("Hello everyone!!");
		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		super.selfTest();

	//	System.out.println("Testing the console device. Typed characters");
	//	System.out.println("will be echoed until q is typed.");

/*		char c;
		debug.print(Machine.processor().getNumPhysPages());
		do {
			c = (char) console.readByte(true);
		//	debug.print("I am OK");
			console.writeByte(c);
		} while (c != 'q');   */

		debug.print("***********I am ok*****************");
	}

	/**
	 * Returns the current process.
	 * 
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 * 
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 * 
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();
		
		UserProcess process = UserProcess.newUserProcess();

		String shellProgram = Machine.getShellProgramName();
//		System.out.println(shellProgram);
		Lib.assertTrue(process.execute(shellProgram, new String[] {}));

		KThread.finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;
	private static LinkedList<node> FreePhyPages=new LinkedList<node>();
	
	public static LinkedList<node> allocPhyMem(int num)
	{
		if(num==0) 
		{ System.out.println("You want to zero mem!"); return null;	}
		if(freePagesNum<num)
		{
		    debug.print("allocPhyMem:There is no so much free mem");
		    return null;
		}
		freePagesNum-=num;
		LinkedList<node> tempList=new LinkedList<node>();
		
		for(int i=0;i<FreePhyPages.size();++i)
		{
			node tempNode=FreePhyPages.get(i);
			if(tempNode.length>num)
			{
				tempNode.length-=num;
				tempList.addLast(newNode(tempNode.start,num));	
				tempNode.start+=num;
				return tempList;
			}
			else
			{
				num-=FreePhyPages.get(i).length;
				tempList.addLast(FreePhyPages.get(i));
				FreePhyPages.remove(i);
				i--;
			}
		}
		System.out.println("There is no enough physics to run the program");
		return null;
	}
	public static void freePhyMem(LinkedList<node> tempList)
	{
		for(int i=0;i<tempList.size();++i)
		{
			FreePhyPages.add(tempList.get(i));
			for(int j=0;j<tempList.get(i).length;++j)
			    freePagesNum++;
		}
	}
	public static node newNode(int s,int l)
	{		
		return new node(s,l);
	}
	public static int getFreePagesNum()
	{
	    return freePagesNum;
	}
    public static UserKernel getKernel() 
    {
        if(kernel instanceof UserKernel) return (UserKernel)kernel;
        return null;
    }
	public static int freePagesNum;

}

