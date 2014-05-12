package nachos.vm;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import nachos.filesys.File;
import nachos.filesys.FilesysKernel;
import nachos.machine.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
   
   public VMKernel() {
          super();
   }
   public void initialize(String[] args) {
       super.initialize(args);
       mem = new PhysicalMemory();
   }

  public void selfTest() {
 
  }
        
  public void run() {
     super.run();
  }
        
  public void terminate() {
      if(swap==null) swap=new SwapFile();
      swap.delete();
      super.terminate();
  }
        
  static public VMKernel getKernel() {
      return (VMKernel)UserKernel.getKernel();
  }
        
        //Releases all the resource occupied by this process
  public void discard(VMProcess process) {
      Lib.debug('v', "************discard all the data about Process"+process.getProcessID());
      if(swap==null) swap=new SwapFile(); 
      swap.discard(process);     
       mem.discard(process);
  }
  public void invalidateTLB() {
     Processor p = Machine.processor();              
     for(int t=0; t<p.getTLBSize(); t++) {
         fillTLBEntry(t, null);
     }
  //   Lib.debug('v', "invalidateTLB");
  }
  public TranslationEntry readyPage(VMProcess p, int vpn) {
      TranslationEntry e=readyPage(new PageId(p, vpn));
  //    if(e!=null)
  //       Lib.debug(dbgVM,"readyPage:"+"PID-"+ p.getProcessID()+" VPN-"+vpn+" PPN-"+e.ppn);
  //    else
  //       Lib.debug(dbgVM, "readyPage: can't readypage");
      return e;
  }
        //there three location to store;
  public TranslationEntry readyPage(PageId pid) {
            
     TranslationEntry page = null;
                
     page = mem.getPage(pid);
     if(page != null)
        return page;
                
     pageFaults++;
     VMProcess.pageLock.acquire();
     
     if(swap==null) swap=new SwapFile();
     if(swap.swapIn(pid)) {
         VMProcess.pageLock.release();
         return mem.getPage(pid);
     }
                
     page = freePage();
                
     if(page == null) {
         VMProcess.pageLock.release();
        return null;
     }
     page.vpn = pid.getVPN();
                
    if(pid.getProcess().readyPage(page)) {
        mem.addPage(pid, page);
        VMProcess.pageLock.release();
        return page;
    }             
    VMProcess.pageLock.release();
    return null;
  }
        
  public TranslationEntry freePage() 
  {
     int victim = -1;
     for(int p=0; p<Machine.processor().getNumPhysPages(); p++) 
     {
       if(!mem.hasOccupied(p)) 
       {
           victim = p;
           break;
       }
     }
     if(victim < 0) 
     {                        
        PageId pid = mem.clock();
        victim = mem.getPage(pid).ppn;
        if(swap==null) swap=new SwapFile();
        if(!swap.swapOut(pid)) 
        {
           return null;
        }
     }
     return new TranslationEntry(-1, victim, true, false, false, false);
   }
        
   public void updateFromTLB() 
   {
     Processor p = Machine.processor();
     for(int i=0; i<p.getTLBSize(); i++) 
     {
       TranslationEntry page = p.readTLBEntry(i);
       TranslationEntry mpage = mem.ppnToTransEntry(page.ppn);
                        
       if(page != null && mpage != null && page.valid && mpage.valid) 
       {
          if(page.used)
             mpage.used = true;
          if(page.dirty)
             mpage.dirty = true;
       }
     }
    }
    public void updateTLB() 
    {
       Processor p = Machine.processor();         
       for(int i=0; i<p.getTLBSize(); i++) 
       {
          TranslationEntry page = p.readTLBEntry(i);
          TranslationEntry mpage = mem.ppnToTransEntry(page.ppn);
          if(mpage != null) 
          {
                   p.writeTLBEntry(i, mpage);
          }                       
       }
    }
        
        public void fillTLBEntry(int t, TranslationEntry fill) {
                Processor p = Machine.processor();
                
                TranslationEntry old = p.readTLBEntry(t);               
                if(old.valid) {
                        TranslationEntry page = mem.ppnToTransEntry(old.ppn);
                        
                        if(page != null) {
                                if(old.used)
                                        page.used = true;
                                if(old.dirty)
                                        page.dirty = true;
                        }               
                }
                
                if(fill == null) {
                        fill = new TranslationEntry(-1,-1,false,false,false,false);
                }
                
                p.writeTLBEntry(t,fill);
        }
        
        public void fillTLBEntry(TranslationEntry fill) {
                Processor p = Machine.processor();
                
                int tlbs = p.getTLBSize();      
                for(int t=0; t<tlbs; t++) {
                        TranslationEntry te = p.readTLBEntry(t);
                        if(!te.valid) {
                                p.writeTLBEntry(t, fill);
                                return;
                        }
                }
                
                int t = new Random().nextInt(tlbs);
                fillTLBEntry(t,fill);           
        }
             
   private class PageId 
   {
       public PageId(VMProcess p, int vpn) 
       {
            this.process = p;
            this.vpn = vpn;
       }
       public int hashCode() 
       {
            return process.hashCode() + 7*vpn;
       }
       public boolean equals(Object other) 
       {
            if(other instanceof PageId) 
            {
                PageId opid = (PageId)other;
                return process == opid.process && vpn == opid.vpn; 
            }
            return false;
       }
       public VMProcess getProcess() 
       {
            return process;
       }
       public int getVPN() 
       {
           return vpn;
       }
       public String toString()
       {
           return process.getProcessID()+" "+vpn;
        }
        private VMProcess process = null;
        private int vpn = -1;
    }
        
    static Set<PageId> findProcess(VMProcess p, Set<PageId> ids) 
    {
       Set<PageId> found = new HashSet<PageId>();
       for(PageId pid : ids) 
       {
          if(pid.getProcess() == p)
             found.add(pid);
       }
       return found;
    }
    public int getSecNum()
    {
        if(swap==null) swap=new SwapFile();
        return swap.getSwapNum();
    }
    private class SwapFile {        
               
       private Map<PageId, Integer> index = new HashMap<PageId, Integer>();
       private Queue<Integer> holes = new LinkedList<Integer>();
       private int size = 0;            
       private OpenFile file = null;
       private String fileName = "SWAP";

       public SwapFile() 
       {
           file=fileSystem.open("/"+fileName, true);
           Lib.debug(dbgVM,"SwapFile: "+fileName+" is created");
       }
       public int getSwapNum()
       {
           return ((File)file).getsector();
       }
       public void discard(VMProcess p )
       { 
          Set<PageId> toRemove=findProcess(p, index.keySet());
          for(Iterator<PageId> pid=toRemove.iterator();pid.hasNext();) 
          {
               holes.add(index.remove(pid.next()));
          }
       }
       public void delete() 
       {
          if(file != null)
              file.close();
          file = null;
          index.clear();
          holes.clear();
          size = 0;
          fileSystem.remove("/"+fileName);
       }
       public boolean swapIn(PageId pid) 
       {
           
           if(mem.hasPage(pid))
           {
   //            Lib.debug('v',"swapIn: this page is already in the memory");
               return true;
           }
           if(!index.containsKey(pid))
           {
  //             Lib.debug('v',"swapIn: this page is not in the swap file");
               return false;
           }
           TranslationEntry page = freePage();
           if(page == null)
           {
   //           Lib.debug('v',"swapIn: can't find an free physics memory");
              return false;
           }
           page.vpn = pid.getVPN();                                                
   //        Lib.debug(dbgVM, "swapIn: " + page.ppn);
                        
           int pos = index.get(pid);
           Processor processor = Machine.processor();
           byte[] memory = processor.getMemory();
           int paddr = Processor.makeAddress(page.ppn, 0);
           int bytes = file.read(pos, memory, paddr, Processor.pageSize);
           if(bytes == Processor.pageSize) 
           {                             
               mem.addPage(pid, page);                                                         
           }
           if(bytes == Processor.pageSize)
           {
    //           Lib.debug(dbgVM,"swapIn: is success");
               return true;
           }
           else
           {
    //          Lib.debug(dbgVM,"swapIn: something is wrong");
              return false;
           }
       }
       public boolean swapOut(PageId pid) 
       {
           debug.print("****SWAP out****");
           if(!mem.hasPage(pid))
           {
   //            Lib.debug(dbgVM,"swapOut: this page is not in mem");
               return true;
           }
           TranslationEntry page = mem.getPage(pid);
           if(page == null)
           {
    //          Lib.debug(dbgVM,"swapOut: this page is null");
              return false;
            }
            for(int t=0; t<Machine.processor().getTLBSize(); t++) 
            {
               TranslationEntry e = Machine.processor().readTLBEntry(t);
               if(e.vpn == page.vpn) 
               { 
                    fillTLBEntry(t, null);
               }
            }
            boolean ok = true;
            if(!page.readOnly && page.dirty) 
            {      
                Integer pos = index.get(pid);
                if(pos == null) 
                {
                      pos = holes.poll();
                }                       
                if(pos == null) 
                {
                      pos = size;                          
                      size += Processor.pageSize;
                }
                int bytes = 0;
                if(file != null) 
                {
                   Processor processor = Machine.processor();
                   byte[] memory = processor.getMemory();
                   int paddr = Processor.makeAddress(page.ppn, 0);
  //                 Lib.debug(dbgVM, "swapOut: to disk @ "+ pos + " id " + page.hashCode());  
                   bytes = file.write(pos, memory, paddr, Processor.pageSize);
                }
                if(bytes != Processor.pageSize) 
                {
                   ok = false;
                   holes.add(pos);
                } 
                else
                {
                    index.put(pid, pos);                         
                }
             }       
             mem.removePage(pid);
             return ok;
        }
  }
        
 private class PhysicalMemory 
 {
   private Map<PageId, TranslationEntry> ipt = new HashMap<PageId,TranslationEntry>();
   private Map<Integer, PageId> coreMap = new HashMap<Integer, PageId>();
   private ArrayList<PageId> clock = new ArrayList<PageId>();
   private int clockPos = 0;
   
   public PageId clock() 
   {
     if(clock.size() <= 0)
     {
        Lib.debug(dbgVM,"ClockAlg: clock size is not bigger than zero");
        return null;   
     }
     updateFromTLB();
     while(true) 
     {
       PageId pid = clock.get(clockPos);
       TranslationEntry page = getPage(pid);                           
       if(!page.used) 
       {
           updateTLB();
   //        Lib.debug(dbgVM,"ClockAlg: select-"+pid.getProcess().getProcessID()+" "+pid.getVPN());                            
           return pid;
       }
       else 
       {
            page.used = false;
       }
       clockPos = (clockPos+1) % clock.size();
     }                       
   }
   public void discard(VMProcess process) 
   {
        for(PageId r : findProcess(process, mem.getallPageIdsInMem())) 
        {
          mem.removePage(r);
         }    
   }
   public PageId ppnToPageId(int ppn) 
   {
        return coreMap.get(ppn);
   }
   public Set<PageId> getallPageIdsInMem() 
   {
        return ipt.keySet();
   }
   public Set<TranslationEntry> getAllPagesTransEntry() 
   {
       return new HashSet<TranslationEntry>(ipt.values());
   }
   public boolean hasOccupied(int ppn) {
       return coreMap.get(ppn)!=null;
   }
   public boolean hasPage(PageId id) 
   {
     return ipt.containsKey(id);
   }
   public TranslationEntry getPage(PageId id) 
   {
       return ipt.get(id);
   }
   public TranslationEntry ppnToTransEntry(int ppn) {
       return getPage(ppnToPageId(ppn));
   }
   public TranslationEntry removePage(PageId id) 
   {
          TranslationEntry page = getPage(id);
          coreMap.remove(page.ppn);
          ipt.remove(id);
          page.ppn = -1;
          page.valid = false;         
          clock.remove(id);
          if(clock.size() > 1)
              clockPos = clockPos % clock.size();
          return page;
   }
   public void addPage(PageId pid, TranslationEntry page) 
   {
     coreMap.put(page.ppn, pid);
     ipt.put(pid, page);
     if(clock.size() > 0)
        clock.add((clockPos -1 + clock.size()) % clock.size(), pid);
     else 
     {
          clock.add(pid);
          clockPos = 0;
     }
     page.valid = true;
   }
 }
  private SwapFile swap=null;
  private PhysicalMemory mem =null;
        
  private int pageFaults = 0;
              
  private static final char dbgVM = 'v';
}