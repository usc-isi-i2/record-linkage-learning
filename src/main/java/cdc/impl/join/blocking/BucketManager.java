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


package cdc.impl.join.blocking;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import cdc.datamodel.DataRow;
import cdc.impl.datastream.DataRowInputStream;
import cdc.impl.datastream.DataRowOutputStream;
import cdc.utils.Log;
import cdc.utils.Props;
import cdc.utils.RJException;
import cdc.utils.RowUtils;

/**
 * This class provides a complex functionality of the buckets manager. Specifically,
 * it has a capability of putting the records from two data sources into separate buckets.
 * The records that have the same value calculated by the hashing function end up in the
 * same bucket. The hashing function and attributes used for hashing can be specified in
 * the costructor.
 * 
 * The interface that this class provides works as follows. Once the BucketManager
 * is created, user can start adding records into it. As records are added, the buckets
 * are built (the records are cached in memory). Once there are too many records in the
 * cache of bucket manager, the class dumps the records into cache on disk. This frees
 * the space for new data. Once the phase of adding records to the bucket manager is
 * finished (signaled by calling the addingComplete), the user can start getting 
 * back the buckets. The main method to do that is the getBucket method, that
 * returns two dimensional array of records for both data sources that ended up in the same
 * bucket.
 * 
 * The caching of records to the files is quite complex process. The number of cache files
 * is specified in the configuration (by default 50). The records are being divided among those cache files,
 * and each cache file is responsible for a set of buckets. Initially the buckets are assigned
 * to cache files on a round-robin basis.
 * 
 * If the input files are small enough and the data that is being added to BucketManager fits within
 * its in-memory buffer, no caching to external files is performed.
 * 
 * The class uses inside thread to do the heavy work. This helps to keep the running time
 * of "add" methods quite short - the actual operation is scheduled in queue, and the add method
 * can return quickly. The inside thread has a role of reading the input items and doing the actual
 * processing of the item that should be stored in the bucket manager.
 *   
 * @author Pawel Jurczyk
 *
 */
public class BucketManager {

	/**
	 * Just some properties that are used by the code. The properties are read from the
	 * configuration file. BLOCK_TRASH_FACTOR specifies the size of in-memory cache for data.
	 */
	private static final int BLOCK_TRASH_FACTOR = Props.getInteger("bucket-manager-trash-factor");
	private static final String FILE_PREFIX = Props.getString("bucket-manager-file-prefix");
	private static final int FILE_POOL = Props.getInteger("bucket-manager-file-pool");
	
	/**
	 * The synonyms for left and right data source.
	 */
	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	
	/**
	 * Name of the property that will be appended to row cached into disk.
	 * This property will indicate what is the bucket of the record.
	 */
	private static final String BUCKET_MARKER = "bucket-id";
	
	/**
	 * An item of in-memory buffer.
	 */
	private class BufferItem {
		private int id;
		private DataRow row;
		private String hash;
	}
	
	//TODO: This class is a bit too long. Probably need to refactor, possibly move the the bucket consumer so that it is an external class
	/**
	 * A thread that has a role of reading buffer of records that should be stored in bucket manager
	 * and then adding those records into actual buffer - based on the bucket hash code.
	 * When all the records has been added, the BufferConsumer will start reading the data from bucket
	 * manager, and putting them into buffer of ready buckets.
	 *
	 */
	private class BufferConsumer extends Thread {
		
		public void run() {
			Log.log(getClass(), "Thread starts.", 2);
			try {
				//Phase of reading the data items scheduled in the add methods
				while ((!done || !buffer.isEmpty()) && !stopped) {
					BufferItem item = (BufferItem)buffer.poll(100, TimeUnit.MILLISECONDS);
					if (item == null) {
						continue;
					} else if (item.id == LEFT) {
						addToBucket(LEFT, item.row, item.hash);
					} else {
						int pool = addToBucket(RIGHT, item.row, item.hash);
						test[pool] = true;
					}
				}
				Log.log(getClass(), "Thread done with data reading. Now pushing results.", 2);
				
				//If data provided by any of the sources was stored in disk cache,
				//force the tail of records into the disk
				if (dataInFile[LEFT]) {
					trashIfNeeded(LEFT, true);
				}
				if (dataInFile[RIGHT]) {
					trashIfNeeded(RIGHT, true);
				}
				
				//Notify the counter that the consumer is ready to read back data
				counter.countDown();
				
				//Read the data (from disk if needed)
				while (true && !stopped) {
					if (bucketsInMemory.isEmpty()) {
						closeStreams();
						if (usedFilesFromPool[0] == FILE_POOL || usedFilesFromPool[1] == FILE_POOL) {
							break;
						}
						read[LEFT] = new HashMap();
						read[RIGHT] = new HashMap();
						if (dataInFile[0] && dataInFile[1]) {
							readBucketsFromFile(LEFT);
							readBucketsFromFile(RIGHT);
						} else if (dataInFile[0]) {
							readBucketsFromFile(LEFT);
							readBucketsFromMem(RIGHT);
						} else if (dataInFile[1]) {
							readBucketsFromMem(LEFT);
							readBucketsFromFile(RIGHT);
						} else {
							readBucketsFromMem(LEFT);
							readBucketsFromMem(RIGHT);
						}
						
						bucketsInMemory.addAll(read[LEFT].keySet());
						for (Iterator iterator = read[RIGHT].keySet().iterator(); iterator.hasNext();) {
							Bucket b = (Bucket) iterator.next();
							if (!bucketsInMemory.contains(b)) {
								bucketsInMemory.add(b);
							}
						}
						
					}
					
					//Check whether the data was exhausted, if yes, finish the loop
					if (bucketsInMemory.isEmpty()) {
						break;
					}
					
					//Fill in the buffer of ready buckets
					DataRow[][] ret = new DataRow[2][];
					Bucket b = (Bucket) bucketsInMemory.remove(0);
					ret[0] = getBucket(LEFT, b);
					ret[1] = getBucket(RIGHT, b);
					Log.log(getClass(), "Adding bucket to buffer: " + ret[0].length + " <-> " + ret[1].length, 3);
					bufferBuckets.put(ret);
				}
				
				//Thread done its work...
				Log.log(getClass(), "Thread has completed.", 2);
				bufferBuckets.put(new DataRow[][] {});
				thread = null;
				return;
			} catch (InterruptedException e) {
				Log.log(getClass(), "Thread was interrupted.", 2);
			} catch (IOException e) {
				error = true;
				synchronized(BucketManager.this) {
					exception = new RJException("Error", e);
				}
			} catch (RJException e) {
				error = true;
				synchronized(BucketManager.this) {
					exception = e;
				}
			} catch (Exception e) {
				error = true;
				Log.log(getClass(), "Unexpected error.", 2);
				e.printStackTrace();
			} catch (Error e) {
				error = true;
				Log.log(getClass(), "Unexpected error.", 2);
				e.printStackTrace();
			}
			Log.log(getClass(), "Thread has completed with error.", 2);
			thread = null;
		}
	}
	
	/**
	 * Prefix for cache files used by this bucket manager
	 */
	private String filePrefix;
	
	/**
	 * Files used by the cache
	 */
	private File[][] file = new File[2][FILE_POOL];
	
	/**
	 * Output streams used for the cache (using the files above)
	 */
	private DataRowOutputStream dros[][] = new DataRowOutputStream[2][FILE_POOL];
	
	/**
	 * Blocking function used by this bucket manager
	 */
	private BlockingFunction blockingFunction;
	
	/**
	 * Blocks of data in memory
	 */
	private Map[][] blocks;
	
	/**
	 * Mappings of buckets to files (so that all records 
	 * that fall into the same bucket end up in the same file on disk if caching is needed)
	 */
	private Map bucktesToFileId = new HashMap();
	
	/**
	 * Count of records added to the bucket manager (per data source) 
	 */
	private int[] sizes;
	
	/**
	 * Round robin index
	 */
	private int nextFileFromPool = 0;
	
	/**
	 * The buffered buckets.
	 */
	private Map buckets = new HashMap();
	
	/**
	 * Structures used to read buckets from the bucket manager,
	 * and to decide whether any data is stored in files
	 */
	private int[] usedFilesFromPool;
	private Iterator bucketIterator[] = new Iterator[FILE_POOL];
	private Iterator rowsIterator;
	private Bucket trashedBucket;
	private boolean dataInFile[] = new boolean[] {false, false};
	private Map[] read = new Map[2];
	
	/**
	 * Current buckets in memory - in the reading phase
	 */
	private List bucketsInMemory = new ArrayList();
	
	private boolean test[] = new boolean[FILE_POOL];
	private boolean cachingEnabled;
	
	/**
	 * Just to count statistics.
	 */
	private int addedRows = 0;
	
	/**
	 * The instance of buffer consumer thread that does actual job.
	 */
	private BufferConsumer thread = new BufferConsumer();
	
	/**
	 * Indication of BucketManager state
	 */
	private volatile boolean stopped= false;
	private volatile boolean done = false;
	private volatile boolean error = false;
	private volatile boolean completed = false;
	private volatile RJException exception = null;
	
	/**
	 * The input queue (adding phase)
	 */
	private ArrayBlockingQueue buffer = new ArrayBlockingQueue(Props.getInteger("intrathread-buffer"));
	
	/**
	 * The output queue (reading phase)
	 */
	private ArrayBlockingQueue bufferBuckets = new ArrayBlockingQueue(300);
	
	/**
	 * Indication of BufferConsumer being ready to read data
	 */
	private CountDownLatch counter = new CountDownLatch(1);
	
	/**
	 * Count of records per data source - for statistics
	 */
	private AtomicInteger leftSize = new AtomicInteger(0);
	private AtomicInteger rightSize = new AtomicInteger(0);
	
	/**
	 * Constructs a new bucket manager
	 * @param blockingFunction the function used for blocking
	 * @param cache true if data should be cached into files if there are too many records in memory
	 */
	public BucketManager(BlockingFunction blockingFunction, boolean cache) {
		this.cachingEnabled = cache;
		
		this.filePrefix = FILE_PREFIX + "_" + hashCode();
		
		Log.log(getClass(), "Buckets manager created. File prefix: " + filePrefix, 1);
		
		this.blockingFunction = blockingFunction;
		blocks = new Map[2][FILE_POOL];
		for (int i = 0; i < FILE_POOL; i++) {
			blocks[0][i] = new HashMap();
		}
		for (int i = 0; i < FILE_POOL; i++) {
			blocks[1][i] = new HashMap();
		}
		sizes = new int[2];
		for (int i = 0; i < FILE_POOL; i++) {
			file[0][i] = new File(filePrefix + "_" + i +  "_0.bin");
			file[0][i].deleteOnExit();
		}
		for (int i = 0; i < FILE_POOL; i++) {
			file[1][i] = new File(filePrefix + "_" + i + "_1.bin");
			file[1][i].deleteOnExit();
			test[i] = false;
		}
		usedFilesFromPool = new int[2];
		usedFilesFromPool[0] = 0;
		usedFilesFromPool[1] = 0;
		
		thread.setName("buckets-manager-thread");
		thread.start();
	}
	
	/**
	 * Constructor
	 * @param blockingFunction
	 */
	public BucketManager(BlockingFunction blockingFunction) {
		this(blockingFunction, true);
	}

	/**
	 * Adds record from left data source to bucket manager.
	 * Actually just computes the value of blocking function and schedules the operation.
	 * @param row
	 * @throws IOException
	 */
	public void addToBucketLeftSource(DataRow row) throws IOException {
		BufferItem item = new BufferItem();
		item.row = row;
		item.id = LEFT;
		item.hash = blockingFunction.hash(row, LEFT);
		if (item.hash == null) {
			item.hash = "";
		}
		leftSize.incrementAndGet();
		try {
			buffer.put(item);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Adds record from right data source to bucket manager.
	 * Actually just computes the value of blocking function and schedules the operation.
	 * @param row
	 * @throws IOException
	 */
	public void addToBucketRightSource(DataRow row) throws IOException {
		BufferItem item = new BufferItem();
		item.row = row;
		item.id = RIGHT;
		item.hash = blockingFunction.hash(row, RIGHT);
		if (item.hash == null) {
			item.hash = "";
		}
		rightSize.incrementAndGet();
		try {
			buffer.put(item);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The function reads next bucket. Has to be called after addingCompleted was called.
	 * @return
	 * @throws IOException
	 * @throws RJException
	 */
	public synchronized DataRow[][] getBucket() throws IOException, RJException {
		try {
			if (completed ) {
				return null;
			}
			while (true) {
				if (error) {
					synchronized(this) {
						throw exception;
					}
				}
				DataRow[][] bucket = (DataRow[][]) bufferBuckets.poll(100, TimeUnit.MILLISECONDS);
				if (bucket == null) {
					continue;
				} else if (bucket.length == 0) {
					completed = true;
					break;
				} else {
					resetRows(bucket);
					return bucket;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	private void resetRows(DataRow[][] bucket) {
		for (int i = 0; i < bucket.length; i++) {
			for (int j = 0; j < bucket[i].length; j++) {
				RowUtils.resetRow(bucket[i][j]);
			}
		}
	}

	/**
	 * Requests the consumer thread to stop processing (e.g., when stopping working on the linkage)
	 */
	public void stopProcessing() {
		stopped = true;
	}
	
	/**
	 * Indicates the fact that all the records have been added, and now it will be time to read.
	 * May take some time as it waits for the consumer thread to being ready.
	 * @throws IOException
	 */
	public void addingCompleted() throws IOException {
		done = true;
		try {
			counter.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private DataRow[] getBucket(int id, Bucket b) {
		List bucket = (List)read[id].get(b);
		if (bucket == null) {
			bucket = new ArrayList();
		}
		return (DataRow[]) bucket.toArray(new DataRow[] {});
	}

	/**
	 * Closes the streams to cache files (called by the consumer)
	 * @throws IOException
	 * @throws RJException
	 */
	private void closeStreams() throws IOException, RJException {
		if (dataInFile[0]) {
			for (int i = 0; i < FILE_POOL; i++) {
				if (dros[0][i] != null) dros[0][i].close();
			}
		}
		if (dataInFile[1]) {
			for (int i = 0; i < FILE_POOL; i++) {
				if (dros[1][i] != null) dros[1][i].close();
			}
		}
	}
	
	/**
	 * Cleans up after using bucket manager. Clears some maps from memory and closes/deletes temporary cache files.
	 * @throws IOException
	 * @throws RJException
	 */
	public void cleanup() throws IOException, RJException {
		closeStreams();
		if (file != null) {
			for (int i = 0; i < file.length; i++) {
				if (file[i] == null) {
					continue;
				}
				for (int j = 0; j < file[i].length; j++) {
					if (file[i][j] != null) {
						file[i][j].delete();
					}
				}
			}
		}
		
		for (int i = 0; i < blocks.length; i++) {
			for (int j = 0; j < blocks[i].length; j++) {
				blocks[i][j].clear();
			}
		}
	}

	private void readBucketsFromMem(int id) {
		read[id] = blocks[id][usedFilesFromPool[id]];
		usedFilesFromPool[id]++;
	}

	private void readBucketsFromFile(int id) throws FileNotFoundException, IOException, RJException {
		DataRowInputStream dris;
		try {
			dris = new DataRowInputStream(createInputStream(file[id][usedFilesFromPool[id]]));
		} catch (FileNotFoundException e) {
			System.out.println("File " + file[id][usedFilesFromPool[id]].getAbsolutePath() + " has not been found.");
			return;
		}
		DataRow row;
		while ((row = dris.readDataRow()) != null) {
			Bucket b = new Bucket(Integer.parseInt(row.getProperty(BUCKET_MARKER)));
			List list = (List) read[id].get(b);
			if (list == null) {
				list = new ArrayList();
				read[id].put(b, list);
			}
			list.add(row);
		}
		usedFilesFromPool[id]++;
		dris.close();
	}

	/**
	 * Called by the consumer thread. Adds the row to bucket, chooses destination file for the record (based on bucket) 
	 * and possibly flushes data to external cache files if needed.
	 * @param id
	 * @param row
	 * @param bucket
	 * @return
	 * @throws IOException
	 */
	private synchronized int addToBucket(int id, DataRow row, String bucket) throws IOException {
		addedRows++;
		sizes[id]++;
		
		if (bucket == null) {
			return 0;
		}
		Bucket b = (Bucket)buckets.get(bucket); //new Bucket(new String[] {bucket});
		if (b == null) {
			b = new Bucket(new String[] {bucket});
			buckets.put(bucket, b);
		}
		if (id == LEFT) {
			b.leftRecordAdded();
		} else {
			b.rightRecordAdded();
		}
		Integer pool = (Integer) bucktesToFileId.get(b);
		if (pool == null) {
			pool = new Integer(nextFileFromPool);
			bucktesToFileId.put(b, pool);
			nextFileFromPool = (nextFileFromPool + 1) % FILE_POOL;
		}

		int poolId = pool.intValue();
		if (blocks[id][poolId].containsKey(b)) {
			List l = (List) blocks[id][poolId].get(b);
			l.add(row);
		} else {
			List l = new ArrayList();
			l.add(row);
			blocks[id][poolId].put(b, l);
		}
		if (cachingEnabled) {
			trashIfNeeded(id, false);
		}
		return poolId;
	}

	/**
	 * Saves the data from memory into cache files.
	 * @param id
	 * @param force
	 * @throws IOException
	 */
	private void trashIfNeeded(int id, boolean force) throws IOException {
		if (sizes[id] > BLOCK_TRASH_FACTOR || force) {
			
			dataInFile[id] = true;
			for (int i = 0; i < FILE_POOL; i++) {
				bucketIterator[i] = null;
				DataRow row = getNext(id, i);
				if (row == null) {
					continue;
				}
				if (dros[id][i] == null) {
					dros[id][i] = new DataRowOutputStream(row.getSourceName(), row.getRowModel(), createOutputStream(file[id][i]));
				}
				do {
					dros[id][i].writeDataRow(row);
				} while ((row = getNext(id, i)) != null);
				blocks[id][i] = new HashMap();
			}
			sizes[id] = 0;
		}
	}

	private DataRow getNext(int id, int poolId) {
		if (bucketIterator[poolId] == null) {
			bucketIterator[poolId] = blocks[id][poolId].keySet().iterator();
		}
		if (rowsIterator == null || !rowsIterator.hasNext()) {
			if (!bucketIterator[poolId].hasNext()) {
				return null;
			}
			trashedBucket = (Bucket) bucketIterator[poolId].next();
			rowsIterator = ((List)blocks[id][poolId].get(trashedBucket)).iterator();
		}
		DataRow row = (DataRow) rowsIterator.next();
		row.setProperty(BUCKET_MARKER, trashedBucket.hashCode() + "");
		return row;
	}
	
	private InputStream createInputStream(File file) throws FileNotFoundException, IOException {
		return new InflaterInputStream(new FileInputStream(file), new Inflater(), 4096);
	}

	private OutputStream createOutputStream(File file) throws FileNotFoundException, IOException {
		return new DeflaterOutputStream(new FileOutputStream(file), new Deflater(Deflater.BEST_SPEED), 4096);
	}

	/**
	 * The used hashing function
	 * @return
	 */
	public BlockingFunction getHashingFunction() {
		return this.blockingFunction;
	}
	
	/**
	 * Resets the bucket manager. The bucket manager will be able to accept new work...
	 */
	public void reset() {
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if (dataInFile[0]) {
			for (int j = 0; j < blocks[0].length; j++) {
				blocks[0][j].clear();
			}
		}
		if (dataInFile[1]) {
			for (int j = 0; j < blocks[1].length; j++) {
				blocks[1][j].clear();
			}
		}
		
		usedFilesFromPool[0] = 0;
		usedFilesFromPool[1] = 0;
		bucketsInMemory.clear();
		bufferBuckets.clear();
		stopped = false;
		completed = false;
		thread = new BufferConsumer();
		thread.setName("buckets-manager-thread");
		thread.start();
	}
	
	/**
	 * Not so nice, but makes sure the temporary data is deleted, and memory cleaned.
	 */
	protected void finalize() throws Throwable {
		cleanup();	
		Log.log(getClass(), "Temporary files with prefix " + filePrefix + " deleted.");
		super.finalize();
	}
	
	/**
	 * Gets number of buckets in the system.
	 * @return
	 */
	public long getNumberOfBuckets() {
		return bucktesToFileId.size();
	}
	
	/**
	 * Returns total number of comparisons that will have to be performed by 
	 * BlockingJoin using this bucket manager - for progress reporting
	 * @return
	 */
	public long getTotalNumberOfComparisons() {
		long comps = 0;
		for (Iterator keys = bucktesToFileId.keySet().iterator(); keys.hasNext();) {
			Bucket bucket = (Bucket) keys.next();
			comps += bucket.getLeftRowsCount() * bucket.getRightRowsCount();
		}
		return comps;
	}

	/**
	 * Gets number of rows added to the bucket manager - for statistical purposes.
	 * @return
	 */
	public int getNumberOfRows() {
		return addedRows ;
	}
}
