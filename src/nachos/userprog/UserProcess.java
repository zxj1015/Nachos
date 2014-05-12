package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.vm.VMProcess;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
//		for(int i=0;i<numPhysPages;++i)
//			pageTable[i]=new TranslationEntry(i,i,true,false,false,false);
		descriptortable[0]=UserKernel.console.openForReading();
		descriptortable[1]=UserKernel.console.openForWriting();
		processLock.acquire();
		processID=ID++;  
		aliveProcess++;
		processLock.release();
		idToProcess.put(processID,this);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;
		//System.out.print("hello word-----------------------------");
		thread=new UThread(this);
		thread.setName(name).fork();
		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr
	 *            the starting virtual address of the null-terminated string.
	 * @param maxLength
	 *            the maximum number of characters in the string, not including
	 *            the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 *         found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);
	//	debug.print(bytesRead);
		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
			{
			    String s=new String(bytes, 0, length);
	//		    Lib.debug(dbgSyscall,"readvirMem:"+"vaddr-"+vaddr+"length:"+length+" "+s);
			    return	s;
			}
		}
		Lib.debug(dbgSyscall, "readvirMem:"+"vaddr-"+vaddr+" nothing is read");
		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to read.
	 * @param data
	 *            the array where the data will be stored.
	 * @param offset
	 *            the first byte to write in the array.
	 * @param length
	 *            the number of bytes to transfer from virtual memory to the
	 *            array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
		byte[] memory = Machine.processor().getMemory();
		int amount=0;
		int paddr=getPhyAddr(vaddr);
		if (paddr==-1)
			return 0;
		if (UserProcess.pageSize-vaddr%(UserProcess.pageSize)>length)
		{
			System.arraycopy(memory, paddr, data,offset,length);
			amount+=length;
			return amount;
		}
		else
		{
			System.arraycopy(memory, paddr, data, offset,UserProcess.pageSize-vaddr%(UserProcess.pageSize));
			amount+=UserProcess.pageSize-vaddr%(UserProcess.pageSize);
			offset+=UserProcess.pageSize-vaddr%(UserProcess.pageSize);
			length-=UserProcess.pageSize-vaddr%(UserProcess.pageSize);
			vaddr+=UserProcess.pageSize-vaddr%(UserProcess.pageSize);
	//		debug.print(vaddr);
			while(length>=UserProcess.pageSize)
			{
				paddr=getPhyAddr(vaddr);
				if(paddr==-1) return amount;
				System.arraycopy(memory, paddr, data, offset, UserProcess.pageSize);
				amount+=UserProcess.pageSize;
				offset+=UserProcess.pageSize;
				length-=UserProcess.pageSize;
				vaddr+=UserProcess.pageSize;
			}
			if(length==0) return amount;
			else
			{
				paddr=getPhyAddr(vaddr);
				if(paddr==-1) return amount;
				System.arraycopy(memory, paddr, data, offset, length);
				amount+=length;
			//	debug.print(new String(data));
				return amount;
			}
		}
	}
	int getPhyAddr(int vaddr)
	{
		int paddr=0;
		int pagenum=0;
		int pageoffset=0;
	    pagenum=getPage(vaddr);
	//    debug.print(pagenum);
		pageoffset=getOffset(vaddr);
		if(pageTable[pagenum]==null)
		{   
		    debug.print("This page is not exist");
		    return -1;
		}
		paddr=pageTable[pagenum].ppn*pageSize+pageoffset;
	//	debug.print(paddr);
		return paddr;
	}
	int getPage(int vaddr)
	{
	    return (int) (((long) vaddr & 0xFFFFFFFFL) / pageSize);
	}
	int getOffset(int vaddr)
	{
	       return (int) (((long) vaddr & 0xFFFFFFFFL) % pageSize);
	}
	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr
	 *            the first byte of virtual memory to write.
	 * @param data
	 *            the array containing the data to transfer.
	 * @param offset
	 *            the first byte to transfer from the array.
	 * @param length
	 *            the number of bytes to transfer from the array to virtual
	 *            memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);
	//	debug.print("I am zhixingle");
		byte[] memory = Machine.processor().getMemory();
	//	debug.print("vaddr:"+vaddr);
		int lengthini=length;
		int vaddrini=vaddr;
		//first to see is the page is read only!
		if(UserProcess.pageSize-getOffset(vaddr)<length)
		{
			if(pageTable[getPage(vaddr)].readOnly)
			{
			    debug.print("writevirMem: The page is read Only");
				return 0;
			}
			vaddr+=UserProcess.pageSize-getOffset(vaddr);
			length-=UserProcess.pageSize-getOffset(vaddr);
			while(length>=UserProcess.pageSize)
			{
				if(pageTable[getPage(vaddr)].readOnly)
					return 0;
				vaddr+=UserProcess.pageSize;
				length-=UserProcess.pageSize;
			}
			if(length>0)
			{
				if(pageTable[getPage(vaddr)].readOnly)
					return 0;
			}
		}
		//now to write data
		length=lengthini;
		vaddr=vaddrini;
		int paddr=getPhyAddr(vaddr);
		int amount=0;
	//	debug.print("vaddr:"+vaddr);
	//	debug.print("paddr:"+paddr);
		if (paddr==-1)
		{
		    debug.print("writeVirMem: The addr is wrong");
			return 0;
		}
		if (UserProcess.pageSize-getOffset(vaddr)>length)
		{
			System.arraycopy(data,offset,memory, paddr,length);
			amount+=length;
			pageTable[getPage(vaddr)].dirty=true;
		//	debug.print();
			return amount;
		}
		else
		{
			System.arraycopy(data, offset,memory, paddr,UserProcess.pageSize-vaddr%(UserProcess.pageSize));
            pageTable[getPage(vaddr)].dirty=true;
			amount+=UserProcess.pageSize-getOffset(vaddr);
			offset+=UserProcess.pageSize-getOffset(vaddr);
			length-=UserProcess.pageSize-getOffset(vaddr);
			vaddr+=UserProcess.pageSize-getOffset(vaddr);
			
			while(length>=UserProcess.pageSize)
			{
				paddr=getPhyAddr(vaddr);
				if(paddr==-1) return amount;
				System.arraycopy(data, offset,memory, paddr, UserProcess.pageSize);
	            pageTable[getPage(vaddr)].dirty=true;
				amount+=UserProcess.pageSize;
				offset+=UserProcess.pageSize;
				length-=UserProcess.pageSize;
				vaddr+=UserProcess.pageSize;
			}
			if(length==0) return amount;
			else
			{
				paddr=getPhyAddr(vaddr);
				if(paddr==-1) return amount;
				System.arraycopy(data, offset,memory, paddr,length);
	            pageTable[getPage(vaddr)].dirty=true;
				amount+=length;
				return amount;
			}
		}
		
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name
	 *            the name of the file containing the executable.
	 * @param args
	 *            the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	protected boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	    
		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
			exe=executable;
		} catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}
		
		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;
		
		if (!loadSections())
			return false;
		//my code allocate for stack and argument
		if(!(this instanceof nachos.vm.VMProcess))
		{
		    int point=numPages-stackPages-1;
	//	debug.print(Integer.toString(point));
		    LinkedList<node> stack=UserKernel.allocPhyMem(stackPages+1);
		    for(Iterator<node> it=stack.iterator();it.hasNext();)
		    {
		        node tempNode=it.next();
		        for(int i=0;i<tempNode.length;++i)
		        {
		            pageTable[point]=new TranslationEntry(point,tempNode.start+i,true,false,false,false);
		            debug.print(Integer.toString(point)+" "+Integer.toString(tempNode.start+i));
		            ++point;
		        }
		    }
		}
		
		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib
					.assertTrue(writeVirtualMemory(entryOffset,
							stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib
					.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib
					.assertTrue(writeVirtualMemory(stringOffset,
							new byte[] { 0 }) == 1);
			stringOffset += 1;
		}
		debug.print("----------------------------------load is over");
		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		//the number of pages must smaller than PhysPages
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if(section.getLength()==0)
				continue;
			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

//**********************mycode*********************	
			boolean isReadOnly=section.isReadOnly();
			LinkedList<node> proPhyMem=UserKernel.allocPhyMem(section.getLength());
			
			int vpn=section.getFirstVPN();
			int start=0;
			for(int i=0;i<proPhyMem.size();++i)
			{
				for(int j=proPhyMem.get(i).start;j<proPhyMem.get(i).start+proPhyMem.get(i).length;++j)
				{
					section.loadPage(start, j);
					pageTable[vpn]=new TranslationEntry(vpn,j,true,isReadOnly,true,false);
					debug.print(Integer.toString(vpn)+" "+Integer.toString(j));
					vpn+=1;
					start++;
				}
			}
			
		}
	//	debug.print("loadSection is Ok");
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	    LinkedList<node> tempList=new LinkedList<node>();
	    for(int i=0;i<pageTable.length;++i)
	    {
	        if(pageTable[i]!=null)
	        {
	            tempList.add(new node(pageTable[i].ppn,1));
	            pageTable[i]=null;
	        }
	    }
	    UserKernel.freePhyMem(tempList);
		return ;
		
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < Processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {
		if(processID==0)
		{	
			Machine.halt();		
			Lib.assertNotReached("Machine.halt() did not halt machine!");
			return 0;
		}
		else
		{
			debug.print("You are not the root Process!!!");
			return 0;
		}

	}

	protected static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			handleHalt();
			break;
//**************************************************************
		case syscallExit:
			handleExit(a0);
			break;
		case syscallExec:
			return handleExec(a0,a1,a2);
		case syscallJoin:
			return handleJoin(a0,a1);
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0,a1,a2);
		case syscallWrite:
			return handleWrite(a0,a1,a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
//**************************************************************
		default:
		    handleExit(-1);
			debug.print(syscall);
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	private int handleUnlink(int name) {
	    try{
	        String fileName=readVirtualMemoryString(name, MAS);
	        if(fileName==null||fileName.length()==0)
	        {
	            System.out.print("syscall Unlink name is null");
	            return -1;
	        }
	        boolean hasRemoved=ThreadedKernel.fileSystem.remove(fileName);
	        if(!hasRemoved)
	        {
	            debug.print("The file can't remove");
	            return -1;
	        }
	        Lib.debug(dbgSyscall, "UnLink:"+fileName);
	        return 0;
	        }
	        catch(Exception e)
	        {
	            debug.print("handleUnlink catch exception!");
	            return -1;
	        }
	}

	private int handleClose(int fd) {
	    try{
	        if(fd<0||fd>MFN)
	        {
	            debug.print("This fileDescriptor is out of bound!");
	            return -1;
	        }
		    OpenFile openFile=descriptortable[fd];
		    if(openFile==null)
		    {
		        debug.print("Syscall close :The file is not opened");
		        return -1;
		    }
		    openFile.close();
		    descriptortable[fd]=null;
		    Lib.debug(dbgSyscall, "Close:"+fd);
		    return 0;
	        }
	    catch(Exception e)
	    {
	        debug.print("handleClose catch exception");
	        return -1;
	    }
	}

	private int handleWrite(int fd, int buffer, int size) {
	    try{
	        if(fd<0||fd>MFN)
	        {
	            debug.print("handlewrite out of bound!");
	            return -1;
	        }
	        OpenFile fileWrite=descriptortable[fd];
	        if(fileWrite==null)
	        {
	            debug.print("The file to write is not open");
	            return -1;
	        }
	        byte buf[]=new byte[size];
	        int numReadFromBuf=readVirtualMemory(buffer, buf,0,size);
	       // debug.print(buffer);
	        if(numReadFromBuf<=0) 
	        {
	            debug.print("Write: no words able read from mem");
	            return -1;
	        }
	        int numWrite=fileWrite.write(buf, 0, numReadFromBuf);
	        Lib.debug(dbgSyscall,"Write:"+fd+" size-"+size+"actually write-"+numWrite+" mes-"+new String(buf));
	        return numWrite;
	       }
	   catch(Exception e)
	   {
	       debug.print("handleWrite cathche exception");
	       return -1;
	   }
	}

	private int handleRead(int fd, int buffer, int size) {
	    try{
	        if(fd<0||fd>MFN)
	        {
	           debug.print("Read: handleRead out of bound!");
	           return -1;
	        }
	        OpenFile fileRead=descriptortable[fd];
	        if(fileRead==null)
	        {
	            debug.print("Read:The File to read id null");
	            return -1;
	        }
	        byte[] buf=new byte[size]; 
	        int numRead=fileRead.read(buf, 0, size);
	        if(numRead==-1)
	        {
	            debug.print("Read: File read Nothing");
	            return -1;
	        }
	        int numWrite=writeVirtualMemory(buffer, buf, 0, numRead);
	      //  debug.print("buffer"+n);
	      //  Lib.debug(dbgSyscall,"Read:"+fd+" size-"+size+"actually read-"+numWrite+" mes-"+new String(buf));
	        return numWrite;
	       }
	    catch(Exception e)
	    {
	        debug.print("Handleread catch exception!");
	        return -1;
	    }
	}

	private int handleOpen(int name) {
	  try{
		int desc=assignDesc();
		if(desc==-1)
		{
			debug.print("Open:syscall more than MFN files is open");
			return -1;
		}	
		String fileName=readVirtualMemoryString(name, MAS);
		if(fileName==null||fileName.length()==0)
		{
			debug.print("Open:systemCall open has a null filename");
			return -1;
		}
		OpenFile file=ThreadedKernel.fileSystem.open(fileName, false);
		if(file==null)
		{
			debug.print("Open:This file open may be not exists!");
			return -1;
		}
		if(file.getFileSystem()==null)
		{
			debug.print("OPen:This file open isnot the file on disk");
		}
		descriptortable[desc]=file;
		Lib.debug(dbgSyscall, "Open:"+fileName+" desr:"+desc);
		return desc;
	  }
	  catch(Exception e)
	  {
	      debug.print("handleOpen open catch exception");
	      return -1;
	  }
	}

	private int handleCreate(int name) {
	  try{
		int desc=assignDesc();
		if(desc==-1)
		{
			debug.print("syscall more than MFN files is open");
			return -1;
		}	
		String fileName=readVirtualMemoryString(name, MAS);
	//    debug.print(fileName);
		if(fileName==null)
		{
			debug.print("systemCall create has a null filename");
			return -1;
		}
		OpenFile file=ThreadedKernel.fileSystem.open(fileName, true);
		if(file==null)
		{
			debug.print("This file may be not exists!");
			return -1;
		}
//		if(file.getFileSystem()==null)
//		{
//			debug.print("This file isnot the file on disk");
//			return -1;
//		}
	    Lib.debug(dbgSyscall, "Create:"+fileName+" desr:"+desc);
		descriptortable[desc]=file;
		return desc;
	  }
	  catch(Exception e)
	  {
	      debug.print("handleCreate catch exception");
	      return -1;
	  }
	}
	private int handleJoin(int pid, int status) {
	    joinLock.acquire();
	//    System.out.print("I am join to"+pid);
		UserProcess child=mapTo(pid);
		if(child==null) 
		{
		    debug.print("Join:There is no such process");
			joinLock.release();
			return -1;
		}
		if(!childPro.contains(child))
		{
			debug.print("Join:The process has the pid is not the child of me");
			joinLock.release();
			return -1;
		}
		if(child.hasJoined)
		{
		    debug.print("Join: This child has already joined");
		    joinLock.release();
		    return -1;
		}
        Integer childstatus = exitChild.get(pid);
        if (childstatus != null){
                int number = writeVirtualMemory(status, Lib.bytesFromInt(childstatus.intValue()));
                if (number!= 4){
                        joinLock.release();
                        debug.print("Join: write status is wrong!");
                        return -1;
                }
                else
                {
                    if(childstatus.equals(-1))
                    {
                        joinLock.release();
                        debug.print("Join: child exit abnormally early");
                        return 0;
                    }
                    else 
                    {
                        joinLock.release();
                        debug.print("Join:Child exited OK");
                        return 1; 
                    }
                }
                
        }
        child.hasJoined=true;
        childToJoin=pid;
        joinCondition.sleep();
        childstatus = exitChild.get(pid);
        joinLock.release();
        if (childstatus != null && childstatus.equals(-1)){
                debug.print("Join: child exeited abnormaly");
                writeVirtualMemory(status, Lib.bytesFromInt(childstatus.intValue()));
                return 0;
        }
        if(childstatus==null){
            debug.print("Join: childstatus is null");
            return -1;
        }
        else{
                debug.print("Join:Child exited OK");
                writeVirtualMemory(status, Lib.bytesFromInt(childstatus.intValue()));
                return 1;
        }
	}

	private int handleExec(int name, int argc, int argv) {
	  try{
	    if(name<0||argc<0||argv<0)
	    {
	        debug.print("handleExec:Your argument mabe wrong");
	        return -1;	       
	    }
		String fileName=readVirtualMemoryString(name, MAS);
		if(fileName==null||fileName.length()==0)
		{
		    debug.print("handleExec:Your fileName is null");
		    return -1;
		}
		String args[]=new String[argc];
		
        int currentVaddr = argv;
        for (int i = 0; i < argc; i++) {
                byte[] data = new byte[4];
                int numberOfBytesXferd = readVirtualMemory(currentVaddr, data);
                if (numberOfBytesXferd != data.length){
                        return -1;
                }
                int ptrArgv = Lib.bytesToInt(data, 0);        
                String argument = null;
                if (0 != ptrArgv) {
                        argument = readVirtualMemoryString(ptrArgv, MAS);
                       
                        if (argument == null){
                                return -1;
                        }
                }   
                args[i]=argument;
                currentVaddr += 4;
        }  
        UserProcess child=null;
        if(this instanceof nachos.vm.VMProcess)
            child=new nachos.vm.VMProcess();
        else if(this instanceof UserProcess)
            child=new UserProcess();

		childPro.addLast(child); 
		child.father=this;
		boolean exec=child.execute(fileName, args);
	//	if(child.exe!=null)
//		child.exe.close();
		if(exec)
		{
		    Lib.debug(dbgSyscall, "Exec:childId-"+child.processID);
		    return child.processID;
		}
		else
		{
		    debug.print("Exec: child may have something wrong");
		    child.handleClose(0);
		    child.handleClose(1);
		    exitChild.put(child.processID,-1);
		    aliveProcess--;
		    return -1;
		}
	  }
	  catch(Exception e)
	  {
	      debug.print("handleExec catch exception!");
	     // handleExit(-1);
	      return -1;
	  } 
	}
	protected int handleExit(int status) {
		for(int i=0;i<MFN;++i)
		{
			if(descriptortable[i]!=null)
			{	
				if(handleClose(i)<0)
				{
				    debug.print("can't close ,there maybe something wrong!");
				}
				descriptortable[i]=null;
			}
		}
		for(Iterator<UserProcess> it=childPro.iterator();it.hasNext();)
		  it.next().father=null;

		if(father!=null)
		{
		    father.exitChild.put(processID,status);
		      
		    father.joinLock.acquire();
		    if(father.childToJoin==processID)
		    {		       
		        father.joinCondition.wake();
		    }
		    father.joinLock.release();
		}
		unloadSections();
		processLock.acquire();
		aliveProcess--;
		if(aliveProcess==0) 
			Kernel.kernel.terminate();
		processLock.release();
		if(((UThread)UThread.currentThread()).process.exe!=null)
		    ((UThread)UThread.currentThread()).process.exe.close();
		UThread.finish();
		
		debug.print("I am OK");
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause
	 *            the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0), processor
							.readRegister(Processor.regA1), processor
							.readRegister(Processor.regA2), processor
							.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
		    
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			debug.print(((UThread)KThread.currentThread()).process.processID);
			handleExit(-1);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;
	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages =Config.getInteger("Processor.numStackPages", 8);

	private int initialPC, initialSP;
	private int argc, argv;

	private static final int pageSize = Processor.pageSize;
	private static final char dbgProcess = 'a';
	private static final char dbgSyscall='c';
	
	public static final int MAS=256;
	public static final int MFN=16;
	
	public static int ID=0;
	private int processID=0;
	
	private UserProcess father=null;
	protected LinkedList<UserProcess> childPro=new LinkedList<UserProcess>();
	
	public static HashMap<Integer,UserProcess> idToProcess=new HashMap<Integer,UserProcess>();
	public OpenFile[] descriptortable=new OpenFile[MFN];
	
	protected UThread thread;
	protected OpenFile exe=null;
	Lock joinLock=new Lock();
	Condition joinCondition=new Condition(joinLock);
	int childToJoin=-1;
	Lock processLock=new Lock();
	public static int aliveProcess=0;
	protected HashMap<Integer,Integer> exitChild=new HashMap<Integer,Integer>();
	public boolean hasJoined=false;
	public int getProcessID()
	{
		return processID;
	}
	public UserProcess mapTo(int id)
	{
		return idToProcess.get(id);
	}
	private int assignDesc()
	{
		for(int i=0;i<MFN;++i)
		{
			if(descriptortable[i]==null)
			{
				return i;
			}
		}
		debug.print("This process has already open Maxnum files");
		return -1;
	}
}
