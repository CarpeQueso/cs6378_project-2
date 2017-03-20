import java.io.*;
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
	private HashMap<Integer,String[]> hostInfo =new HashMap<Integer,String[]>();  //stores nodeID,hostname,port for node i
	private HashMap<Integer,String[]> neighborName =new HashMap<Integer,String[]>(); //stores neighbours with node i
	
	public void parseConfig(){
		try {
			RandomAccessFile configuration = new RandomAccessFile("config.txt","rw");
			String[] host = read(configuration).split(" ");
			nodeNumber = Integer.parseInt(host[0]);
			minPeractive = Integer.parseInt(host[1]);
			maxPeractive = Integer.parseInt(host[2]);
			minSendDelay = Integer.parseInt(host[3]);
			snapshotDelay = Integer.parseInt(host[4]);
			
			for(int i=0;i<nodeNumber;i++){
				String[] a = read(configuration).split(" ");
				hostInfo.put(i,a);
			}
			
			for(int i=0;i<nodeNumber;i++){
				String[] b = read(configuration).split(" ");
				neighborName.put(i,b);
			}
			
//			for(int i=0;i<nodeNumber;i++){
//				for(int j=0;j<neighborName.get(i).length;j++){
//					System.out.print(neighborName.get(i)[j]);
//				}
//				System.out.println( );
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
	
	public static void main(String[] args) {		
		
		
	}
}
