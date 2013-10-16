/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the FRIL Framework.
 *
 * The Initial Developers of the Original Code are
 * The Department of Math and Computer Science, Emory University and 
 * The Centers for Disease Control and Prevention.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */ 


package cdc.utils;


public class CPUInfo {
	
	private static int cpus = -1;
	
	public static int testNumberOfCPUs() {
		if (cpus == -1) {
			String useMulticore = Props.getString("cpu-use-multicore");
			if (!"yes".equalsIgnoreCase(useMulticore)) {
				cpus = 1;
			} else {
				String cpuNumber = Props.getString("cpu-number");
				if (cpuNumber != null) {
					Log.log(CPUInfo.class, "Warning: CPU info was not determined automatically. Property \"cpu-number\" present.", 1);
					cpus = Integer.parseInt(cpuNumber);
				} else {
					cpus = Runtime.getRuntime().availableProcessors();
				}
			}
		}
		return cpus;
	}
	
//	private static long TEST_INTERVAL;
//	private static final long MAX_TIME = Props.getLong("10000");
//	private static final int MAX_CPU_TESTED = Props.getInteger("cpu-utils-max-cpus-tested");
//	
//	private static class TestThread extends Thread {
//		
//		public void run() {
//			ThreadMXBean info = ManagementFactory.getThreadMXBean();
//			long t1 = info.getCurrentThreadCpuTime();
//			for (long i = 0; i < TEST_INTERVAL; i++) {
//				if (i % 10000 == 0) {
//					long c = comps.addAndGet(10000);
//					double frac = c / (double)TEST_INTERVAL / MAX_CPU_TESTED * (maxValProgress / (double)2);
//					progressBar.setValue((int) (maxValProgress / 2 + frac));
//				}
//			}
//			long t2 = info.getCurrentThreadCpuTime();
//			System.out.println("Thread" + Thread.currentThread() + " cpu time: " + (t2-t1));
//			total.addAndGet(t2-t1);
//			latch.countDown();
//		}
//	}
//
//	private static AtomicLong total = new AtomicLong();
//	private static AtomicLong comps = new AtomicLong();
//	private static CountDownLatch latch;
//	private static JProgressBar progressBar;
//	private static int maxValProgress = -1;
//	
//	public static int testNumberOfCPUs() {
//		//test cpus
//		TEST_INTERVAL = determineInterval();
//		int n = MAX_CPU_TESTED;
//		System.out.println("Max CPUs: " + n);
//		System.out.println("Interval: " + TEST_INTERVAL);
//		try {
//			long t1 = System.nanoTime();
//			latch = new CountDownLatch(n);
//			TestThread[] thrs = new TestThread[n];
//			for (int i = 0; i < thrs.length; i++) {
//				thrs[i] = new TestThread();
//				thrs[i].start();
//			}
//			latch.await();
//			long t2 = System.nanoTime();
//			System.out.println("Elapsed time:  " + (t2-t1));
//			System.out.println("CPU time:      " + total.get());
//			double factor = total.get() / (double) (t2-t1);
//			System.out.println("The factor is: " + (factor));
//			
//			return (int)Math.floor(factor);
//			
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		
//		return -1;
//	}
//	
//	private static long determineInterval() {
//		long t1 = System.currentTimeMillis();
//		long elapsed = 0;
//		long n = 0;
//		while ((elapsed = System.currentTimeMillis() - t1) < MAX_TIME) {
//			n++;
//			if (n % 10000 == 0) {
//				if (progressBar != null) {
//					double frac = elapsed / (double)MAX_TIME * (maxValProgress / (double)2);
//					progressBar.setValue((int) frac);
//				}
//			}
//		}
//		return n * 6 / (long)MAX_CPU_TESTED;
//	}
//
//	public static void main(String[] args) {
//		testNumberOfCPUs();
//	}
//
//	public static void registerProgressBar(JProgressBar progress) {
//		progressBar = progress;
//		maxValProgress = progressBar.getMaximum();
//	}
}
