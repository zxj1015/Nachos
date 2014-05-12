package nachos.userprog;

public class debug {
    private static int num=0;
	public static void print(String mes)
	{
	//        if(num++>300) return;
			System.out.println(mes);
	}
	public static void print(int mes)
	{
	 //   if(num++>300) return ;
		System.out.println(mes);
	}
	public static void print(int mes1,int mes2,int mes3)
	{
	//    if(num++>300) return;
	    System.out.println(mes1+" "+mes2+" "+mes3);
	}
}
