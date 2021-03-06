package deviceLayer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import controlLayer.libraryCode.Constants;

// Request and Response Messages, as well as Failed Requests are all manipulated through this class
public class MessageQueueWithPriorities {

    private PriorityQueue<deviceLayer.Request>  requestQueue;	// Queue used for Request Messages
    private ConcurrentLinkedQueue<deviceLayer.Request>  failureQueue;	// Queue used for Failed Request Messages
    private ConcurrentLinkedQueue<deviceLayer.Response> responseQueue;	// Queue used for Response Messages
    private int requestMaxNumber = 0;									// for testing reasons: maximun number of pending Requests in Request Queue
    
	/** A random number generator initialized with a seed (used in simulations including priorities) */
	public static final Random random = new Random(System.currentTimeMillis()); 
	
	public static int  totalHighPriorityRequests   = 0;
	public static int  totalLowPriorityRequests    = 0;
	public static int  totalNormalPriorityRequests = 0;
    
	FileWriter fstream;
	BufferedWriter resultFile;
    
	String deviceName = "";
	
	class PrioritiesComparator implements Comparator<Request>{
		 
	    public int compare(Request req1, Request req2){    
	       
	        long req1Priority = req1.priority;        
	        long req2Priority = req2.priority;
	       
	        if(req1Priority < req2Priority)
	        	return 1;
	        else if(req1Priority == req2Priority)
	        	return 0;
	        else
	        	return -1;
	   
	    }
	 
	}
	
	PrioritiesComparator prioritiesComparator = new PrioritiesComparator();
	
	
	// default Constructor
    public MessageQueueWithPriorities(String deviceName){
		requestQueue 		= new PriorityQueue<Request>(10, prioritiesComparator);
		failureQueue		= new ConcurrentLinkedQueue<Request>();
		responseQueue 		= new ConcurrentLinkedQueue<Response>();
		
		try{
			if(deviceName != "" && !deviceName.contains("ateway")){
				String fileName = "tinyRequestQueueDevice" + deviceName + ".txt";
				fstream = new FileWriter(fileName, true);
				resultFile = new BufferedWriter(fstream);
			}
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
    
    public void closeRequestQueueSizeFile(){
    	try {
			resultFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
    public int getMaxPendingRequestNum(){
    	return requestMaxNumber;
    }
    
    /* retrieves next Request Message from Request Queue, in a FIFO manner */
	public Request getNextRequestMessage(){
		// return request message which is in head of Queue, null if Queue is empty
		Request r = requestQueue.peek();
		r.priority = 10000;
		return r;
	}
	
	/* retrieves next failed Request Message from Failed Requests Queue, in a FIFO manner */
	public Request getNextFailedRequestMessage(){
		// return request message which is in head of Queue, null if Queue is empty
		return failureQueue.peek();
	}
	
	/* retrieves next Response Message from Response Queue, in a FIFO manner */
	public Response getNextResponseMessage(){
		// return response message which is in head of Queue, null if Queue is empty
		Response resp = responseQueue.peek();
		return resp;
		
	}
	
	/* checks if Request Queue has a pending Request Message  */
	public boolean hasRequestMessage(){
		if(requestQueue.isEmpty() == true)
			return false;
		else
			return true;
	}
	
	/* checks if Failed Requests Queue has a pending Request Message  */
	public boolean hasFailedRequestMessage(){
		if(failureQueue.isEmpty() == true)
			return false;
		else
			return true;
	}
	
	/* checks if Response Queue has a pending Response Message  */
	public boolean hasResponseMessage(){
		if(responseQueue.isEmpty() == true)
			return false;
		else
			return true;
	}
	
	/* adds a Request Message in Request Queue in a FIFO way */
	public void addRequestMessage(deviceLayer.Request r){
		
		// assign probabilistically a priority to the new request message
		int randomPriority = Math.abs(random.nextInt()) % 100;
		if(randomPriority < Constants.PROBABILITY_HIGH_PRIORITY){
			r.priority 		   = Constants.HIGH_PRIORITY;
			r.dequeuePriority  = Constants.HIGH_PRIORITY;
			r.priorityCategory = "High";
			totalHighPriorityRequests++;
		}
		else if(randomPriority < Constants.PROBABILITY_HIGH_PRIORITY + Constants.PROBABILITY_LOW_PRIORITY){
			r.priority         = Constants.LOW_PRIORITY;
			r.dequeuePriority  = Constants.LOW_PRIORITY;
			r.priorityCategory = "Low";
			totalLowPriorityRequests++;
		}
		else{
			r.priority 		   = Constants.NORMAL_PRIORITY;
			r.dequeuePriority  = Constants.NORMAL_PRIORITY;
			r.priorityCategory = "Normal";
			totalNormalPriorityRequests++;
		}
		
		// increase all priorities at the priority queue by 1 (this is to avoid starvation of low-priority requests)
		Iterator<Request> requests = requestQueue.iterator();
		while(requests.hasNext()){
			Request req = requests.next();
			req.priority = req.priority + Constants.INC_PRIORITY_VALUE;
			req.dequeuePriority = req.dequeuePriority + Constants.INC_PRIORITY_VALUE;
		}
	
		// add new request to the queue
		requestQueue.add(r);
		
		if(requestQueue.size() > requestMaxNumber){
			requestMaxNumber = requestQueue.size();
		}
		System.out.println("REQUEST QUEUE CURRENT NUMBER:" + requestQueue.size());
		
		// writing request queue size to file
		try {
			resultFile.append( ("" + requestQueue.size()));
			resultFile.newLine();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// report the exact time the request enqueues in the Request Queue
		r.enqueueTime = System.currentTimeMillis();
		// report the exact queue length when the request enqueues in the Request Queue
		r.requestQueueSize = requestQueue.size();
	}
	
	/* adds a failed Request Message in failed Requests Queue in a FIFO way */
	public void addFailedRequestMessage(deviceLayer.Request r){
		failureQueue.add(r);
	}
	
	/* adds a Response Message in Response Queue in a FIFO way */
	public void addResponseMessage(deviceLayer.Response r,  long id){
		System.err.println("DBG: ADDED NEW RESPONSE (" + r.getServiceName() +") IN REQUEST QUEUE");
		responseQueue.add(r);
		
		// writing request queue size to file
		try {
			int reQueueNewSize = requestQueue.size()-1;
			resultFile.append( ("" + reQueueNewSize));
			resultFile.newLine();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// report the exact time the request dequeues from the Request Queue
		try {
			long responseTime = System.currentTimeMillis();	 
			
			// write information regarding timing of request/response at the tinyRequestQueueTimeDelay.txt file
			if(!requestQueue.isEmpty()){
				long enqueueTime = getNextRequestMessage().enqueueTime;
				long dequeueTime = getNextRequestMessage().dequeueTime;
				long queueTime   = dequeueTime - enqueueTime;
				long reqRespTime = responseTime - dequeueTime;
				Devices.reqQueueTimesFile.append("" + getNextRequestMessage().priorityCategory + " " + getNextRequestMessage().dequeuePriority + " " + getNextRequestMessage().requestQueueSize + " " + 
												queueTime + " " + reqRespTime);
				Devices.reqQueueTimesFile.newLine();
			}
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/* deletes a Request Message from Request Queue */
	public void deleteRequestMessage(deviceLayer.Request r){
		//requestQueue.remove(r);
		requestQueue.poll();
		System.out.println("REQUEST QUEUE CURRENT NUMBER:" + requestQueue.size());
	}

	/* deletes a failed Request Message from failed Requests Queue */
	public void deleteFailedRequestMessage(deviceLayer.Request r){
		failureQueue.remove(r);
	}
	
	/* deletes a Response Message from Response Queue */
	public void deleteResponseMessage(deviceLayer.Response r){
		responseQueue.remove(r);
	}
	
}
