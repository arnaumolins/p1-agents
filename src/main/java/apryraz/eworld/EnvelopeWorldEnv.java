

package apryraz.eworld;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;


public class EnvelopeWorldEnv {
/**
   world dimension

**/
  int  WorldDim;

/**
 * X,Y position of Treasure and world dimension
 **/
  int EnvelopeX, EnvelopeY;
  ArrayList<String> envelopeLoc = new ArrayList<>();


/**
*  Class constructor
*
* @param dim dimension of the world
* @param envelopeFile File with list of envelopes locations
**/
  public EnvelopeWorldEnv( int dim, String envelopeFile ) {

    WorldDim = dim;
    loadEnvelopeLocations(envelopeFile);
  }

    /**
     * Class constructor
     *
     * @param dim         dimension of the world
     * @param tx          X position of Treasure
     * @param ty          Y position of Treasure
     * @param FileEnvelopes File with list of envelopes locations<
     **/
    public EnvelopeWorldEnv(int dim, int tx, int ty, String FileEnvelopes) {

        EnvelopeX = tx;
        EnvelopeY = ty;
        WorldDim = dim;
        loadEnvelopeLocations(FileEnvelopes);
    }

/**
*   Load the list of envelopes locations
*  
*    @param: name of the file that should contain a
*            set of envelope locations in a single line.
**/
  public void loadEnvelopeLocations( String FileEnvelopes) {
      try {
          File myObj = new File(FileEnvelopes);
          Scanner myReader = new Scanner(myObj);
          while (myReader.hasNextLine()) {
              String data = myReader.nextLine();
              String[] pirates = data.split(" ");
              Collections.addAll(envelopeLoc,pirates);
          }
          myReader.close();
      } catch (FileNotFoundException e) {
          System.out.println("Error opening Pirates file");
          e.printStackTrace();
      }
  }


/**
* Process a message received by the EFinder agent,
* by returning an appropriate answer
* It should answer to moveto and detectsat messages
*
* @param   msg message sent by the Agent
*
* @return  a msg with the answer to return to the agent
**/
   public AMessage acceptMessage( AMessage msg ) {
       AMessage ans = new AMessage("voidmsg", "", "", "" );

         msg.showMessage();
       if ( msg.getComp(0).equals("moveto") ) {
           int nx = Integer.parseInt( msg.getComp(1) );
           int ny = Integer.parseInt( msg.getComp(2) );
           
           if (withinLimits(nx,ny))
           {
             
             
             ans = new AMessage("movedto",msg.getComp(1),msg.getComp(2), msg.getComp(2));
           }
           else
             ans = new AMessage("notmovedto",msg.getComp(1),msg.getComp(2), "" );

       } else {
           if(msg.getComp(0).equals("detectsat")){
               int nx = Integer.parseInt(msg.getComp(1));
               int ny = Integer.parseInt(msg.getComp(2));
               String detectorRange = metalSensorReading(nx,ny);
               ans = new AMessage(detectorRange, msg.getComp(1), msg.getComp(2),"");
           }
           if(msg.getComp(0).equals("treasureup")){
               int ny = Integer.parseInt(msg.getComp(2));
               String isup = IsTreasureUp(ny);
               ans = new AMessage(isup, msg.getComp(1), msg.getComp(2),"");
           }
         }
       return ans;

   }

    /**
     * Check if there is a pirate in position (x,y)
     *
     * @param x x coordinate of agent position
     * @param y y coordinate of agent position
     * @return 1  if (x,y) contains a pirate, 0 otherwise
     **/
    public int isPirateInMyCell(int x, int y) {
        String coord = x + "," + y;

        for (String p: envelopeLoc) {
            if(p.equals(coord)){return 1;}
        }
        return 0;
    }


 /**
  * Check if position x,y is within the limits of the
  * WorldDim x WorldDim   world
  *
  * @param x  x coordinate of agent position
  * @param y  y coordinate of agent position
  *
  * @return true if (x,y) is within the limits of the world
  **/
   public boolean withinLimits( int x, int y ) {

    return ( x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
  }

  private String IsTreasureUp(int y){
       if(EnvelopeY > y){
            return "yes";
       }
       return  "no";
  }

  private String metalSensorReading(int x, int y){
       if(x == EnvelopeX && y == EnvelopeY){
           return "1";
       }else if(pitagor(Math.abs(EnvelopeX-x),Math.abs(EnvelopeY-y)) == 1){
           return "2";
       }else if(pitagor(Math.abs(EnvelopeX-x),Math.abs(EnvelopeY-y)) == 2){
           return "3";
       }else{
           return "0";
       }
  }
  public static double pitagor (int x , int y){
       double c = Math.sqrt((x*x)+(y*y));
       return Math.floor(c);
  }
}
