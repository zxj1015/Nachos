package nachos.filesys;

import java.util.Hashtable;
import java.util.Set;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.userprog.debug;

/**
 * Folder is a special type of file used to implement hierarchical filesystem.
 * It maintains a map from filename to the address of the file.
 * There's a special folder called root folder with pre-defined address.
 * It's the origin from where you traverse the entire filesystem.
 * 
 * @author starforever
 */
public class Folder extends File
{
  /** the static address for root folder */
  public static int STATIC_ADDR = 1;
  
  private int size;
  
  /** mapping from filename to folder entry */
  private Hashtable<String, FolderEntry> entry;
  
  
  public Folder (INode inode)
  {
    super(inode);
    /*size = 4; origin*/
    //mycode
    size=0;
    entry = new Hashtable<String, FolderEntry>();
  }
  
  /** open a file in the folder and return its address */
  public int open (String filename)
  {
    FolderEntry fd=entry.get(filename);
    if(fd==null)
    {
        debug.print("open:the file to open is null");
        return -1;
    }
    return fd.addr;
  }
  
  /** create a new file in the folder and return its address */
  public int create (String filename)
  {
    FolderEntry fd=entry.get(filename);
    if(fd!=null)
    {
        debug.print("This file is already in this folder:"+filename);
        return -1;
    }
    int inodeSector=FilesysKernel.realFileSystem.getFreeList().allocate();
    if(inodeSector==-1)
    {
        debug.print("There is no enough space on the disk");
        return -1;
    }
    int addr=inodeSector*Disk.SectorSize;
    addEntry(filename, addr);
    return addr;
  }
  
  /** add an entry with specific filename and address to the folder */
  public void addEntry (String filename, int addr)
  {
    FolderEntry fd=new FolderEntry(filename,addr);
    entry.put(filename, fd);
    size++;
  }
  /** get fileName's addr*/
  public int getEntry(String fileName)
  {
      FolderEntry fd=entry.get(fileName);
      if(fd==null)    
      return -1;
      return fd.addr;      
  }
  /** remove an entry from the folder */
  public void removeEntry (String filename)
  {
      FolderEntry fd=entry.remove(filename);
      if(fd==null)
          debug.print("This filename is null");
      else
          size--;
      
  }
  public Set<String> getKeySet()
  {
      return entry.keySet();
  }
  /** save the content of the folder to the disk */
  public void save ()
  {
      int length=0;
      length+=8;
      Set<String> names=entry.keySet();
      for(String name:names)
      {
          length+=4;
          length+=name.length();
          length+=4;
      }
      byte[] data=new byte[length];
      int offset=0;
      Lib.bytesFromInt(data,offset,size); offset+=4;
      
      Lib.bytesFromInt(data, offset,names.size()); offset+=4;
      for(String name:names)
      {
          Lib.bytesFromInt(data,offset,name.length()); offset+=4;
          System.arraycopy(name.getBytes(), 0, data, offset, name.length());
          offset+=name.length();
          int addr=entry.get(name).addr;
          Lib.bytesFromInt(data,offset,addr); offset+=4;
      }
 //     System.out.println(inode.getAddr());
      super.write(0, data,0, offset);
      inode.file_size=offset;
      inode.save();
  }

  /** load the content of the folder from the disk */
  public void load ()
  { 
	  	  inode.load();
	  	  entry.clear();
          byte data[]=new byte[inode.file_size];  
      //    System.out.println(inode.file_size);
          super.read(0, data, 0, inode.file_size);
          int offset=0;
          size=Lib.bytesToInt(data, offset); offset+=4;
          int nameNum=Lib.bytesToInt(data, offset); offset+=4;
          for(int i=0;i<nameNum;++i)
          {
              int stringLength=Lib.bytesToInt(data, offset); offset+=4;
              char[] tempc=new char[stringLength];
              for(int j=0;j<stringLength;++j)
              {
                  tempc[j]=(char)data[offset++];
              }
              String temps=new String(tempc);
              int tempAddr=Lib.bytesToInt(data, offset); offset+=4;
              addEntry(temps,tempAddr);
          }
                        
  }
}
