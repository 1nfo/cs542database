package test;

import project.DBManager;

public class DBManagerTest {
	public static void main(String[] args) {
		DBManager dbManager=DBManager.getInstance();
		try {
			dbManager.setAttribute(1,"year",2016);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(dbManager.getAttribute(0,dbManager.Get(0,1),"year"));
	}
}
