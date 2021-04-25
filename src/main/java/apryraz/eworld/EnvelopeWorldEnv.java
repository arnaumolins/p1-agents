

package apryraz.eworld;

import java.util.ArrayList;





public class EnvelopeWorldEnv {
/**
   world dimension

**/
  int  WorldDim;
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
*   Load the list of pirates locations
*  
*    @param: name of the file that should contain a
*            set of envelope locations in a single line.
**/
  public void loadEnvelopeLocations( String envelopeFile ) {
      try {
          File myObj = new File(piratesFile);
          Scanner myReader = new Scanner(myObj);
          while (myReader.hasNextLine()) {
              String data = myReader.nextLine();
              String[] pirates = data.split(" ");
              Collections.addAll(pirateLoc,pirates);
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
           
           if (withinLimits(nx,ny)) {
               int envelope = isEnvelopeInMyCell(nx, ny);
             
             ans = new AMessage("movedto",msg.getComp(1),msg.getComp(2)  );
           }
           else
             ans = new AMessage("notmovedto",msg.getComp(1),msg.getComp(2), "" );

       } else {
           if (msg.getComp(0).equals("detectsat")) {
               int nx = Integer.parseInt(msg.getComp(1));
               int ny = Integer.parseInt(msg.getComp(2));
               String detectorRange = metalSensorReading(nx, ny);
               ans = new AMessage(detectorRange, msg.getComp(1), msg.getComp(2), "");
           }
       }
       return ans;

   }

   * Check if there is a pirate in position (x,y)
   * @param x x coordinate of agent position
   * @param y y coordinate of agent position
   * @return 1  if (x,y) contains a pirate, 0 otherwise
   **/
    public int isEnvelopeInMyCell(int x, int y) {
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

  private String metalSensorReading(int x, int y){
       if(x == TreasureX && y == TreasureY){
           return "5";
           if(x == EnvelopeX && y == EnvelopeY){
               return "5";
           }else if((Math.abs(EnvelopeX-x)) == -1){
               return "3";
           }else if((Math.abs(EnvelopeX-x)) == 1){
               return "1";
           }else if((Math.abs(EnvelopeY-y)) == 1){
               return "2";
           }else if((Math.abs(EnvelopeY-y)) == -1){
               return "4";
           }
           return "0";
       }
  }

  }
}
