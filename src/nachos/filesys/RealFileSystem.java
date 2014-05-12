package nachos.filesys;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import nachos.machine.Disk;
import nachos.machine.FileSystem;
import nachos.machine.Kernel;
import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.threads.ThreadedKernel;
import nachos.userprog.debug;
import nachos.vm.VMKernel;

/**
 * RealFileSystem provide necessary methods for filesystem syscall.
 * The FileSystem interface already define two basic methods, you should implement your own to adapt to your task.
 * 
 * @author starforever
 */
public class RealFileSystem implements FileSystem
{
  /** the free list */
  private FreeList free_list;
  
  /** the root folder */
  private Folder root_folder;
  
  /** the current folder */
  private Folder cur_folder;
  
  /** the string representation of the current folder */
//  private LinkedList<String> cur_path = new LinkedList<String>();
  public LinkedList<Integer> cur_path=new LinkedList<Integer>();
  /**
   * initialize the file system
   * 
   * @param format
   *          whether to format the file system
   */
  public void init (boolean format)
  {
    debug.print("...inilizing the filesystem");
    if (format)
    {     
      INode inode_free_list=new INode(FreeList.STATIC_ADDR);
      inode_free_list.isactive=true;
      free_list=new FreeList(inode_free_list);
      free_list.init();
      free_list.isactive=true;
      INode inode_root_folder = new INode(Folder.STATIC_ADDR*Disk.SectorSize);
      inode_root_folder.isactive=true;
      root_folder=new Folder(inode_root_folder);
      root_folder.isactive=true;
      cur_folder=root_folder;
      inode_root_folder.setFileSize(512*4);
    }
    else
    {
      INode inode_free_list = new INode(FreeList.STATIC_ADDR);
      inode_free_list.load();
      free_list = new FreeList(inode_free_list);
      free_list.load();
      
      INode inode_root_folder = new INode(Folder.STATIC_ADDR);
      inode_root_folder.load();
      root_folder = new Folder(inode_root_folder);
      root_folder.load();
      cur_folder=root_folder;
    }
    cur_path.addLast(Folder.STATIC_ADDR*Disk.SectorSize);
    importStub();
  }
  
  public void finish ()
  {
    root_folder.save();
    free_list.save();
  }
  /** import from stub filesystem */
  private void importStub ()
  {
    FileSystem stubFS = Machine.stubFileSystem();
    FileSystem realFS = FilesysKernel.realFileSystem;
    String[] file_list = Machine.stubFileList();
    for (int i = 0; i < file_list.length; ++i)
    {
      if (!file_list[i].endsWith(".coff"))
        continue;
      System.out.println(file_list[i]);
      OpenFile src = stubFS.open(file_list[i], false);
      if (src == null)
      {
        continue;
      }
      OpenFile dst = realFS.open(file_list[i], true);
      int size = src.length();
      byte[] buffer = new byte[size];
      src.read(0, buffer, 0, size);
      dst.write(0, buffer, 0, size);
      src.close();
      dst.close();
    }
 //   System.out.println("nihaoa");
  }
  
  String cwd="/";
  String getCwd()
  {
      return  cwd;
  }
  
  /** get the only free list of the file system */
  public FreeList getFreeList ()
  {
    return free_list;
  }
  
  /** get the only root folder of the file system */
  public Folder getRootFolder ()
  {
    return root_folder;
  }
  //relative path
  public OpenFile open (String name, boolean create) //absolute path
  {
    cur_folder.load();
    String foldername=FilesysProcess.getFather(name);
    name=FilesysProcess.getName(name);
    LinkedList<Folder> myFolderList=new LinkedList<Folder>();
    Folder myFolder=null;
    if(!foldername.equals("#"))
    {
        changeCurFolder(foldername,false,myFolderList);
        myFolder=myFolderList.poll();
    }
    else
    {
    	myFolder=cur_folder;
    }   	    
    if(myFolder==null)
    {
    	System.out.print("Open: the Path is wrong");
    	return null;
    }
    int addr=myFolder.open(name);
    boolean hasCreated=false;
    if(addr<0)
    {
        if(create)   
        {
            addr=myFolder.create(name);
            if(addr<0)
            {
                debug.print("Open :there is no space"); 
                return null;
            }
            hasCreated=true;
        }
        else
        {
            debug.print("Open:The file is not existed,cant't open");
            return null;
        }
    }
    INode fileNode=new INode(addr);
    if(!hasCreated)
        fileNode.load();
    else 
        fileNode.isactive=true;
    File tempFile=new File(fileNode);
    fileNode.use_count++;
    if(hasCreated)
        debug.print("Create a new File:"+name);
    else
        debug.print("Open the File: "+name);
    return tempFile;
  }
  
  public boolean remove (String name)
  {
    cur_folder.load();
    String foldername=FilesysProcess.getFather(name);
    name=FilesysProcess.getName(name);
    LinkedList<Folder> myFolderList=new LinkedList<Folder>();
    Folder myFolder=null;
    if(!foldername.equals("#"))
    {
        changeCurFolder(foldername,false,myFolderList);
        myFolder=myFolderList.poll();
    }
    else
    {
    	myFolder=cur_folder;
    }   	    
    if(myFolder==null)
    {
    	System.out.print("Remove: the Path is wrong");
    	return false;
    }
    
    int addr=cur_folder.getEntry(name);
    if(addr==-1) 
    {
        debug.print("There is no such file in current folder");
        return false;
    }
    myFolder.removeEntry(name);
    myFolder.save();
    INode inode=new INode(addr);
    inode.load();
    if(inode.link_count==0)
    {
        inode.free();
        inode.file_type=INode.TYPE_FILE_DEL;
        inode.save();
    }
    debug.print("Remove a file:"+name);
    return true;

  }
  
  public boolean createFolder (String name)
  {
    cur_folder.load();
    String foldername=FilesysProcess.getFather(name);
    name=FilesysProcess.getName(name);
    LinkedList<Folder> myFolderList=new LinkedList<Folder>();
    Folder myFolder=null;
    if(!foldername.equals("#"))
    {
        changeCurFolder(foldername,false,myFolderList);
        myFolder=myFolderList.poll();
    }
    else
    {
    	myFolder=cur_folder;
    }   	    
    if(myFolder==null)
    {
    	System.out.print("Remove: the Path is wrong");
    	return false;
    }
    
    int addr=myFolder.create(name);
    if(addr<0) 
        return false;
    INode inode=new INode(addr);
    inode.file_type=INode.TYPE_FOLDER;
    Folder folder=new Folder(inode);
    folder.save();
    myFolder.save();
    System.out.println("Createfolder:"+name);
    return true;
  }
  public boolean removeFolder (String name)
  {
	   cur_folder.load();
	   String foldername=FilesysProcess.getFather(name);
	   name=FilesysProcess.getName(name);
	   LinkedList<Folder> myFolderList=new LinkedList<Folder>();
	   Folder myFolder=null;
	   if(!foldername.equals("#"))
	   {
	        changeCurFolder(foldername,false,myFolderList);
	        myFolder=myFolderList.poll();
	   }
	   else
	   {
	   	myFolder=cur_folder;
	   }   	    
	   if(myFolder==null)
	   {
	    	System.out.print("Remove: the Path is wrong");
	    	return false;
	   }
	    
      int addr=myFolder.getEntry(name);
      if(addr==-1) 
      {
          debug.print("There is no such file in current folder");
          return false;
      }
      INode inode=new INode(addr);
      inode.load();
      if(inode.file_type!=INode.TYPE_FOLDER)
      {
          debug.print("Remover folder: this file is not a folder");
          return false;
      }
      Folder tempFolder=new Folder(inode);
      tempFolder.load();
      if(!tempFolder.getKeySet().isEmpty())
      {
    	  debug.print("RemoveFolder: Folder is notEmpty");
    	  return false;
      }
      myFolder.removeEntry(name);
      if(inode.link_count==0)
      inode.free();
      inode.file_type=INode.TYPE_FOLDER_DEL;
      inode.save();
      myFolder.save();
      return true;
  }
  
  public boolean changeCurFolder (String name,boolean flag,LinkedList<Folder> myFolderList)
  {
	Folder myFolder=null;	  
	cur_folder.load();
    if(name.equals("/"))
    {
            root_folder.load();
            if(flag)
            	cur_folder=root_folder;
            else
            {
            	myFolder=root_folder;
            	myFolderList.add(myFolder);
            }            	
            return true;
    }
    if(name.endsWith("/"))
        name=name.substring(0,name.length()-1);
    if(name.startsWith("/"))
        return changeAbs(name,flag,myFolderList);
    else 
        return changeRel(name,flag,myFolderList);
  }
  private boolean changeAbs(String name,boolean flag,LinkedList<Folder> myFolderList) {
	Folder myFolder=null;
	root_folder.load();
    myFolder=root_folder;

    LinkedList<Integer> tempPathList=new LinkedList<Integer>();
    tempPathList.addLast(Folder.STATIC_ADDR*Disk.SectorSize);
    String[] paths=name.split("/");
    for(int i=1;i<paths.length;++i)
    {
        int addr=myFolder.getEntry(paths[i]);
        if(addr<0)
        {            
     //       debug.print("+++++++++++++++++++++++++"+cur_folder.inode.getAddr()+" "+paths[i]);
            debug.print("ChangeAbs:The path is wrong");
            return false;
        }
        INode inode=new INode(addr);
        inode.load();
        if(inode.file_type!=INode.TYPE_FOLDER)
        {       
            debug.print("ChangeAbs:The path is wrong filetype");
            return false;
        }
        myFolder=new Folder(inode);
        myFolder.load();
        tempPathList.addLast(addr);
    }
    if(flag)   	
    {
    	cwd=name;
    	cur_path=tempPathList;
    	myFolderList.add(myFolder);
    }
    return true;
  }

private boolean changeRel(String name,boolean flag,LinkedList<Folder> myFolderList) {
    cur_folder.load();
	String[] paths=name.split("/");
    LinkedList<Integer> tempList=new LinkedList<Integer>();
    Folder myFolder=cur_folder;
    String myCwd=new String(cwd);
    
    for(int i=0;i<cur_path.size();++i)
    {
    	tempList.addLast(cur_path.get(i));
    }
    for(int i=0;i<paths.length;++i)
    {
        if(paths[i].equals("."))
            continue;
        else if(paths[i].endsWith(".."))
        {
            if(myCwd.equals("/"))
                continue;
            tempList.removeLast();
            int tempaddr=tempList.removeLast();
            INode tempNode=new INode(tempaddr);
            tempNode.load();
            myFolder=new Folder(tempNode);
            myFolder.load();
            if(myCwd.lastIndexOf('/')>0)
                myCwd=myCwd.substring(0,myCwd.lastIndexOf('/'));
            else
                myCwd="/";
        }
        else
        {
          int addr=myFolder.getEntry(paths[i]);
        //  System.out.println("cwd:"+paths[i]);
          if(addr<0) 
          {
        //      System.out.println(paths[i]);
              debug.print("Changrel:The path is wrong");
              return false;
          }
          INode inode=new INode(addr);
          inode.load();
          if(inode.file_type!=INode.TYPE_FOLDER)
          {
        	  System.out.println("Change rel path is wrong");
              return false;
          }
          myFolder=new Folder(inode);
          myFolder.load();
          tempList.addLast(addr);
          if(myCwd.equals("/")) myCwd=myCwd.concat(paths[i]);
          else
          myCwd=myCwd.concat("/"+paths[i]);
        }
    }
    if(flag)
    {
    	cwd=myCwd;
    	cur_path=tempList;
    	cur_folder=myFolder;
    }
    else
    	myFolderList.add(myFolder);
    return true;
}

  public String[] readDir (String name)
  {
	cur_folder.load();
	String foldername=FilesysProcess.getFather(name);
	name=FilesysProcess.getName(name);
	LinkedList<Folder> myFolderList=new LinkedList<Folder>();
	Folder myFolder=null;
	if(!foldername.equals("#"))
	{
	        changeCurFolder(foldername,false,myFolderList);
	        myFolder=myFolderList.poll();
	}
	else
	{
	    	myFolder=cur_folder;
	}   	    
	if(myFolder==null)
	{
	    	System.out.print("Remove: the Path is wrong");
	    	return null;
	}
	
    Set<String> names=myFolder.getKeySet();
    String[] temp=new String[names.size()];
    int pointer=0;
    for(String i:names)
    {
        temp[pointer++]=i;
    }
    return temp;
  }
  
  public FileStat getStat (String name)
  {
    FileStat tempState=new FileStat();
	cur_folder.load();
	String foldername=FilesysProcess.getFather(name);
	name=FilesysProcess.getName(name);
	LinkedList<Folder> myFolderList=new LinkedList<Folder>();
	Folder myFolder=null;
	if(!foldername.equals("#"))
	{
	        changeCurFolder(foldername,false,myFolderList);
	        myFolder=myFolderList.poll();
	}
	else
	{
	    	myFolder=cur_folder;
	}   	    
	if(myFolder==null)
	{
	    	System.out.print("Remove: the Path is wrong");
	    	return null;
	}
    int addr=myFolder.getEntry(name);
    if(addr<0)
    {
        debug.print("GetState:This file is not existed:"+name);
        return null;
    }
    INode inode=new INode(addr);
    inode.load();
    debug.print(inode.file_size);
    tempState.name=name;
    tempState.size=inode.file_size;
    tempState.sectors=inode.getSectorNum();
    tempState.type=inode.file_type;
    tempState.links=inode.link_count;
    tempState.inode=inode.getAddr();
    return tempState;
  }   
  
  public boolean createLink (String src, String dst)
  {
	cur_folder.load();
    String srcFolder=FilesysProcess.getFather(src);
    String srcFile=FilesysProcess.getName(src);
	LinkedList<Folder> mySrcFolderList=new LinkedList<Folder>();
	Folder mySrcFolder=null;
	if(!srcFolder.equals("#"))
	{
        changeCurFolder(srcFolder,false,mySrcFolderList);
        mySrcFolder=mySrcFolderList.poll();
	}
	else
	{
		mySrcFolder=cur_folder;
	}   	  
	if(mySrcFolder==null)
	{
		debug.print("CreateLink: folder is wrong");
		return false;
	}
	
    String dstFolder=FilesysProcess.getFather(dst);
    String dstFile=FilesysProcess.getName(dst);
    LinkedList<Folder> myDstFolderList=new LinkedList<Folder>();
    Folder myDstFolder=null;
    if(!dstFolder.equals("#"))
    {
    	changeCurFolder(dstFolder,false,myDstFolderList);
    	myDstFolder=myDstFolderList.poll();
    }
    else
    {
    	myDstFolder=cur_folder;
    }
    if(myDstFolder==null)
    {
    	debug.print("CreateLink:Dst folder wrong");
    	return false;
    }

    return true;
  }
  
  public boolean createSymlink (String src, String dst)
  {
		cur_folder.load();
	    String srcFolder=FilesysProcess.getFather(src);
	    String srcFile=FilesysProcess.getName(src);
		LinkedList<Folder> mySrcFolderList=new LinkedList<Folder>();
		Folder mySrcFolder=null;
		if(!srcFolder.equals("#"))
		{
	        changeCurFolder(srcFolder,false,mySrcFolderList);
	        mySrcFolder=mySrcFolderList.poll();
		}
		else
		{
			mySrcFolder=cur_folder;
		}   	  
		if(mySrcFolder==null)
		{
			debug.print("CreateLink: folder is wrong");
			return false;
		}
		
	    String dstFolder=FilesysProcess.getFather(dst);
	    String dstFile=FilesysProcess.getName(dst);
	    LinkedList<Folder> myDstFolderList=new LinkedList<Folder>();
	    Folder myDstFolder=null;
	    if(!dstFolder.equals("#"))
	    {
	    	changeCurFolder(dstFolder,false,myDstFolderList);
	    	myDstFolder=myDstFolderList.poll();
	    }
	    else
	    {
	    	myDstFolder=cur_folder;
	    }
	    if(myDstFolder==null)
	    {
	    	debug.print("CreateLink:Dst folder wrong");
	    	return false;
	    }
      return true;
  }

public int getFreeSize() {
    return free_list.getSize()+getSwapFileSectors();
}

public int getSwapFileSectors() {
      return  ((VMKernel)Kernel.kernel).getSecNum();
}
}
