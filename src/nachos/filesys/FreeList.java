package nachos.filesys;

import java.util.LinkedList;
import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.userprog.debug;

/**
 * FreeList is a single special file used to manage free space of the filesystem.
 * It maintains a list of sector numbers to indicate those that are available to use.
 * When there's a need to allocate a new sector in the filesystem, call allocate().
 * And you should call deallocate() to free space at a appropriate time (eg. when a file is deleted) for reuse in the future.
 * 
 * @author starforever
 */
public class FreeList extends File
{
  /** the static address */
  public static int STATIC_ADDR = 0;
  
  /** size occupied in the disk (bitmap) */ //I don't know what I can do with you???
  static int size = Lib.divRoundUp(Disk.NumSectors, 8);
  
  /** maintain address of all the free sectors */
  private LinkedList<Integer> free_list;
  
  public boolean isactive;
  
  public FreeList (INode inode)
  {
    super(inode);
    free_list = new LinkedList<Integer>();
    isactive=false;
  }
  public int getSize()
  {
      return free_list.size();
  }
  
  public void init ()
  {
    for (int i = 2; i < Disk.NumSectors; ++i)
      free_list.add(i);
  }
  
  /** allocate a new sector in the disk */
  public int allocate ()
  {
    
    if(free_list.size()==0)
    {
        debug.print("acllocate: there is no empty space!");
        return -1;
    }
    int x=free_list.removeFirst();
  //  debug.print("***********allocate :"+x+" "+free_list.size());
    return x;
  }
  
  /** deallocate a sector to be reused */
  public void deallocate (int sec)
  {
      free_list.add(sec);
    //  System.out.println("***********dellocate"+sec+" "+free_list.size());
  }
  
  /** save the content of freelist to the disk */
  public void save ()
  {
    byte[] data=new byte[(1+free_list.size())*4];
    int offset=0;
    Lib.bytesFromInt(data,offset,free_list.size());  offset+=4;
    for(int i=0;i<free_list.size();++i)
    {
        Lib.bytesFromInt(data, offset,free_list.get(i)); 
        offset+=4;
    }
    
    super.write(0, data, 0, data.length);
    
    isactive=false;
  }
  
  /** load the content of freelist from the disk */
  public void load ()
  {
    byte[] data=new byte[Disk.SectorSize];
    Machine.synchDisk().readSector(0, data,0);
    int offset=0;
    int size=Lib.bytesToInt(data, offset);   offset+=4;
    for(int i=0;i<size;++i)
    {
        free_list.addLast(Lib.bytesToInt(data, offset));
        offset+=4;
    }
    isactive=true;
  }
}
