// ref: karuto/Parallel-Sample-Sort

import java.util.*;
import java.io.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class SampleSort implements Runnable {
  static CyclicBarrier barrier;
  static int i, threadCount, sampleSize, listSize, suppressOutput;
  static int[] list, sampleKeys, sortedKeys, splitters, tmpList, sortedList;
  static int[] rawDist, prefixDist, colDist, prefixColDist;

  private int myRank;

  public static void initClass(int tc, int ss, int ls, int[] inputList) {
    threadCount = tc;
    sampleSize = ss;
    listSize = ls;
    
    list = inputList;
    tmpList = new int[listSize];
    sortedList = new int[listSize];
    sampleKeys = new int[sampleSize];
    sortedKeys = new int[sampleSize];
    splitters = new int[threadCount + 1];
    
    rawDist = new int[threadCount * threadCount];
    colDist = new int[threadCount];
    prefixDist = new int[threadCount * threadCount];
    prefixColDist = new int[threadCount];
    barrier = new CyclicBarrier(threadCount);
  }

  public static int[] getSortedList () {
    return sortedList;
  }

  public SampleSort(long rank) {
    myRank = (int) rank;
  }

  private boolean isUsed(int seed, int offset, int range) {
    int i;    
  	for (i = offset; i < (offset + range); i++) {
  		if (sampleKeys[i] == list[seed]) {
  			return true;
  		} else {
  			return false;
  		}
  	}
  	return false;
  }


  @Override
  public void run() {
    int i, j, seed, index, offset, localChunkSize, localSampleSize;
    int localPointer, sindex, mySegment, colSum;
    int[] localData;

    localChunkSize = listSize / threadCount;
    localSampleSize = sampleSize / threadCount;
    
    // Get random sample keys from original list
    Random random = new Random(myRank + 1);
    offset = myRank * localSampleSize;
    for (i = offset; i < (offset + localSampleSize); i++) {
  	  do {
  		  // If while returns 1, you'll be repeating this
        int rand = random.nextInt((999999 - 0) + 1) + 0;
  		  seed = (myRank * localChunkSize) + (rand % localChunkSize);
  	  } while (isUsed(seed, offset, localSampleSize));
  	  // If the loop breaks (while returns 0), data is clean, assignment
  	  sampleKeys[i] = list[seed];
  	  index = offset + i;  	  
    }
    // Ensure all threads have reached this point, and then let continue    
    try { barrier.await(); } catch (Exception e) {}

    // Parallel count sort the sample keys
    for (i = offset; i < (offset + localSampleSize); i++) {
  	  int mykey = sampleKeys[i];
  	  int myindex = 0;
  	  for (j = 0; j < sampleSize; j++) {
  		  if (sampleKeys[j] < mykey) {
  			  myindex++;
  		  } else if (sampleKeys[j] == mykey && j < i) {
  			  myindex++;
  		  } else {
  		  }
  	  }  	  
  	  sortedKeys[myindex] = mykey;
    }
    // Ensure all threads have reached this point, and then let continue    
    try { barrier.await(); } catch (Exception e) {}
    // Besides thread 0, every thread generates a splitter
    // splitters[0] should always be zero
    if (myRank != 0) {
  	  splitters[myRank] = (sortedKeys[offset] + sortedKeys[offset-1]) / 2;
    }
    // Ensure all threads have reached this point, and then let continue    
    try { barrier.await(); } catch (Exception e) {}

    // Using block partition to retrieve and sort local chunk
    localPointer = myRank * localChunkSize;    
    localData = new int[localChunkSize];

    j = 0;
    for (i = localPointer; i < (localPointer + localChunkSize); i++) {
  	  localData[j] = list[i];
  	  j++;
    }
    // Quick sort on local data before splitting into buckets    
    Arrays.sort(localData);
    // index in the splitter array
    sindex = 1;
    // starting point of this thread's segment in dist arrays
    mySegment = myRank * threadCount;
    // Generate the original distribution array, loop through each local entry
    for (i = 0; i < localChunkSize; i++) {
  	  if (localData[i] < splitters[sindex]) {
  		  // If current elem lesser than current splitter
  		  // That means it's within this bucket's range, keep looping
  	  } else {
  		  // Elem is out of bucket's range, time to increase splitter
  		  // Keep increasing until you find one that fits
  		  // Also make sure if equals we still increment
  		  while (sindex < threadCount && localData[i] >= splitters[sindex]) {
  			  sindex++;
  		  }
  	  }
  	  // Add to the raw distribution array, -1 because splitter[0] = 0
  	  rawDist[mySegment + sindex-1]++;
    }

    // Ensure all threads have reached this point, and then let continue    
    try { barrier.await(); } catch (Exception e) {}

    // Generate prefix sum distribution array    
    for (i = mySegment; i < (mySegment + threadCount); i++) {
  	  if (i == mySegment) {
  		  prefixDist[i] = rawDist[i];
  		  // printf("Thread %ld ### i = %d, prefixDist[i] = %d, rawDist[i] = %d\n", myRank, i, prefixDist[i], rawDist[i];
  	  } else {
  		  prefixDist[i] = rawDist[i] + prefixDist[i - 1];
  	  }
    }

    // Ensure all threads have reached this point, and then let continue    
    try { barrier.await(); } catch (Exception e) {}
    // Generate column distribution array     
    for (i = mySegment; i < (mySegment + threadCount); i++) {
  	  if (i == mySegment) {
  		  prefixDist[i] = rawDist[i];	 
  	  } else {
  		  prefixDist[i] = rawDist[i] + prefixDist[i - 1];
  	  }
    }
    
    // Ensure all threads have reached this point, and then let continue
    try { barrier.await(); } catch (Exception e) {}
    
    // Generate column sum distribution, each thread responsible for one column
    colSum = 0;
    for (i = 0; i < threadCount; i++) {
  	  colSum += rawDist[myRank + i * threadCount];
    }
    colDist[myRank] = colSum;
    
    // Ensure all threads have reached this point, and then let continue
    try { barrier.await(); } catch (Exception e) {}
    
    // Generate prefix column sum distribution, each thread responsible for one column    
    if (myRank == 0) {
  	  for (i = 0; i < threadCount; i++) {
  		  if (i == 0) {
  		  	prefixColDist[i] = colDist[i];
  		  } else {
  		  	prefixColDist[i] = colDist[i] + prefixColDist[i - 1];
  		  }
  	  }
    }
    
    // Reassemble the partially sorted list, prepare for retrieval
    for (i = 0; i < localChunkSize; i++) {
  	  tmpList[localPointer + i] = localData[i];
    }
    
    // Ensure all threads have reached this point, and then let continue
    try { barrier.await(); } catch (Exception e) {}
    
    // Reassemble each thread's partially sorted list based on buckets
    // Allocate an array based on the column sum of this specific bucket
    int myFirstD = colDist[myRank];
    int[] myD = new int[myFirstD];
    
    int bindex = 0;
    // int i_manual = 0;
    // For each thread in the column...
    for (i = 0; i < threadCount; i++) {
  	  if (myRank == 0) {
  		  offset = (i * localChunkSize);
  	  } else {
  	  	  offset = (i * localChunkSize) + prefixDist[i*threadCount + myRank-1];
  	  }
  	  
  	  if (rawDist[i*threadCount + myRank] != 0) {
  		  for (j = 0; j < rawDist[i*threadCount + myRank]; j++) {
  			  myD[bindex] = tmpList[offset + j];
  			  bindex++;
  		  }
  		  
  	  } 
    }

    Arrays.sort(myD);

    // Merge thread bucket data into final sorted list
    if (myRank == 0) {
  	  for (i = 0; i < myFirstD; i++) {
  		  sortedList[i] = myD[i];
  	  }
    } else {
  	  offset = prefixColDist[myRank-1];
  	  for (i = 0; i < myFirstD; i++) {
  		  sortedList[offset + i] = myD[i];
  	  }
    }
    
    return;
  }
}