package nachos.filesys;

import java.util.LinkedList;

import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.userprog.debug;
import nachos.vm.VMProcess;

/**
 * FilesysProcess is used to handle syscall and exception through some callback methods.
 * 
 * @author starforever
 */
public class FilesysProcess extends VMProcess
{
  protected static final int SYSCALL_MKDIR = 14;
  protected static final int SYSCALL_RMDIR = 15;
  protected static final int SYSCALL_CHDIR = 16;
  protected static final int SYSCALL_GETCWD = 17;
  protected static final int SYSCALL_READDIR = 18;
  protected static final int SYSCALL_STAT = 19;
  protected static final int SYSCALL_LINK = 20;
  protected static final int SYSCALL_SYMLINK = 21;
  
  RealFileSystem myFS=FilesysKernel.realFileSystem;
  public static String getFather(String path)
  {
      String temp=null;
      if(path.endsWith("/")) 
          temp=path.substring(0,path.length()-1);
      else temp=path;
      if(temp.contains("/"))
      {
           temp=temp.substring(0,temp.lastIndexOf('/'));
           if(temp.length()==0)
               temp="/";
           return temp;
      }
      else
      return "#";
  }
  public static String getName(String path)
  {
      String temp=null;
      if(path.endsWith("/")) temp=path.substring(0,path.length()-1);
      else temp=path;
      return temp.substring(temp.lastIndexOf('/')+1,temp.length());
  }
  public int handleSyscall (int syscall, int a0, int a1, int a2, int a3)
  {
    switch (syscall)
    {
      case SYSCALL_MKDIR:
          return handleMkdir(a0);        
      case SYSCALL_RMDIR:
          return handleRmdir(a0);
      case SYSCALL_CHDIR:
          return handleChdir(a0);
      case SYSCALL_GETCWD:
          return handleGetcwd(a0,a1);
      case SYSCALL_READDIR:
          return handleReaddir(a0,a1,a2,a3);
      case SYSCALL_STAT:
          return handleStat(a0,a1);
      case SYSCALL_LINK:
          return handleLink(a0,a1);
      case SYSCALL_SYMLINK:
          return handleSymlink(a0,a1);
      default:
        return super.handleSyscall(syscall, a0, a1, a2, a3);
    }
  }
  private int handleMkdir(int Path) {
    String path=super.readVirtualMemoryString(Path, MAS);  
    boolean flag=myFS.createFolder(path);
    if(flag)
    {
    	debug.print("Mkdir:"+path);
    	return 0;
    }
    else
    {
    	debug.print("Mkdir:"+path+" wrong");
    	return -1;
    }
   
  }
  private int handleRmdir(int Path) {
    String path=super.readVirtualMemoryString(Path, MAS);
    boolean flag=myFS.removeFolder(path);
    if(flag)
    {
    	debug.print("RemoveFolder:"+path);
    	return 0;
    }
    else
    {
    	debug.print("RemoveFolder:"+path+" wrong");
    	return -1;
    }
         
  }
  private int handleChdir(int Path) {
      String path=super.readVirtualMemoryString(Path, MAS);
      boolean flag=myFS.changeCurFolder(path,true,new LinkedList<Folder>());
      if(flag)
      {
          debug.print("Chdir :"+myFS.getCwd());
          return 0;
      }
      else
      {
          debug.print("Chdir:"+path+"is not existed");
          return -1;
      }
  }
  private int handleGetcwd(int buf,int size) {
      String path=myFS.getCwd();
      path=path+"\0";
      byte[] buffer=path.getBytes();
      super.writeVirtualMemory(buf, buffer, 0, buffer.length);
      return 1;
          
  }
  private int handleReaddir(int Path,int buf,int size,int namesize) {
      String path=super.readVirtualMemoryString(Path, MAS);
      String[] names=myFS.readDir(path);
      if(names.length>size)
          return -1;
      byte[] data= new byte[size*namesize];
      int length=0;
      for(int i=0;i<names.length;++i)
      {
          if(names[i].length()+1>namesize)
              return -1;
          System.arraycopy((names[i]+"\0").getBytes(), 0, data, length, (names[i]+"\0").length());
          length+=(names[i]+"\0").length();
      }
      super.writeVirtualMemory(buf, data, 0, length);
      return 1;
  }
  private int handleStat(int Path,int stat) {
     String path=super.readVirtualMemoryString(Path, MAS);
     FileStat tempStat=myFS.getStat(path);
     byte[] data=tempStat.tobyte();
     int temp=super.writeVirtualMemory(stat, data, 0, data.length);
     return temp;
     
  }
  private int handleLink(int oldAddr,int newAddr) {
      String oldname=super.readVirtualMemoryString(oldAddr, MAS);
      String newname=super.readVirtualMemoryString(newAddr, MAS);
      boolean flag=myFS.createLink(oldname, newname);
      if(flag)
      {
          debug.print("Link from:"+oldname+" To:"+newname);
          return 0;
      }
      else
      {
    	  debug.print("Link Wrong");
          return -1;
      }
  }
  private int handleSymlink(int oldAddr,int newAddr) {
      String oldname=super.readVirtualMemoryString(oldAddr, MAS);
      String newname=super.readVirtualMemoryString(newAddr, MAS);
      boolean flag=myFS.createSymlink(oldname, newname);
      if(flag)
      {
    	  debug.print("SymLink from:"+oldname+" To:"+newname);
    	  return 0;
      }
      else
      {
    	  debug.print("SymLink wrong");
    	  return -1;
      }
  }

public void handleException (int cause)
  {
    if (cause == Processor.exceptionSyscall)
    {
        Processor processor = Machine.processor();
        int result = handleSyscall(processor.readRegister(Processor.regV0),
                processor.readRegister(Processor.regA0), processor
                        .readRegister(Processor.regA1), processor
                        .readRegister(Processor.regA2), processor
                        .readRegister(Processor.regA3));
        processor.writeRegister(Processor.regV0, result);
        processor.advancePC();
    }
    else
      super.handleException(cause);
  }
}
