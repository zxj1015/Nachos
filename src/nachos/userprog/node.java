package nachos.userprog;

public class node {

		int start=0;
		int length=0;

		node(int s,int l)
		{
			start=s;
			length=l;
		}
		public int getStart()
		{
		    return start;
		}
		public int getLength()
		{
		    return length;
		}
}
