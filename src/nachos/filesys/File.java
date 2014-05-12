package nachos.filesys;

import nachos.machine.Disk;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.userprog.debug;

/**
 * File provide some basic IO operations.
 * Each File is associated with an INode which stores the basic information for the file.
 * 
 * @author star forever
 */
public class File extends OpenFile
{
  INode inode;
  
  private int pos;
  
  public boolean isactive=false;
  
  public int getsector()
  { 
      return inode.getSectorNum();
  }
  public File (INode inode)
  {
    this.inode = inode;
    pos = 0;
    isactive=false;
  }
  public int length ()
  {
    return inode.file_size;
  }
  
  public void close ()
  {
    pos=0;
    inode.use_count--;
    inode.save();
  }
  
  public void seek (int pos)
  {
    this.pos = pos;
  }
  
  public int tell ()
  {
    return pos;
  }
  
  public int read (byte[] buffer, int start, int limit)
  {
    int ret = read(pos, buffer, start, limit);
    pos += ret;
    return ret;
  }
  
  public int write (byte[] buffer, int start, int limit)
  {
    int ret = write(pos, buffer, start, limit);
    pos += ret;
    return ret;
  }
  //to be consistent with openfile ,I think limit is length
  public int read (int pos, byte[] buffer, int start, int limit)
  {
    int firstSector=0;
    int lastSector=0;
    int numSectors=0;
    int numBytes=limit;
    if(numBytes<=0||pos>inode.file_size) 
    {
        debug.print("read: The bytes to read is zero");
        return 0;
    }
    if(pos+numBytes>inode.file_size)
        numBytes=inode.file_size-pos;
    firstSector=divRoundDown(pos,Disk.SectorSize);
    lastSector=divRoundDown(pos+numBytes, Disk.SectorSize);
    numSectors=1+lastSector-firstSector;
    byte[] data=new byte[numSectors*Disk.SectorSize];
    int index=0;
    for(int i=firstSector;i<=lastSector;++i)
    {
        Machine.synchDisk().readSector(inode.getPhyNum(i), data, index);
        index+=Disk.SectorSize;
    }
    System.arraycopy(data,pos%Disk.SectorSize,buffer,start, numBytes);
    return numBytes;
  }
  private int divRoundDown(int num1,int num2)
  {
      Lib.assertTrue(num1>=0&&num2>0);
      int temp=num1/num2;
      if(num1>0&&num1%num2==0)
          temp-=1;
      return temp;
  }
  //also I think there limit is length;
  public int write (int pos, byte[] buffer, int start, int limit)
  {
    int firstSector=0;
    int lastSector=0;
    int numSectors=0;
    int numBytes=limit;
    if(numBytes<=0)
        return 0;
    if(numBytes+pos>inode.file_size)
        inode.setFileSize(numBytes+pos);
    
    firstSector=divRoundDown(pos, Disk.SectorSize);
    lastSector=divRoundDown(pos+numBytes, Disk.SectorSize);
    numSectors=lastSector-firstSector+1;
    byte[] data=new byte[numSectors*Disk.SectorSize];
    if(pos%Disk.SectorSize!=0)
        read(inode.getPhyNum(firstSector)*Disk.SectorSize,data,0,Disk.SectorSize);
    if((pos+numBytes)%Disk.SectorSize!=0)
        read(inode.getPhyNum(lastSector)*Disk.SectorSize,data,lastSector*Disk.SectorSize,Disk.SectorSize);
    System.arraycopy(buffer, start, data, pos%Disk.SectorSize, numBytes);
    int index=0;
    for(int i=firstSector;i<=lastSector;i++)
    {
        Machine.synchDisk().writeSector(inode.getPhyNum(i), data, index);
        index+=Disk.SectorSize;
    }
    return numBytes;
  }
}
