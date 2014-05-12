package nachos.filesys;

import java.util.LinkedList;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.userprog.debug;

/**
 * INode contains detail information about a file.
 * Most important among these is the list of sector numbers the file occupied, 
 * it's necessary to find all the pieces of the file in the filesystem.
 * 
 * @author starforever
 */
public class INode
{
  /** represent a system file (free list) */
  public static int TYPE_SYSTEM = 0;
  
  /** represent a folder */
  public static int TYPE_FOLDER = 1;
  
  /** represent a normal file */
  public static int TYPE_FILE = 2;
  
  /** represent a normal file that is marked as delete */
  public static int TYPE_FILE_DEL = 3;
  
  /** represent a symbolic link file */
  public static int TYPE_SYMLINK = 4;
  
  /** represent a folder that are not valid */
  public static int TYPE_FOLDER_DEL = 5;
  
  /** the reserve size (in byte) in the first sector */
  private static final int FIRST_SEC_RESERVE = 16;
  
  /** size of the file in bytes */
  int file_size;
  
  /** the type of the file */
  int file_type;
  
  /** the number of programs that have access on the file */
  int use_count;
  
  /** the number of links on the file */
  int link_count;
  
  /** maintain all the sector numbers this file used in order */
  private LinkedList<Integer> sec_addr;
  
  /** the first address */
  private int addr;
   
  public boolean isactive;
  
  /** the extended address ,my opinion is that if inode is bigger the one sector*/
  private LinkedList<Integer> addr_ext;
    
  public INode (int addr)
  {
    file_size = 0;
    file_type = TYPE_FILE;
    use_count = 0;
    link_count = 0;
    sec_addr = new LinkedList<Integer>();
    this.addr = addr;
    addr_ext = new LinkedList<Integer>();   
    isactive=false;
  }
  public int getAddr()
  {
      return addr;
  }
  /** get the sector number of a position in the file  */
  public int getSector (int pos)
  {
    if(pos<0||pos>this.sec_addr.size())
    {
        debug.print("Getsector:this sector is not existed");
        return -1;
    }
    else
    {
        int num=pos/Disk.SectorSize;
        int sector=sec_addr.get(num);
        debug.print("GetSetcor: pos-"+pos+"sector-"+sector);
        return sector;
    }
  }
  public int getPhyNum(int num)
  {
      if(num<0||num>sec_addr.size())
          return -1;
      return sec_addr.get(num);
  }
  /** change the file size and adjust the content in the inode accordingly */
  public void setFileSize (int size)
  {
      int sectornum=Lib.divRoundUp(size,Disk.SectorSize);
      if(sectornum==sec_addr.size())
      {
          this.file_size=size;
          return ;
      }
      else if(sectornum<sec_addr.size())
      {
          int temp=sec_addr.size()-sectornum;
          for(int i=0;i<temp;++i)
              sec_addr.removeLast();
      }
      else
      {
          int temp=sectornum-sec_addr.size();
          for(int i=0;i<temp;++i)
              sec_addr.addLast(FilesysKernel.realFileSystem.getFreeList().allocate());
      }
      debug.print("setFilesize:"+size);
      this.file_size=size;
      return ;

  }
  
  /** free the disk space occupied by the file (including inode) */
  public void free ()
  {
    for(int i=0;i<sec_addr.size();i++)
    {
    	 FilesysKernel.realFileSystem.getFreeList().deallocate(sec_addr.get(i));
    }
    FilesysKernel.realFileSystem.getFreeList().deallocate(this.addr/Disk.SectorSize);
    if(addr_ext.size()!=0)
        for(int i=0;i<addr_ext.size();++i)
        	 FilesysKernel.realFileSystem.getFreeList().deallocate(addr_ext.get(i));
  }
  public int getSectorNum()
  {
      return sec_addr.size()+1+addr_ext.size();
  }
  /** load inode content from the disk */
  public void load ()
  {
	  sec_addr.clear();
	  addr_ext.clear();
      byte[] data=new byte[Disk.SectorSize];
      byte[] totalData=null;
      Machine.synchDisk().readSector(addr/Disk.SectorSize, data, 0);
      int offset=0;
      int extNum=Lib.bytesToInt(data, offset); offset+=4;
 
      for(int i=0;i<extNum;++i)
      {
          addr_ext.addLast(Lib.bytesToInt(data, offset));
          offset+=4;
      }
      if(extNum!=0)
      {
          totalData=new byte[(extNum+1)*Disk.SectorSize];
          System.arraycopy(data, 0, totalData, 0, Disk.SectorSize);
          for(int i=0;i<extNum;++i)
          {
              Machine.synchDisk().readSector(addr_ext.get(i), totalData,(i+1)*Disk.SectorSize);            
          }
      }
      else totalData=data;
      int secNum=Lib.bytesToInt(totalData, offset); offset+=4;
      for(int i=0;i<secNum;i++)
      {
          sec_addr.addLast(Lib.bytesToInt(totalData, offset));
          offset+=4;
      }
      file_size=Lib.bytesToInt(totalData, offset); offset+=4;
      file_type=Lib.bytesToInt(totalData, offset); offset+=4;
      use_count=Lib.bytesToInt(totalData, offset); offset+=4;
      link_count=Lib.bytesToInt(totalData, offset); offset+=4;
      isactive=true;
  }
  
  /** save inode content to the disk */
  public void save ()
  {
    int mysize=(4+1+sec_addr.size()+1+1+addr_ext.size())*4;
    while(mysize>(1+addr_ext.size())*Disk.SectorSize)
    {
        addr_ext.addLast(FilesysKernel.realFileSystem.getFreeList().allocate());
        mysize+=4;
    }
    byte[] data=new byte[Lib.divRoundUp(mysize,Disk.SectorSize)*Disk.SectorSize];
    int offset=0;
    Lib.bytesFromInt(data, offset, addr_ext.size()); offset+=4;
    for(int i=0;i<addr_ext.size();++i)
    {
        Lib.bytesFromInt(data, offset, addr_ext.get(i));
        offset+=4;
    }
    
    Lib.bytesFromInt(data, offset, sec_addr.size()); offset+=4;
    for(int i=0;i<sec_addr.size();++i)
    {
        Lib.bytesFromInt(data, offset, sec_addr.get(i));
        offset+=4;
    }
    Lib.bytesFromInt(data,offset,file_size); offset+=4;
    Lib.bytesFromInt(data,offset,file_type); offset+=4;
    Lib.bytesFromInt(data,offset,use_count); offset+=4;
    Lib.bytesFromInt(data,offset,link_count); offset+=4;
    Lib.bytesFromInt(data,offset,addr); offset+=4;
    
    System.out.println(link_count);
    Machine.synchDisk().writeSector(addr/Disk.SectorSize, data,0);
    
    for(int i=0;i<addr_ext.size();++i)
        Machine.synchDisk().writeSector(addr_ext.get(i),data,(i+1)*Disk.SectorSize);
    isactive=false;
  }
}
