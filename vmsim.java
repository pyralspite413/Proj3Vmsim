import java.util.*;
import java.lang.Math;
import java.io.*;
	public class vmsim{
		
		
	@SuppressWarnings("unchecked") 
	public static void main(String[] args) throws Exception{
		//KB = 2^10 =1024
		final int kiloByte= 1024; 
		final int addSize=32;
		
		//check to ensure proper args
		if(args.length<9){
			System.out.println("must include command arguments in format:"+
			"-a <opt|lru> –n <numframes> -p <pagesize in KB> -s <memory split> <tracefile>");
			System.exit(0);
		}
		
		
		int mode=0; //base case lru
		int numframes; //number of frames 
		int pagesize;  //size of a page
		String memsplit; //memory split
		String tracefile; //string containing trace file name.
		
		String alg=args[1].toUpperCase();
		if(args[1].equals("opt"))mode=1;
		
		numframes=Integer.parseInt(args[3]);
		
		int frameNumSize= log2(numframes); 
		
		pagesize=Integer.parseInt(args[5]);
		
		pagesize=pagesize*kiloByte;
		
		memsplit=args[7];
		String[] splits=memsplit.split(":");
		int proc0Split=Integer.parseInt(splits[0]);
		int proc1Split=Integer.parseInt(splits[1]);
		int totSplit=proc0Split+proc1Split;
		int proc0Frames=(numframes/totSplit)*proc0Split;
		int proc1Frames=(numframes/totSplit)*proc1Split;
		tracefile=args[8];
		
		
		
		//num frames determines how many addresses can be stored at once (size of array)
		//page size=
		//address offset = num of bits in page size exponent ie 2^12 4Kb page size= 12 bit offset 
		int offset=log2(pagesize);
		int[] stats=new int[3];
		if(mode==0) 
			stats=LRU(pagesize,numframes, proc0Frames,proc1Frames,offset,tracefile);
	
		if(mode==1) 
			stats=opt(pagesize,numframes, proc0Frames,proc1Frames,offset,tracefile);
		System.out.println("Algorithm: "+alg);
		System.out.println("Number of frames: "+numframes);
		System.out.println("Page size: "+(pagesize/1024)+" KB");
		System.out.println("total memory accesses: "+stats[0]);
		System.out.println("total page faults: "+ stats[1]);
		System.out.println("total writes to disk: "+stats[2]);
		
		
		}
		@SuppressWarnings("unchecked") 
		private static boolean isDirty(LinkedList<Boolean> list, int index){
			if(list.get(index).booleanValue()==true){
				return true;
			}
			else return false;
		}
		
		@SuppressWarnings("unchecked") 
		//helper method determines size of log base 2 of a number - used for offset
		public static int log2(int n){
			int res=(int) (Math.log(n)/Math.log(2));
			return res;
		}
		@SuppressWarnings("unchecked") 
		public static void printList(LinkedList<Long> list, LinkedList<Boolean> dlist){
			for(int i=0;i<list.size();i++){
				System.out.print("index:"+i+":"+Long.toHexString(list.get(i)));
				if(isDirty(dlist,i)==true){
					System.out.print("*");
				}
				System.out.println("");
			}
			
		}
		@SuppressWarnings("unchecked") 
		public static int indexOf(LinkedList<Long> list, long address){
			int i;
			for(i=0;i<list.size();i++){
				//System.out.println("list["+i+"]:"+list.get(i)+"looking for"+address);
				if(list.get(i).longValue()==address){
					return i;
				}
			}
			return -1;
		}
		
	@SuppressWarnings("unchecked") 
	public static int[] LRU(int pagesize,int numFrames,int proc0Frames,int proc1Frames,int offset, String tracefile)throws Exception{
			LinkedList p0= new LinkedList<Long>();
			LinkedList p0dirty=new LinkedList<Boolean>();
			LinkedList p1= new LinkedList<Long>();
			LinkedList p1dirty=new LinkedList<Boolean>();
			int pagefaults=0;
			int memWrite=0;
			int memAccesses=0;
			int pnum;
			String op;
			boolean dirty=false;
			String line="";
			BufferedReader infile = new BufferedReader (new FileReader( tracefile ));
			
			Long address;
				while(infile.ready()){
					line= infile.readLine();
					String[] fields= line.split(" ");
					op=fields[0];
					if(op.equals("s")){ dirty=true;}
					else{dirty=false;}
					address=(Long.decode(fields[1]))>>offset;
					pnum=Integer.parseInt(fields[2]);
					//shift to get page num for adresses
					if(pnum==0){ //proc 0
						int ind=indexOf(p0,address);
						if(ind==-1){
							//page fault
							pagefaults++;
							if(p0.size()==proc0Frames){
								p0.remove();
								@SuppressWarnings("unchecked")
								Boolean dRemoved=(Boolean)p0dirty.remove();
								if(dRemoved.booleanValue()==true){
									//disk write
									memWrite++;
								}
							
						}
						p0.addLast(address);
						p0dirty.addLast(dirty);
					}
					else{ //address found in p0 list
						//need to remove the index, and re add to beginning of list
						@SuppressWarnings("unchecked")
						Long removed=(Long)p0.remove(ind);
						@SuppressWarnings("unchecked")
						Boolean dremoved=(Boolean)p0dirty.remove(ind);
						if(dremoved.booleanValue()==true|| dirty==true){
							dirty=true;
						}
						p0.addLast(removed);
						p0dirty.addLast(new Boolean(dirty));
					}
				}
				if(pnum==1){ //proc 0
						int ind=indexOf(p1,address);
						if(ind==-1){
							//page fault
							pagefaults++;
							if(p1.size()==proc1Frames){
								p1.remove();
								@SuppressWarnings("unchecked") 
								Boolean dRemoved=(Boolean)p1dirty.remove();
								if(dRemoved.booleanValue()==true){
									//disk write
									memWrite++;
								}
						}
						p1.addLast(address);
						p1dirty.addLast(dirty);
					}
					else{ //address found in p0 list
						//need to remove the index, and re add to beginning of list
						@SuppressWarnings("unchecked")
						Long removed=(Long)p1.remove(ind);
						@SuppressWarnings("unchecked")
						Boolean dremoved=(Boolean)p1dirty.remove(ind);
						if(dremoved.booleanValue()==true|| dirty==true){
							dirty=true;
						}
						p1.addLast(removed);
						p1dirty.addLast(new Boolean(dirty));
					}
				}
							
				memAccesses++;
			}
			infile.close();
			int[] data=new int[3];
			data[0]=memAccesses;
			data[1]=pagefaults;
			data[2]=memWrite;
			
			return data;
		}
		@SuppressWarnings("unchecked") 
		public static int[]opt(int pagesize,int numFrames,int proc0Frames,int proc1Frames,int offset, String tracefile)throws Exception{
			//hash table with Long, Linked List 
			//another with the dirty bits for the tables?
			Hashtable<Long,LinkedList<Integer>> p0table= new Hashtable<Long,LinkedList<Integer>>();
			Hashtable<Long,LinkedList<Integer>> p1table= new Hashtable<Long,LinkedList<Integer>>();
			LinkedList p0= new LinkedList<Long>();
			LinkedList p0dirty=new LinkedList<Boolean>();
			LinkedList p1= new LinkedList<Long>();
			LinkedList p1dirty=new LinkedList<Boolean>();
			int pagefaults=0;
			int memWrite=0;
			int memAccesses=0;
			int pnum;
			String op;
			boolean dirty=false;
			String line="";
			BufferedReader infile;
			infile=new BufferedReader(new FileReader(tracefile ));
			Long address;
			int i=0;
				while(infile.ready()){
					line= infile.readLine();
					String[] fields= line.split(" ");
					address=(Long.decode(fields[1]))>>offset;
					pnum=Integer.parseInt(fields[2]);
					Integer num=new Integer(i);
					if(pnum==0){//add next line to the hashtables, 
						if(p0table.get(address)==null){ //avoid null pointer exception 
							LinkedList<Integer> addList=new LinkedList<Integer>();
							addList.add(num);
							p0table.put(address,addList);
						}
						else{//pull keys linked list, add next index, replace it
						LinkedList<Integer> addList=p0table.get(address);
						addList.add(num);
						p0table.put(address,addList);
						}
						
					}
					if(pnum==1){
						if(p1table.get(address)==null){
							LinkedList<Integer> addList=new LinkedList<Integer>();
							addList.add(num);
							p1table.replace(address,addList);
						}
						else{
						LinkedList<Integer> addList=p1table.get(address);
						addList.add(num);
						p1table.replace(address,addList);
						}
					}
				 i++;
				}
				infile.close();	
				infile=new BufferedReader (new FileReader(tracefile ));
				//now re trace the file to find pf and all that.
				while(infile.ready()){
					int num=0;
					line= infile.readLine();
					if(line==null){System.out.println("line is null");}
					String[] fields= line.split(" ");
					op=fields[0];
					address=(Long.decode(fields[1]))>>offset;
					pnum=Integer.parseInt(fields[2]);
					if(op.equals("s")){ dirty=true;}
					else{dirty=false;}
			
					
					if(pnum==0){
					
						//opt is useful for selecting eviction. funct for non evictions should not change
						int ind= indexOf(p0,address);
						if(ind==-1){ 
							//page fault
							pagefaults++;
							if(p0.size()==proc0Frames){// eviction time
								//look through the page table, for each entry calculate the numaccesses till they are accessed again
								//index with longest dist, removed.
								int key=optremoval(p0,p0table,num);
								@SuppressWarnings("unchecked")
								Long removed=(Long) p0.remove(key);
								@SuppressWarnings("unchecked")
								Boolean drem=(Boolean) p0dirty.remove(key);
								if(drem.booleanValue()==true){ 
									memWrite++;
								}
							}
							p0.add(address);
							p0dirty.add(dirty);
							LinkedList<Integer> newlist= p0table.get(address);
							if(newlist!=null){
							newlist.remove();
							p0table.replace(address,newlist);
							}
							
						}
						else{//page hit
							@SuppressWarnings("unchecked")
							Long removed=(Long)p0.remove(ind);
							@SuppressWarnings("unchecked")
							Boolean dremoved=(Boolean)p0dirty.remove(ind);
							if(dremoved.booleanValue()==true|| dirty==true){
								dirty=true;
							}
							p0.addLast(removed);
							p0dirty.addLast(new Boolean(dirty));
							LinkedList<Integer> newlist= p0table.get(address);
							if(newlist!=null){
							newlist.remove();
							p0table.replace(address,newlist);
							}
						}
						
					}
				if(pnum==1){
						
						//opt is useful for selecting eviction. funct for non evictions should not change
					int ind= indexOf(p1,address);
					if(ind==-1){ 
						//page fault
						pagefaults++;
						if(p1.size()==proc1Frames){// eviction time
							//look through the page table, for each entry calculate the numaccesses till they are accessed again
							//index with longest dist, removed.
							int key=optremoval(p1,p1table,num);
							@SuppressWarnings("unchecked")
							Long removed=(Long) p1.remove(key);
							@SuppressWarnings("unchecked") 
							Boolean drem=(Boolean) p1dirty.remove(key);
							if(drem.booleanValue()==true){ 
								memWrite++;
							}
						}
						p1.add(address);
						p1dirty.add(dirty);
						LinkedList<Integer> newlist= p1table.get(address);
						if(newlist!=null){ 
						newlist.remove();
						p1table.replace(address,newlist);
						}
					}
					else{//page hit
						@SuppressWarnings("unchecked") 
						Long removed=(Long)p1.remove(ind);
						@SuppressWarnings("unchecked") 
						Boolean dremoved=(Boolean)p1dirty.remove(ind);
						if(dremoved.booleanValue()==true|| dirty==true){
							dirty=true;
						}
						p1.addLast(removed);
						p1dirty.addLast(new Boolean(dirty));
						LinkedList<Integer> newlist= p1table.get(address);
						if(newlist!=null){
						newlist.remove();
						p1table.replace(address,newlist);
						}
					}
						
				}
				
				memAccesses++;
			}
		int[] data=new int[3];
		data[0]=memAccesses;
		data[1]=pagefaults;
		data[2]=memWrite;
			
		return data;
			
		}
		
		
@SuppressWarnings("unchecked") 	
public static int optremoval(LinkedList<Long> list, Hashtable<Long, LinkedList<Integer>> table, int num){
	int[] dists=new int[list.size()];
	for(int i=0;i<list.size();i++){
		
		LinkedList temp=table.get(list.get(i));
		if(temp!=null){
			if(temp.size()>0){
			Integer next=(Integer)temp.getFirst();
			dists[i]=next.intValue() -num;
			}
			else{ dists[i]=Integer.MAX_VALUE;}
		}
		else{
			dists[i]=Integer.MAX_VALUE;
		}
	}
		int max=dists[0];
		int maxind=0;
		for(int j=1;j<dists.length;j++){
			if(max<dists[j]){
				max=dists[j];
				maxind=j;
				}
				}	
	return maxind;
			
			
		}
		
		
}
