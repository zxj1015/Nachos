package nachos.threads;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat {
	static BoatGrader bg;

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}

	
	public static void begin(int adults, int children, BoatGrader b) {
		
		bg = b;		
		KThread adultThread[ ]=new KThread[adults];
		for(int i=0;i<adults;++i)
		{
			Runnable r1=new Runnable()	{
				public void run()		{
						AdultItinerary();
				}				
			};
			adultThread[i]=new KThread(r1);
			adultThread[i].setName("AdultBoat "+i+" Thread");
			adultThread[i].fork();
		}
		KThread childThread[ ]=new KThread[children];
		for(int i=0;i<children;++i)
		{
			Runnable r2=new Runnable(){
				public void run(){
					ChildItinerary();
				}
			} ;
			childThread[i]=new KThread(r2);
			childThread[i].setName("ChildBoat "+i+" Thread");
			childThread[i].fork();
		}
		for(int i=0;i<adults;++i)
			adultThread[i].join();
		for(int j=0;j<children;++j)
			childThread[j].join();
		
	}
static int adultsOahu=0;
static int adultsMolokai=0;
static int childrenOahu=0;
static int childrenMolokai=0;
	
static String boatLocation="Oahu";	
static Lock  adultSignUpLock=new Lock();
static Condition adultSignUp	=new Condition(adultSignUpLock);	

static void AdultItinerary() {
		boolean flag=Machine.interrupt().disable();
		adultsOahu++;
		String location="Oahu";
		adultSignUpLock.acquire();
		adultSignUp.sleep();
		adultSignUpLock.release();
		
		--adultsOahu;
		++adultsMolokai;
		location="Molokai";
		bg.AdultRowToMolokai();
		sleepMolokaiLock.acquire();
		sleepMolokai.wake();
		sleepMolokaiLock.release();
		Machine.interrupt().restore(flag);
		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * bg.AdultRowToMolokai(); indicates that an adult has rowed the boat
		 * across to Molokai
		 */
	}
	static int childrenOnBoat=0;
	static int children=0;
	static Lock childrenBoatLock=new Lock();
	static Condition childrenBoat=new Condition(childrenBoatLock);

	static Lock childrenwaitBoatLock=new Lock();
	static Condition childrenwaitBoat=new Condition(childrenwaitBoatLock);
	
	static Lock sleepMolokaiLock=new Lock();
	static Condition sleepMolokai=new Condition(sleepMolokaiLock);
	
	static Lock sleepOahuLock=new Lock();
	static Condition sleepOahu=new Condition(sleepOahuLock);
	
	static void ChildItinerary() {
		childrenOahu++;
		String location="Oahu";
		KThread.yield();
		boolean flag=Machine.interrupt().disable();
		children++;
		if(children>2) 
		{	
			childrenBoatLock.acquire();
			childrenBoat.sleep(); 
			childrenBoatLock.release();
		}
		else
		{
			KThread.yield();
		}
		childrenOnBoat++;
		while(childrenOahu+adultsOahu>0)
		{
		
			if(childrenOnBoat<2)
			{
				childrenwaitBoatLock.acquire();
				childrenwaitBoat.sleep();
				childrenwaitBoatLock.release();
				//when wake up do next
				--childrenOahu;
				++childrenMolokai;
				location="Molokai";
				boatLocation="Molokai";
				bg.ChildRideToMolokai();
				if(childrenOahu+adultsOahu==0)
				{
					sleepMolokaiLock.acquire();
					sleepMolokai.wakeAll();
					sleepMolokaiLock.release();
					break;
				}
				++childrenOahu;
				--childrenMolokai;
				location="Oahu";
				boatLocation="Oahu";
				bg.ChildRowToOahu();
				
				childrenBoatLock.acquire();
				childrenBoat.wake();
				childrenBoatLock.release();
	//			System.out.println("-----------------------------------"+childrenOahu);
				childrenOnBoat--;
				if(childrenOahu<=1) break;
				
			}
			else
			{				
				--childrenOahu;
				++childrenMolokai;
				location="Molokai";
				bg.ChildRowToMolokai();
				
				childrenwaitBoatLock.acquire();
				childrenwaitBoat.wake();
				childrenwaitBoatLock.release();
				
				sleepMolokaiLock.acquire();
				sleepMolokai.sleep();
				sleepMolokaiLock.release();
				break;
			}
		}
//		System.out.println(location);
//		System.out.println("-------------------------------------------------------------------------------------------------------------------------I am over");
		while(childrenOahu+adultsOahu>0)
		{
			if(location.equals("Molokai"))
			{
				bg.ChildRowToOahu();
				bg.ChildRowToMolokai();
				sleepOahuLock.acquire();
				sleepOahu.wake();
				sleepOahuLock.release();
				sleepMolokaiLock.acquire();
				sleepMolokai.sleep();
				sleepMolokaiLock.release();
				
			}
			else
			{
				adultSignUpLock.acquire();
				adultSignUp.wake();
				adultSignUpLock.release();
				sleepOahuLock.acquire();
				sleepOahu.sleep();
				sleepOahuLock.release();		//shuijiao	
				--childrenOahu;
				bg.ChildRideToMolokai();
				if(adultsOahu==0) 
				{
					sleepMolokaiLock.acquire();
					sleepMolokai.wakeAll();
					sleepMolokaiLock.release();
					break;
				}
				++childrenOahu;
				bg.ChildRowToOahu();
			}
			
		}
		Machine.interrupt().setStatus(flag);
		
	}

}
