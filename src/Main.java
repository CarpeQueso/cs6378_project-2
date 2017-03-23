import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {
/**
 * configuration parsing
 */
	private int nodeNumber;
	private int minPeractive;
	private int maxPeractive;
	private int minSendDelay;
	private int snapshotDelay;
	private int maxNumber;
	private HashMap<Integer,String[]> hostInfo =new HashMap<Integer,String[]>();  //stores nodeID,hostname,port for node i
	private HashMap<Integer,ArrayList<Integer>> neighborName =new HashMap<Integer,ArrayList<Integer>>(); //stores neighbours with node i
	
	public void parseConfig(){
		try {
			RandomAccessFile configuration = new RandomAccessFile("/home/012/j/ja/jac161530/CS6378/cs6378_project-2/config.txt","rw");
			String[] host = read(configuration).split(" ");
			nodeNumber = Integer.parseInt(host[0]);
			minPeractive = Integer.parseInt(host[1]);
			maxPeractive = Integer.parseInt(host[2]);
			minSendDelay = Integer.parseInt(host[3]);
			snapshotDelay = Integer.parseInt(host[4]);
			maxNumber = Integer.parseInt(host[5]);
			
			for(int i=0;i<nodeNumber;i++){
				String[] a = read(configuration).split(" ");
				hostInfo.put(i,a);
			}
			
			for(int i=0;i<nodeNumber;i++){
				String[] b = read(configuration).split(" ");
				ArrayList<Integer> c = new ArrayList<Integer>(); 
				int j = 0;
				for(String a : b){
					c.add(Integer.parseInt(a));
					j++;
				}
				neighborName.put(i,c);
			}
			
//			for(int i=0;i<nodeNumber;i++){
//				System.out.println(neighborName.get(i));
//			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String read(RandomAccessFile config) throws IOException{
	//this method ignores all annotations
		String back;
		back = config.readLine();
		while(back.startsWith("#") || back.replaceAll("\\s", "").matches("")){
			back = config.readLine();	
		}
		if(back.contains("#")){
			int index = back.indexOf("#");
			back = back.substring(0, index);
		}
		back = back.replaceAll("\\s+"," ");
		return back;
}
	
	public HashMap<Integer,Integer> generateST(){
		HashMap<Integer,Integer> spanningTree = new HashMap<Integer,Integer>(); 
//for spanningTree<i,j>,i represents node i, and j represents i's direct parent,node 0 is always the root;
		ArrayList<Integer> sum =new ArrayList<Integer>();
		sum.add(0);
//	    for(int i=0;i<nodeNumber;i++){	 //initialize spanningTree
//	    	spanningTree.put(i, new ArrayList<Integer>());
//	    }
	    
		for(int i=0;i<nodeNumber;i++){
			for(int j:neighborName.get(i)){
				if(!sum.contains(j)){
					spanningTree.put(j, i);
//					spanningTree.get(i).add(j);
//					spanningTree.get(j).add(i);
					sum.add(j);
				}
			}
		}

			System.out.println(spanningTree);
		
		return spanningTree;
	}

	public void run(String arg) {
		parseConfig();
		HashMap<Integer, Integer> spanningTree = generateST();

		int id = Integer.parseInt(arg);
		String hostname = hostInfo.get(id)[1];
		int port = Integer.parseInt(hostInfo.get(id)[2]);
		int parentNodeId = 0;
		if (id == 0) {
			parentNodeId = -1;
		} else {
			parentNodeId = spanningTree.get(id);
		}

		System.out.println(id + ":" + parentNodeId);

		Node node = new Node(id, hostname, port, nodeNumber, minPeractive, maxPeractive, minSendDelay, snapshotDelay, maxNumber, parentNodeId);
		ArrayList<Integer> neighborIds = neighborName.get(id);
		for (Integer neighborId : neighborIds) {
			String neighborHostname = hostInfo.get(neighborId)[1];
			int neighborPort = Integer.parseInt(hostInfo.get(neighborId)[2]);

			node.addNeighbor(neighborId, neighborHostname, neighborPort);
		}

		node.startServer();
				
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			System.err.println("Thread sleep before server start was interrupted");
		}

		node.begin();
		node.stopServer();
	}

	
	public void printParsing(){ //test for the parseConfig method
		System.out.println( nodeNumber+" "+ minPeractive+" "+maxPeractive+" "+
				minSendDelay+" "+snapshotDelay+" "+maxNumber);
		for(int i=0;i<nodeNumber;i++){
			System.out.print("host info for node ");
			for(int j=0;j<hostInfo.get(i).length;j++){
				System.out.print(hostInfo.get(i)[j]+" ");
			}
			System.out.println();
		}
		System.out.println("neighbor for each node:"+neighborName);
	}
	


	public static void main(String[] args) {		
		new Main().run(args[0]);
	}
}
