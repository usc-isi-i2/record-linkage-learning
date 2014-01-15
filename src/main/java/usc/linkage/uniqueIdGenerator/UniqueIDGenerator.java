package usc.linkage.uniqueIdGenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 
 * @author Chen Wang
 * 
 * 	UniqueIDGenerator is used to create unique id for identifying different linkage result files.
 *
 */

public class UniqueIDGenerator {
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS");
	private static AtomicLong randomNumber = new AtomicLong(0);
	
	public static String getUniqueID(){
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append(dateFormat.format(new Date()));
		strBuilder.append("_");
		strBuilder.append(randomNumber.addAndGet(1));
		if(randomNumber.get() >= 999){
			randomNumber.set(0);
		}
		return strBuilder.toString();
	}	
}
