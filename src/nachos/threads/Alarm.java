package nachos.threads;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
        private PriorityQueue<Long> wakeTimes;
        private HashMap<Long, KThread> waiters;
        /**
         * Allocate a new Alarm. Set the machine's timer interrupt handler to this
         * alarm's callback.
         *
         * <p><b>Note</b>: Nachos will not function correctly with more than one
         * alarm.
         */
        public Alarm() {
                Machine.timer().setInterruptHandler(new Runnable() {
                        public void run() { timerInterrupt(); }
                });

                wakeTimes = new PriorityQueue<Long>();
                waiters = new HashMap<Long, KThread>();
        }

        /**
         * The timer interrupt handler. This is called by the machine's timer
         * periodically (approximately every 500 clock ticks). Causes the current
         * thread to yield, forcing a context switch if there is another thread
         * that should be run.
         */
        public void timerInterrupt() {
                KThread.yield();
                
                boolean intStatus = Machine.interrupt().disable();

                if(wakeTimes.size() > 0){
                        long firstWakeTime = wakeTimes.peek();
                        KThread firstWaiter = waiters.get(firstWakeTime);

                        if(Machine.timer().getTime() >= firstWakeTime &&
                                        firstWaiter != null){
                                firstWaiter.ready();
                                waiters.remove(firstWaiter);
                                wakeTimes.poll();
                                Machine.interrupt().restore(intStatus);
                        }
                }
        }

        /**
         * Put the current thread to sleep for at least <i>x</i> ticks,
         * waking it up in the timer interrupt handler. The thread must be
         * woken up (placed in the scheduler ready set) during the first timer
         * interrupt where
         *
         * <p><blockquote>
         * (current time) >= (WaitUntil called time)+(x)
         * </blockquote>
         *
         * @param       x       the minimum number of clock ticks to wait.
         *
         * @see nachos.machine.Timer#getTime()
         */
        public void waitUntil(long x) {
                long wakeTime = Machine.timer().getTime() + x;
                KThread temp = KThread.currentThread();

                boolean intStatus = Machine.interrupt().disable();
                wakeTimes.add(wakeTime);
                waiters.put(wakeTime, temp);

                KThread.sleep();
                Machine.interrupt().restore(intStatus);
        }

}