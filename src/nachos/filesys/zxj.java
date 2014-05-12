package nachos.filesys;

import nachos.machine.Disk;

public class zxj {

    /**
     * @param args
     */
   public static void testINode(INode x)
   {
       System.out.println(x.file_type);
       x.save();
       INode y=new INode(x.getAddr());
       y.load();
       System.out.println(y.file_type);
   }

}
