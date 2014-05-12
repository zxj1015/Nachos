package nachos.filesys;

/** 
 * FolderEntry contains information used by Folder to map from filename to address of the file
 * 
 * @author starforever
 * */
class FolderEntry
{
  /** the file name */
  String name;
  
  /** the sector number of the inode */
  int addr;
  FolderEntry(String name,int addr)
  {
      this.name=name;
      this.addr=addr;
  }
  String getName()
      { return name;    }
  int getAddr()
      {    return addr;    }
  void setName(String name) 
      {    this.name=name; }
  void setAddr(int addr) 
      {   this.addr=addr; }
}
