package com.couchbase.support;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.bucket.BucketManager;
import com.couchbase.client.java.cluster.BucketSettings;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.document.Document;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DesignDocumentAlreadyExistsException;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.View;
import com.couchbase.client.java.view.View;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.List;
import java.util.concurrent.TimeUnit;


// Brian Williams
// June 19, 2015
// Built with Couchbase Java Client 2.1.3

public class ExpirationTimeAnalyzer {

	static int SCREENCOLUMNS = 80;
	 
	public static void main(String[] args) {

		printCenteredBanner("Welcome to Expiration Time Analyzer");

		String HOSTNAME           = "192.168.0.1";   // Replace with your own
		String USERNAME           = "Administrator";
		String PASSWORD           = "password";
		String BUCKETNAME         = "histogramtest";
		String DESIGNDOCUMENTNAME = "dd1";
		String VIEWNAME           = "alldocs";
		String MAPFUNCTION        = "function (doc, meta) { emit(meta.id, meta.expiration); }";
//		Stale  staleValue         = Stale.UPDATE_AFTER;
		Stale  staleValue         = Stale.FALSE;
		
		Cluster cluster = CouchbaseCluster.create(HOSTNAME);
		Bucket  bucket  = cluster.openBucket(BUCKETNAME);
	
		try {
			View    v       = DefaultView.create(VIEWNAME, MAPFUNCTION);
			List<View> listOfViews = new ArrayList<View>();
			listOfViews.add(v);
			DesignDocument dd = DesignDocument.create(DESIGNDOCUMENTNAME, listOfViews);
			BucketManager bm = bucket.bucketManager();
			bm.insertDesignDocument(dd);
		}
		catch (DesignDocumentAlreadyExistsException ddaee) {
			System.out.println("The design document already exists");
		}
		catch (Exception e) {
			System.exit(1);
		}
		
		logMessage("The design doc and view have been created.  Sleeping 10 seconds.");
		
		try { Thread.sleep(10000); } catch (Exception e) { e.printStackTrace(); System.exit(1); }

		logMessage("About to issue the query on the view.");
	
		ViewResult result = bucket.query(ViewQuery.from(DESIGNDOCUMENTNAME, VIEWNAME).stale(staleValue));

		int totalRows = result.totalRows();
		logMessage("totalRows is " + totalRows);
		
		int totalResults = 0;
		JsonDocument jsonDocument = null;
		int documentExpiry = 0;
		int expiryFromNow = 0;
		long timeNow = System.currentTimeMillis() / 1000;
		
		int numBuckets           = 10;
		int maxValue             = 1000000;
		int largestItemCount     = 10;
		int terminalMaximumWidth = 80;
		int numWithExpiryException = 0;
		int numWithNullDocument    = 0;
		
		long timeDelta = 0;
		
		SimpleHistogram histogram = new SimpleHistogram(numBuckets, maxValue, largestItemCount, terminalMaximumWidth);
		
		for (ViewRow row : result) {

			jsonDocument = row.document();

			if (jsonDocument == null) {
				numWithNullDocument++;
			}
			else {
				try {
					documentExpiry = (Integer) row.value();
					timeDelta = documentExpiry - timeNow;
					
					logMessage("timeNow:" + timeNow + " documentExpiry: " + documentExpiry + " timeDelta: " + timeDelta);

					histogram.processValue(timeDelta);
				}
				catch (Exception e) {
					e.printStackTrace();
					numWithExpiryException++;
				}	
			}
				
		    totalResults++;
		}

		logMessage("numWithNullDocument:    " + numWithNullDocument);
		logMessage("numWithExpiryException: " + numWithExpiryException);
		logMessage("totalResults:           " + totalResults);
		
		bucket.close();
		cluster.disconnect();
		
		histogram.display();
		
		printCenteredBanner("Now leaving Expiration Time Analyzer");
		
	}

	public static void printDecoration(int c, String s) {
		for (int i = 0; i < c; i++) { System.out.print(s); }
	}

	public static void printCenteredBanner(String s) {
		int numDecorations = ((SCREENCOLUMNS - (s.length() + 2)) / 2);
		printDecoration(numDecorations,"=");
		System.out.print(" " + s + " ");
		printDecoration(numDecorations,"=");		
		System.out.println();
	}
	
	static void logMessage(String s) {
		System.out.println("=== " + s + " ===");
	}

} // ExpirationTimeAnalyzer





class SimpleHistogram {

	// Displays a terminal-width-scaled histogram like so
	
	//    0 -     9 :    17 : .....
	//   10 -    19 :   259 : ..............................................................
	//   20 -    29 :    42 : ...........
	//   30 -    39 :    13 : ....
	//   40 -    49 :     3 : .
	//   50 -    59 :     4 : .
	//   60 -    69 :     3 : .
	//   70 -    79 :     1 : .
	//   80 -    89 :     1 : .
	//   90 -    99 :     0 : 
	//Items not counted:3
	
	static int _terminalMaximumWidth = 80;
	
	// This is the number of buckets that you want to see in your histogram.
	
	static int _numBuckets = 10;

	// This is supposed to be the largest value that you would ever expect to see.	
	// If I am measuring milliseconds and the largest value I expect to see is
	// 100 milliseconds, then _maxValue would be 100.  
	static int _maxValue = 100;

	// The bucket width ( for example, lets say you had a bucket counting the number of times
	// a query took between 0 and 9 milliseconds ) is computed from _maxValue and _numBuckets.
	
	// So if you wanted to have values between say 0 and 100 milliseconds, and divide these
	// up into 10 buckets, then the first bucket would be 0 - 9, the second would be
	// 10 - 19, and so forth, up until 90 - 99.  In that cause _numBuckets would be 10
	// and _maxValue would be 100.
	
	// If _maxValue is 100 and _numBuckets is 20 then bucketWidth is 5, meaning
	// it holds the values that fell between 0 and 4 inclusively.
	
	int[] _buckets        = null;	// each bucket holds a count of items 
	int _bucketWidth      = 0;		// will be computed in the constructor
	int _largestItemCount = 0;		// used for scaling to terminal width
	int _itemsNotCounted  = 0;

	SimpleHistogram(int numBuckets, int maxValue, int largestItemCount, int terminalMaximumWidth) {
		
		_numBuckets = numBuckets;
		_maxValue   = maxValue;
		_terminalMaximumWidth = terminalMaximumWidth;
		
		// Used for scaling the output for a given terminal width.
		// This is the largest number of items that you would expect to see in a particular
		// bucket.  If this is smaller than the number of columns passed to display() then
		// more than one dot might represent an item in the bucket.  Otherwise more than
		// one item might be represented by a dot.
		_largestItemCount = largestItemCount;   
		// If you have 80 columns and 62 are available to work with, and you set
		// largest value to 5, then each item in a bucket will be represented by 12
		// dots.

		_buckets = new int[_numBuckets];
		_bucketWidth = _maxValue / _numBuckets;
		System.out.println("I have " + _numBuckets + " with a width of " + _bucketWidth);

		// initialize, set all counters to zero
		for (int i = 0; i < _numBuckets; i++) {
			_buckets[i] = 0;
		}

		// display();  // Print the empty histogram, with a terminal width of 80

	}	// end constructor

	// Given a bucket number, check to see if that bucket number is valid,
	// and if it is, then look at the old value ( counter ) in the bucket,
	// compute the new value, and then store the new value.  At the same time,
	// if the new value is larger than the largest known value, then remember
	// the new largest value.
		
	void incrementBucket(int bucketNumber) {
		if ((bucketNumber >= 0) && (bucketNumber < _numBuckets)) {
			int oldValue = _buckets[bucketNumber];
			int newValue = oldValue + 1;
			_buckets[bucketNumber] = newValue;
			//System.out.println("Bucket " + bucketNumber + " was " + oldValue + " now " + newValue);
			if (newValue > _largestItemCount) { _largestItemCount = newValue; }
		}
		else {
			// System.err.println("Cannot increment bucket number " + bucketNumber);
			_itemsNotCounted++;
		}
	}

	// Given a value, determine which bucket the given value should fall into.
	// Then, call incrementBucket() to increase the counter for that bucket.
	
	void processValue(long currVal) {
		int bucketNum = (int) currVal / _bucketWidth;
		// System.out.println("Incrementing bucket " + bucketNum);
		incrementBucket(bucketNum);
	}

	// display() causes the histogram to be printed immediately, with the specified 
	// terminal width.  
	
	// As an example, here the terminal width was 80.  The columns on the left take
	// up about 25 characters, leaving 62 characters for the dots.  The largest 
	// number of items in a bucket is 207.  So we calculate that each dot on the
	// screen represents about 0.3 of an item.
	
	//	largestVal is 207.0 spaceToUse is 62.0 dotsPerVal is 0.29951692
	//    0 -     9 :    14 : .....
	//   10 -    19 :   207 : ..............................................................
	//	(etc)
	
	void display() {

		int numColumns = _terminalMaximumWidth;
		
		int startVal, endVal, countForBucket;
	
		String FORMATTING_STRING       = "%6d - %6d : %6d : ";
		float spaceToUse = numColumns - (FORMATTING_STRING.length());

		String FORMATTING_STRING_TOTAL = "       Total    : %6d : ";
		int totalItems = 0;
		
		// Calculate how many items each screen column should represent
		float largestVal = _largestItemCount;			
		float dotsPerVal = spaceToUse / largestVal;
		System.out.println("largestVal is " + largestVal + " spaceToUse is " + spaceToUse + " dotsPerVal is " + dotsPerVal);

		for (int eachRow = 0; eachRow < _numBuckets; eachRow++) {
			countForBucket = _buckets[eachRow];		
			startVal = (eachRow * _bucketWidth);
			endVal = ((eachRow+1) * _bucketWidth) - 1;
			System.out.printf(FORMATTING_STRING, startVal, endVal, countForBucket);

			if (countForBucket != 0) {
				for (int x = 0; x < (dotsPerVal * countForBucket); x++) {
					System.out.print(".");
				}
			}

			totalItems += countForBucket;
			
			System.out.println();

		} // for each row
		
		System.out.printf(FORMATTING_STRING_TOTAL, totalItems);
		System.out.println();
		
		System.out.println("Items not counted:" + _itemsNotCounted);

	} // display()

} // end of class SimpleHistogram

// EOF
