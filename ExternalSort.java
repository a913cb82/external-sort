
package local.fjava.tick0;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.PriorityQueue;

public class ExternalSort
{
  // Merge sort
  public static void sort(String f1, String f2) throws FileNotFoundException,
      IOException
  {
    // First, let's use Collections.sort() to help us get started
    DataInputStream dis = new DataInputStream(
                           new BufferedInputStream(
                             new FileInputStream(f1)));
    DataOutputStream dos = new DataOutputStream(
                            new BufferedOutputStream(
                             new FileOutputStream(f2)));

    final int initialBlockSizeInts = 2<<10;
    while (spaceAvailable(dis))
      {
        /*
        ArrayList<Integer> tempList = new ArrayList<Integer>(initialBlockSizeInts);

        // read ints into tempList
        for (int i = 0; i < initialBlockSizeInts & spaceAvailable(dis); i++)
          tempList.add(dis.readInt());

        // sort them
        Collections.sort(tempList);

        // write them out
        for (int i : tempList)
          dos.writeInt(i);
        */

        int available = dis.available() / 4;
        int size = (available > initialBlockSizeInts ? initialBlockSizeInts : available);
        int[] tempArray = new int[size];

        // read ints into tempArray
        for (int i = 0; i < size; i++)
          tempArray[i] = dis.readInt();
        
        // sort them
        Arrays.sort(tempArray);

        // write them out
        for (int j : tempArray)
          dos.writeInt(j);
      }

    dos.flush();
    dis.close();
    dos.close();

    // Now prepare for merge sort

    // Size of sorted blocks
    long blockSizeBytes = initialBlockSizeInts * 4;
    // Total length of file
    RandomAccessFile raf = new RandomAccessFile(f1, "r");
    final long lengthBytes = raf.length();
    raf.close();
    System.out.println("File size: " + lengthBytes / 4 + " ints");
    
    // how many blocks to merge at once
    int blocksToMerge =(int) (1 + lengthBytes / (initialBlockSizeInts * 4));
    double power = 1;
    while (Math.pow(blocksToMerge, 1 / power) > 2<<8)
      power += 2;
    
    blocksToMerge = (int) (Math.pow(blocksToMerge, 1 / power) + 1);
    System.out.println("Merge " + blocksToMerge + " blocks at once");
    
    // note these have been swapped round by earlier Collections.sort()
    String inputString = f2;
    String outputString = f1;
    
    // big pass
    while (blockSizeBytes < lengthBytes)
      {
        System.out.println("Block Size = " + blockSizeBytes / 4 + " ints");

        // merge blocks in inputFile, storing in outputFile
        merge(inputString, outputString, blockSizeBytes, blocksToMerge);

        // blocks should now be bigger
        blockSizeBytes *= blocksToMerge;

        // swap input and output files then repeat
        String tempString = inputString;
        inputString = outputString;
        outputString = tempString;
      }
    System.out.println("Finished on Block Size = " + blockSizeBytes / 4 + " ints");
    // sorted result should now be in one file
    // if in f1, done
    // if in f2, copy to f1
    // recall input / outputString have been swapped
    if (outputString.equals(f1))
      {
        System.out.println("Copying back to initial file");
        copy(f2, f1);
      }
    // done!
  }

  // Merge stage of external mergesort
  // Read from input file, already sorted into blocks of size blockSize
  // Write to output file, sorted into blocks of 2*blockSize
  public static void merge(String inputFile, String outputFile,
                           long blockSizeBytes, int blocksToMerge)
      throws IOException
  {
    DataInputStream[] dis = new DataInputStream[blocksToMerge];
    for (int i = 0; i < blocksToMerge; i++)
      dis[i] = new DataInputStream(
                new BufferedInputStream(
                 new FileInputStream(inputFile)));
    DataOutputStream dos = new DataOutputStream(
                            new BufferedOutputStream(
                             new FileOutputStream(outputFile)));

    // when block size > 2*buffer size, we just endlessly write to a file
    // appears that dis.available() returns 0x7FFFFFFF (max int value)
    // fixed by checking for this value (see spaceAvailable method)

    // merging blocksToMerge sub lists

    // initialise blocks at right position
    for (int i = 0; i < blocksToMerge; i++)
      dis[i].skipBytes((int) (blockSizeBytes * i));

    // for use later
    final long blockSizeInts = blockSizeBytes / 4;
    final long bytesToSkip = blockSizeBytes * (blocksToMerge-1);

    // while we haven't reached the end of the file
    while (spaceAvailable(dis[0]))
      {
        // examine 1 set of blocks
        int[] blockPos = new int[blocksToMerge];
        PriorityQueue<Data> dataQueue = new PriorityQueue<Data>(blocksToMerge);
        
        // enque first int of each block
        for (int i = 0; i < blocksToMerge; i++)
          if (spaceAvailable(dis[i]))
            dataQueue.offer( new Data( dis[i].readInt() , i ) );
        
        // keep going until fully examined all blocks
        while ( ! dataQueue.isEmpty() )
          {
            // deque min piece of data available
            Data data = dataQueue.poll();
            int d = data.getData();
            int b = data.getBlock();
            
            // write it to output
            dos.writeInt(d);
            // advance pointer on where we just came from
            blockPos[b]++;
            // if data still available from there, add to priority queue
            if (blockPos[b] < blockSizeInts & spaceAvailable(dis[b]))
              dataQueue.offer( new Data( dis[b].readInt() , b ) );
          }
        // skip to next blocks
        for (int i = 0; i < blocksToMerge; i++)
          dis[i].skipBytes((int) bytesToSkip);
      }
    dos.flush();
    for (int i = 0; i < blocksToMerge; i++)
      dis[i].close();
    dos.close();
  }

  public static boolean spaceAvailable(DataInputStream dis) throws IOException
  {
    int space = dis.available();
    return (space > 0) & (space != 0x7FFFFFFF);
  }

  private static void copy(String f1, String f2) throws IOException
  {
    DataInputStream dis = new DataInputStream(
                           new BufferedInputStream(
                            new FileInputStream(f1)));
    DataOutputStream dos = new DataOutputStream(
                            new BufferedOutputStream(
                             new FileOutputStream(f2)));

    while (spaceAvailable(dis))
      dos.writeInt(dis.readInt());

    dis.close();
    dos.close();
  }

  private static String byteToHex(byte b)
  {
    String r = Integer.toHexString(b);
    if (r.length() == 8)
      {
        return r.substring(6);
      }
    return r;
  }

  @SuppressWarnings("resource")
  public static String checkSum(String f)
  {
    try
      {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream ds = new DigestInputStream(new FileInputStream(f), md);
        byte[] b = new byte[512];
        while (ds.read(b) != - 1)
          ;

        String computed = "";
        for (byte v : md.digest())
          computed += byteToHex(v);

        return computed;
      }
    catch (NoSuchAlgorithmException e)
      {
        e.printStackTrace();
      }
    catch (FileNotFoundException e)
      {
        e.printStackTrace();
      }
    catch (IOException e)
      {
        e.printStackTrace();
      }
    return "<error computing checksum>";
  }

  public static void read(String f) throws IOException
  {
    System.out.println();
    DataInputStream dis = new DataInputStream(
                           new BufferedInputStream(
                            new FileInputStream(f)));

    while (spaceAvailable(dis))
      System.out.print(dis.readInt() + ", ");

    dis.close();
    System.out.println();
  }
  
  public static void main(String[] args) throws Exception
  {
    ///*
    int tosort = 16;
    args = new String[] {
                         "C:/Users/Staples/Documents/Programming/Java/FurtherJavaTicks/test-suite/test"
                             + tosort + "a.dat",
                         "C:/Users/Staples/Documents/Programming/Java/FurtherJavaTicks/test-suite/test"
                             + tosort + "b.dat" };
    //*/

    String f1 = args[0];
    String f2 = args[1];

    // Time to sort 16:
    // Old method                          : 63737
    // LinkedList using Collections.sort() : 50006
    // ArrayList using Collections.sort()  : 45462
    // ArrayList using Collections.min()   : 39251
    // ArrayList using both                : 36284
    // LinkedList using both               : 36252
    // PriorityQueue                       : 25664
    // PriorityQueue + Array first sort    : 14422
    // Updated spaceAvailable              : 10114
    
    long start = System.currentTimeMillis();
    
    sort(f1, f2);
    
    long end = System.currentTimeMillis();

    System.out.println("Sort took " + (end - start) + "ms");

    System.out.println("The checksum is: " + checkSum(f1));
  }
}
