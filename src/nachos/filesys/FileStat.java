package nachos.filesys;

import nachos.machine.Lib;

public class FileStat
{
  public static final int FILE_NAME_MAX_LEN = 256;
  public static final int NORMAL_FILE_TYPE = 0;
  public static final int DIR_FILE_TYPE = 1;
  public static final int LinkFileType = 2;
  
  public String name;
  public int size;
  public int sectors;
  public int type;
  public int inode;  //???????what's this
  public int links;
  public byte[] tobyte()
  {
      byte[] data=new byte[37+name.length()];
      int offset=0;
      Lib.bytesFromInt(data, offset, FILE_NAME_MAX_LEN);    offset+=4;
      Lib.bytesFromInt(data, offset, NORMAL_FILE_TYPE);     offset+=4;
      Lib.bytesFromInt(data, offset, DIR_FILE_TYPE);        offset+=4;
      Lib.bytesFromInt(data, offset, LinkFileType);         offset+=4;
      System.arraycopy(name.getBytes(),0, data, offset, name.length());
      offset+=name.length();
      data[offset++]=new Integer('\0').byteValue();
      Lib.bytesFromInt(data, offset, size);                 offset+=4;
      Lib.bytesFromInt(data, offset, sectors);              offset+=4;
      Lib.bytesFromInt(data, offset, type);                 offset+=4;
      Lib.bytesFromInt(data, offset, inode);                offset+=4;
      Lib.bytesFromInt(data, offset, links);                offset+=4;
      return data;
  }
}
