package com.company;
import java.io.*;
import java.util.*;
import java.io.File;
import java.util.Scanner;

public class Main {

    private static int maxBeams = 32;  // max amount of beams per sat
    private static double maxDistanceSat = 45.0; // the max angle a starlink sat can connect
    private static double maxIntAngle = 20.0; // the max interference angle
    private static double maxUsersAngle = 10.0; // amount angle users need to be seperated


    public static class ECEF { // ecef class holding x y z coordinates
        double x;
        double y;
        double z;
        ECEF() {
            x = 0;
            y=0;
            z = 0;
        }

        ECEF(double a, double b, double c) { // constructor
            x = a;
            y=b;
            z = c;
        }

        public void print(){ // print function for debugging
            System.out.println(x);
            System.out.println(y);
            System.out.println(z);
        }

    }

    public static class user{ // user function which holds user info
        Integer name;
        char color;
        ECEF pos;

        public user(){
            name = -1;
            color = 'z';
            pos = null;
        }
    }


    public static void main(String args[]) throws FileNotFoundException {

        TreeMap<Integer, ECEF> satellite = new TreeMap<>(); // treemap of satellites
        TreeMap<Integer, ECEF> users = new TreeMap<>(); // treemap of users
        TreeMap<Integer, ECEF> interference = new TreeMap<>(); // treemap of interferences

        String input = args[0]; // get filename from args
        //System.out.println(input);

        readFile(satellite, users, interference, input); // call read



        //System.out.println(users.size());
        //System.out.println(satellite.size());
        //System.out.println(interference.size());

        TreeMap<Integer, ArrayList<user>> finalSet = new TreeMap<>();


        for (Map.Entry<Integer, ECEF> entry: satellite.entrySet()) { // loop through all sats

            int counter = 0; // beam counter
            Integer name = entry.getKey(); // sat name
            ECEF loc = entry.getValue(); // sat location
            List<Integer> usersRemaining = new ArrayList<Integer>(users.keySet()); // list of users not yet served


            finalSet.put(name, new ArrayList<user>());

            for (int usrs : usersRemaining){ // go through users

                if (finalSet.get(name).size() == maxBeams){ // if we are at max beams - break
                    break;
                }

                if (findAngle(loc, users.get(usrs), new ECEF(0,0,0)) < maxDistanceSat) // check to see if rachable from current sat
                {


                    Integer inter = interfered(users.get(usrs), loc, interference); // check to see if we are interfered


                    if (inter!= -1){ // if we are print out
                        counter++;
                        System.out.println("sat " + name + " beam " + counter + " user " + usrs + " Interfered " + inter);
                        users.remove(usrs);
                    }
                    else{

                        user conn = new user(); // make new user object
                        conn.name = usrs;
                        conn.pos = users.get(usrs);

                        if (add(finalSet, conn, loc, name, counter) == true) { // if we can add it to the sat we will print info
                            counter++;

                            System.out.println("sat " + name + " beam " + counter + " user " + conn.name + " color " + conn.color );
                            users.remove(usrs);
                        }
                    }

                }







            }
        }

       //System.out.println("USER Size: " + users.size());

       // write(finalSet);









    }

    public static void readFile(TreeMap<Integer, ECEF> satellite, TreeMap<Integer, ECEF> users, TreeMap<Integer, ECEF> interference, String fileNames) throws FileNotFoundException
    {
        File filename = new File(fileNames);
        Scanner scan = new Scanner(filename);

        while (scan.hasNextLine())
        {
            String x = scan.nextLine();

            if (x.length() > 0){
                if (x.charAt(0) != '#')
                {

                    String[] split = x.split("\\s+");

                    if (split[0].equals("user")){

                        ECEF input = new ECEF();
                        input.x = Double.parseDouble(split[2]);
                        input.y = Double.parseDouble(split[3]);
                        input.z = Double.parseDouble(split[4]);

                        users.put(Integer.parseInt(split[1]), input);

                    }

                    if (split[0].equals("sat")){

                        ECEF input = new ECEF();
                        input.x = Double.parseDouble(split[2]);
                        input.y = Double.parseDouble(split[3]);
                        input.z = Double.parseDouble(split[4]);

                        satellite.put(Integer.parseInt(split[1]), input);

                    }

                    if (split[0].equals("interferer")){

                        ECEF input = new ECEF();
                        input.x = Double.parseDouble(split[2]);
                        input.y = Double.parseDouble(split[3]);
                        input.z = Double.parseDouble(split[4]);

                        interference.put(Integer.parseInt(split[1]), input);

                    }





                }


            }

        }

    }

    public static double findAngle(ECEF point1, ECEF point2, ECEF point3) {

        //point three is where the angle is made

        ECEF new1 = new ECEF(); // create two new ECEF objects
        ECEF new2 = new ECEF();

        new1.x = point1.x - point3.x; // normalize the points by subracting the middle / starting point
        new1.y = point1.y - point3.y;
        new1.z = point1.z - point3.z;

        new2.x = point2.x - point3.x;
        new2.y = point2.y - point3.y;
        new2.z = point2.z - point3.z;

        double numerator = (new1.x * new2.x) + (new1.y * new2.y) + (new1.z * new2.z); // get the numerator by doing dot product

        double denominator = Math.sqrt(Math.pow(new1.x, 2) + Math.pow(new1.y, 2) + Math.pow(new1.z, 2))
                *  Math.sqrt(Math.pow(new2.x, 2) + Math.pow(new2.y, 2) + Math.pow(new2.z, 2)); // magnitude of the two points

        double div = numerator/denominator; // get fraction
        double ans  = Math.acos(div); // get radians by using inverse cosine

        double degrees = ans * (180 / Math.PI); // convert to degrees

        return degrees;

    }

    public static int interfered(ECEF user, ECEF starlink, TreeMap<Integer, ECEF> interferences){

        for (Map.Entry<Integer, ECEF> entry: interferences.entrySet()) { // loop though all interfere sats
            ECEF locationIn = entry.getValue();

            if (findAngle(locationIn, starlink, user) < maxIntAngle){ // if there is one then we will interfere

                return entry.getKey();
            }
        }

        return -1;


    }

    public static boolean add(TreeMap<Integer, ArrayList<user>> finalSet, user userAdd, ECEF satLoc, Integer satName, int count){

        ArrayList<user> listOfBeams = finalSet.get(satName); // get current list of beams on this sat


        if (listOfBeams.size() == 0){ // if the size is 0 just add it
            userAdd.color = 'A';
          //  System.out.println("sat " + satName + " beam " + count + " user " + userAdd.name + " color " + userAdd.color );
            listOfBeams.add(userAdd);
        }
        else{
            int counter = 0; // counter to see how many users are less than 10 degrees away
            for (user beam: listOfBeams){
                if (findAngle(userAdd.pos, beam.pos, satLoc) < maxUsersAngle){
                    counter++;
                    if (counter == 4){ // if more that 4 then we cannot add this user to this sat
                        return false;
                    }
                }
            }
            //System.out.println(counter);
            if (counter == 0) { // add color based on how many users are near it
                userAdd.color = 'A';
            }else if (counter == 1){
                userAdd.color = 'B';
            }else if (counter == 2){
                userAdd.color = 'C';
            }else if (counter == 3){
                userAdd.color = 'D';
            }


            //System.out.println("sat " + satName + " beam " + count + " user " + userAdd.name + " color " + userAdd.color );
            listOfBeams.add(userAdd); // add user to list
        }

        return true;

    }
// debugging stuff
    /*public static void write(TreeMap<String, ArrayList<user>> finalSet)
    {

        for (Map.Entry<String, ArrayList<user>> entry : finalSet.entrySet()){

            String satName = entry.getKey();
            ArrayList<user> beams = entry.getValue();
            int counter = 1;

            for (user beam : beams){
                System.out.println("sat " + satName + " beam " + counter + " user " + beam.name + " color " + beam.color );
                counter++;
            }

        }


    }*/


}
