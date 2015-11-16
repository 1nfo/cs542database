package test;

import project.DBManager;

/**
 * Created by vincent on 11/9/15.
 */
public class TestMultab {
    public static void main(String[] args){
        DBManager dbmanager = DBManager.getInstance();

        String schema="Code char 5,Name char 45,Continent char 20,Region char 30," +
                "SurfaceArea int,IndepYear int,Population int,LifeExpectancy float," +
                "GNP int,GNPOID int,LocalName char 50,GovernmentForm char 50,HeadOfState char 50,Capital int,Code2 char 5";
        String schema2 = "ID int,Name char 40,CountryCode char 5,District char 40,Population int";
        //TestReadCSV.readcsv("movies",dbmanager,"@",
        //		"Title char 80,year int,format char 30,genre char 30,director char 60,writer char 60,country char 30,Studio char 80,Price char 10,Catalogno char 10",
        //		"data/movies.txt");
        //TestReadCSV.readcsv("movies1",dbmanager,"@",
        //		"Title char 80,year int,format char 30,genre char 30,director char 60,writer char 60,country char 30,Studio char 80,Price char 10,Catalogno char 10",
        //		"data/movies.txt");
        TestReadCSV.readcsv("Country", dbmanager, ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", schema, "data/country.csv");
        TestReadCSV.readcsv("City", dbmanager, ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", schema2, "data/city.csv");
    }
}
