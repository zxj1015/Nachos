package nachos.vm;


import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
        /**
         * Allocate a new process.
         */
        public VMProcess() {
            
                super();
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
   //             Lib.debug(dbgVM, "restoreState:"+this.getProcessID());
                VMKernel.getKernel().invalidateTLB();
        }
        /**
         * Initializes page tables for this process so that the executable can be
         * demand-paged.
         * 
         * @return <tt>true</tt> if successful.
         */
        protected boolean loadSections() {
     //           Lib.debug(dbgVM, "LoadSections success:infact load nothing");
                return true;
        }
                
        static private void patternArray(byte[] array, int start, int end, int offset) {
                int d = offset;
                int[] pattern = {0xEF, 0xBE, 0xAD, 0xDE};
                //11101111 10111110 10101101 11011110
                while(start < end) {
                        array[start] = (byte)pattern[d];
                        start++;
                        
                        d = (d+1)%pattern.length;
                }
        }
        static private void patternArray(byte[] array, int start, int end) {
                patternArray(array,start,end,0);
        }
        
        static public void selfTest() { 
    //            Lib.debug(dbgVM,"---Testing VMProcess---");                
        }
        
        /**
         * Release any resources allocated by <tt>loadSections()</tt>.
         */
        protected void unloadSections() {
                pageLock.acquire();
                VMKernel.getKernel().discard(this);
                pageLock.release();                
                coff.close();
        }
        
  public boolean readyPage(TranslationEntry te) 
  {
     if(te == null)
     {
    //      Lib.debug(dbgVM, "readyPage: the translationEntry is null");
          return false;
     }
     Lib.assertTrue(pageLock.isHeldByCurrentThread());
                
     if(te.vpn >= numPages - stackPages -1) 
     {
        byte[] memory = Machine.processor().getMemory();
        int paddr = Processor.makeAddress(te.ppn, 0);
        int end = paddr + Processor.pageSize;                
        patternArray(memory, paddr, end);
        Lib.debug(dbgVM, "readyPage:loading from stack to " + te.ppn + " [" + te.vpn + "]"+" Process:"+this.getProcessID());
     } 
     else 
     {
        te.valid = false;
        if(coff != null )
        for (int s = 0; s < coff.getNumSections(); s++) 
        {
            CoffSection section = coff.getSection(s);
            if(section.getFirstVPN() <= te.vpn && te.vpn < section.getFirstVPN() + section.getLength()) 
            {
                int cvpn = te.vpn - section.getFirstVPN();
                section.loadPage(cvpn, te.ppn);
                Lib.debug(dbgVM, "readPage: loading from coff: " + s + "-" + cvpn + " to " + te.ppn + " [" + te.vpn + "]"+" Process:"+this.getProcessID());
                te.valid = true;
                te.readOnly = section.isReadOnly();
                break;
            }
         }                               
         Lib.assertTrue(te.valid);
      }       
      return true;
   }
        
        private void checkAvliadPage(int vpn) {
                if(vpn < 0 || vpn >= numPages) {
                        Lib.debug(dbgVM, "checkAv: page " + vpn + " in " + this.toString());
                  //      System.out.println("-- Page Fault --");
                        handleSyscall(syscallExit, 3, 0, 0, 0);
                        Lib.assertNotReached();
                }               
        }
        
        /**
         * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>.
         * The <i>cause</i> argument identifies which exception occurred; see the
         * <tt>Processor.exceptionZZZ</tt> constants.
         * 
         * @param cause
         *            the user exception that occurred.
         */
  public void handleException(int cause) {
    Processor processor = Machine.processor();
    VMKernel kernel = VMKernel.getKernel();
    int vaddr=0;
    int vpn=0;
    switch (cause) 
    {
        case Processor.exceptionPageFault:
            vaddr = processor.readRegister(Processor.regBadVAddr);
            vpn = Processor.pageFromAddress(vaddr);
            checkAvliadPage(vpn);            
          //  pageLock.acquire();
            if(kernel.readyPage(this, vpn) == null) 
            {
        //       pageLock.release();
               checkAvliadPage(-1);
            }
        //    pageLock.release();
            Lib.debug(dbgVM,"HandlePageFault:"+vpn);
            break;
        case Processor.exceptionTLBMiss:
            vaddr = processor.readRegister(Processor.regBadVAddr);
            vpn = Processor.pageFromAddress(vaddr);
            checkAvliadPage(vpn);
//            pageLock.acquire();
            TranslationEntry page = kernel.readyPage(this, vpn);
            if(page == null) 
            {                 
       //         pageLock.release();
                checkAvliadPage(-1);
            }
            kernel.fillTLBEntry(page);
          //      pageLock.release();
     //       Lib.debug(dbgVM,"HandleTLBMiss:"+vpn);
            break;
        default:
            super.handleException(cause);
            break;
     }
   }
//   public static boolean hasTwoTlb()
//   {
//      int num=0;
//      Processor p=Machine.processor();
//      for(int i=0;i<p.getTLBSize();++i)
//          if(p.readTLBEntry(i)!=null)
//              num++;
  //    System.out.println(num);
//      if(num<2) return false;
//      else return true; 
//   }

   public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
                return super.handleSyscall(syscall, a0, a1, a2, a3);
   }
        
        /**
         * Transfer data from this process's virtual memory to the specified array.
         * This method handles address translation details. This method must <i>not</i>
         * destroy the current process if an error occurs, but instead should return
         * the number of bytes successfully copied (or zero if no data could be
         * copied).
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
                
                int vpn = Processor.pageFromAddress(vaddr);
                
                int voffset = Processor.offsetFromAddress(vaddr);
                int ioffset = offset;           
                
                while(length > 0) {
                        int copy = Math.min(length, pageSize - voffset);

                   //     pageLock.acquire();
                        //the most important thing do
                        TranslationEntry page = VMKernel.getKernel().readyPage(this, vpn);
                        if(page == null) {
                 //               pageLock.release();
                                checkAvliadPage(-1);
                        }
                        int paddr = Processor.makeAddress(page.ppn, voffset);

                        Lib.debug(dbgVM, "READ: " + page.ppn + " vpn: " + vpn);      
                        System.arraycopy(memory, paddr, data, offset, copy);
                        
                        page.used = true;
                 //       pageLock.release();
                        
                        vpn++;
                        voffset = 0;
                        length -= copy;
                        offset += copy;
                }
                return offset - ioffset;
        }

        /**
         * Transfer data from the specified array to this process's virtual memory.
         * This method handles address translation details. This method must <i>not</i>
         * destroy the current process if an error occurs, but instead should return
         * the number of bytes successfully copied (or zero if no data could be
         * copied).
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

                byte[] memory = Machine.processor().getMemory();
                
                int vpn = Processor.pageFromAddress(vaddr);
                
                int voffset = Processor.offsetFromAddress(vaddr);
                int ioffset = offset;           
                
                while(length > 0) {
                        int copy = Math.min(length, pageSize - voffset);
                        
            //            pageLock.acquire();

                        TranslationEntry page = VMKernel.getKernel().readyPage(this, vpn);
                        if(page == null) {
            //                    pageLock.release();
                                checkAvliadPage(-1);
                        }
                        int paddr = Processor.makeAddress(page.ppn, voffset);
                        
                        System.arraycopy(data, offset, memory, paddr, copy);
                        Lib.debug(dbgVM, "---WRITE: " + page.ppn + " vpn: " + vpn);             
                        
                        page.dirty = true;
                        page.used = true;
                        
       //                 pageLock.release();
                        
                        vpn++;
                        voffset = 0;
                        length -= copy;
                        offset += copy;
                }
                return offset - ioffset;
        }

        public static Lock pageLock = new Lock();      
                
        private static final int pageSize = Processor.pageSize;

        private static final char dbgVM = 'v';
        
}
