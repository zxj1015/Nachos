@echo off

java -cp bin nachos.machine.Machine -- nachos.ag.JoinGrader -[] conf/nachos1.conf -# waitTicks=1000,times=1000 
java -cp bin nachos.machine.Machine -[] conf/nachos1.conf -- nachos.ag.LockGrader11 
java -cp bin nachos.machine.Machine -- nachos.ag.ThreadGrader1 -[] conf/nachos1.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.ThreadGrader2 -[] conf/nachos1.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.ThreadGrader3 -[] conf/nachos1.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.ThreadGrader4 -[] conf/nachos1.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.BoatGrader -[] conf/nachos1.conf -# adults=120,children=120 -d s

pause

java -cp bin nachos.machine.Machine -- nachos.ag.ThreadGrader5 -[] conf/nachos2.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.ThreadGrader6 -[] conf/nachos2.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.DonationGrader -[] conf/nachos2.conf 
java -cp bin nachos.machine.Machine -- nachos.ag.PriorityGrader -[] conf/nachos2.conf -# threads=100,times=10,length=10
java -cp bin nachos.machine.Machine -- nachos.ag.PriorityGraderS1 -[] conf/nachos2.conf -# threads=100,times=10,locks=10

pause
