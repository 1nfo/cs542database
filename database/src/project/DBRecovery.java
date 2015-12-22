package project;

import java.io.*;
import java.util.*;

public class DBRecovery {
	private static String RCV_FLAG = "<RCV PNT>";
	private static String BRACKET = "<";
	private static String CHECKPOINT = "<CHK PNT>";
	private static String INTEGER = "Integer";
	private static String FLOAT = "Float";
	private static String STRING = "String";
	private static String REDO_FLAG = "REDO HERE";
	private String path;

	public DBRecovery(String path){
		this.path=path;
		if(!new File(path).isFile()) clearLog();
	}

	public void Recover(DBManager dbm,String Redo_Undo) throws Exception {
		List<LogObj<?>> list_logObj=logLoad();
		if(Redo_Undo.equals("redo")) Collections.reverse(list_logObj);
		if (list_logObj.size()==0) return;
		System.out.println("Database failed previously, \nNow recovering . . .");
		writeIntoLog("\n"+RCV_FLAG+"\n");
		for(LogObj alog:list_logObj){
			int rid=alog.getRowId();
			String attNames=alog.getVarName();
			Object val;
			switch (Redo_Undo.toLowerCase().trim()){
				case "redo":val=alog.getNewVlaue();break;
				case "undo":val=alog.getOldValue();break;
				default:throw new Exception("Unknown redo/undo type!");
			}
			dbm.setAttribute(rid,attNames,val);
		}
		writeCHK();
		System.out.println("Data Recovered.");
	}

	public void clearLog(){
		try {
			PrintWriter writer = new PrintWriter(path);
			writer.println();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void writeCHK(){writeIntoLog(CHECKPOINT);}

	public void logUpdate(int rid, String attrName, int type, Object oldVal,String newV) throws Exception {
		String row,oldV;
		switch (type){
			case 0:oldV=Integer.toString((Integer) oldVal);break;
			case 1:oldV= (String) oldVal;oldV="'"+oldV+"'";break;
			case 2:oldV= Float.toString((Float) oldVal);break;
			default:throw new Exception("Unknown type id");
		}
		row="<"+Integer.toString(rid)+", "+attrName+", "+oldV+", "+newV+">";
		writeIntoLog(row);
	}

	/**
	 * First reading the log file into memory  and push every line<TableID, ColumnID, OldValue, NewValue> into a stack. 
	 * Next in a loop popping the line in the stack and then checking whether the first appearance of line containing "<CHK PNT>" flag.
	 * If there has a  "<CHK PNT>" flag, then stop the loop. For every update or delete record, using parseLog function to parse the line 
	 * from String to LogObj format and add this log object into a list. During the parsing process, based on the value type, we used Java Generic to 
	 * save different variable types in the log line. For example, if the log line <1,1,0.3,0.5>, the parseLog function will automatically create 
	 * a log object including a double type of old value and new value. Finally, we transfer this log object list to the upper layer to do the recovery.
	 * @return the list of log objects
	 */
	private List<LogObj<?>> logLoad(){
		BufferedReader br = null;
		Stack<String> stack_line = new Stack<>();
		List<LogObj<?>> list_logObj = new ArrayList<>();
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				stack_line.push(line);
			}
			// split the line into parts
			while(!stack_line.isEmpty()){
				line = stack_line.pop();
				if(line.contains(DBRecovery.CHECKPOINT))
					return list_logObj;
				else if(line.contains(RCV_FLAG))
					list_logObj = new ArrayList<>();
				else if(line.contains(BRACKET))
					list_logObj.add(this.parseLog(line.substring(1, line.length()-1)));

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Log File '" + "'" + " didn't exist.");
		} catch(Exception e){ 
			e.printStackTrace();
		} finally{
			if(br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return list_logObj;
	}
	
	private LogObj<?> parseLog(String line){
		StringTokenizer st = new StringTokenizer(line, ",");
		List<String> lst = new ArrayList<>();
		String sign = DBRecovery.INTEGER;
		while(st.hasMoreElements()){
			String value = st.nextElement().toString().trim();
			if(value.contains(".")) sign = DBRecovery.FLOAT;
			else if(value.contains("'") || value.contains("\"")) sign = DBRecovery.STRING;
			lst.add(value);
		}
		
		if(sign.equals(DBRecovery.INTEGER)){
			LogObj<Integer> lo = new LogObj<>();
			lo.setRowId(Integer.parseInt(lst.get(0))); // TableID
			lo.setVarName(lst.get(1)); //Variable Name
			lo.setOldValue(Integer.parseInt(lst.get(2))); // Old Value
			lo.setNewVlaue(Integer.parseInt(lst.get(3))); // New Value
			
			return lo;
		}
		if(sign.equals(DBRecovery.FLOAT)){
			LogObj<Float> lo = new LogObj<>();
			lo.setRowId(Integer.parseInt(lst.get(0))); // TableID
			lo.setVarName(lst.get(1)); //Variable Name
			lo.setOldValue(Float.parseFloat(lst.get(2))); // Old Value
			lo.setNewVlaue(Float.parseFloat(lst.get(3))); // New Value
			
			return lo;
		}
		if(sign.equals(DBRecovery.STRING)){
			LogObj<String> lo = new LogObj<>();
			lo.setRowId(Integer.parseInt(lst.get(0))); // TableID
			lo.setVarName(lst.get(1)); //Variable Name
			lo.setOldValue(lst.get(2).substring(1, lst.get(2).length()-1)); // Old Value
			lo.setNewVlaue(lst.get(3).substring(1, lst.get(3).length()-1)); // New Value
			
			return lo;
		}
		return null;
	}

    private void writeIntoLog(String row){
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(path, true));
			output.write(row);
			output.newLine();
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	public void writeFailure() {writeIntoLog("Failed!");}

    public static void main(String[] args){
		String path = "logging2.txt";
		DBRecovery dbr = new DBRecovery(path);
		//dbr.clearLog();
		try {
			dbr.logUpdate(0,"title",1,"oo7","007");
		} catch (Exception e) {
			e.printStackTrace();
		}
		//dbr.writeCHK();
		List<LogObj<?>> st = dbr.logLoad();
		for(LogObj<?> a: st)
			System.out.println(a.getNewVlaue());
	}

	public void writeRedoPoint() {
		writeIntoLog(REDO_FLAG);
	}
}
