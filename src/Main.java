import java.io.*;
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
			RandomAccessFile configuration = new RandomAccessFile("config.txt","rw");
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
		while(back.startsWith("#") | back.replaceAll("\\s", "").matches("")){
			back = config.readLine();	
		}
		if(back.contains("#")){
			int index = back.indexOf("#");
			back = back.substring(0, index);
		}
		back = back.replaceAll("\\s+"," ");
		return back;
}
	
	public HashMap<Integer,ArrayList<Integer>> generateST(){
		HashMap<Integer,ArrayList<Integer>> spanningTree = new HashMap<Integer,ArrayList<Integer>>();
		ArrayList<Integer> sum =new ArrayList<Integer>();
		sum.add(0);
	    for(int i=0;i<nodeNumber;i++){	 //initialize spanningTree
	    	spanningTree.put(i, new ArrayList<Integer>());
	    }
	    
		for(int i=0;i<nodeNumber;i++){
			for(int j:neighborName.get(i)){
				if(!sum.contains(j)){
					spanningTree.get(i).add(j);
					spanningTree.get(j).add(i);
					sum.add(j);
				}
			}
		}
		
//		for(int i=0;i<spanningTree.size();i++){
//			System.out.println(spanningTree.get(i));
//		}
		
		return spanningTree;
	}
	
	public static void main(String[] args) {		
		System.out.println(Arrays.toString(args));
		
		Main main = new Main();
		main.parseConfig();
		main.generateST();

		//Node node = new Node();

	}
}
