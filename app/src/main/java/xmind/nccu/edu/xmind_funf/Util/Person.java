package xmind.nccu.edu.xmind_funf.Util;

/**
 * Created by sid.ku on 8/6/15.
 */
public class Person {

    private String name;
    private String country;
    private String twitter;

    public Person(String name, String country, String twitter){
        this.name = name;
        this.country = country;
        this.twitter = twitter;
    }

    public String getName(){
        return name;
    }

    public String getCountry(){
        return country;
    }

    public String getTwitter(){
        return twitter;
    }
}


