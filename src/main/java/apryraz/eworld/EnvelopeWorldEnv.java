package apryraz.eworld;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;


public class EnvelopeWorldEnv {
    /**
     *  world dimension
     **/
    int WorldDim, EnvelopeX, EnvelopeY;
    ArrayList<String> envelopeLoc = new ArrayList<>();


    /**
     * Class constructor
     *
     * @param dim dimension of the world
     * @param FileEnvelopes File with list of envelopes locations<
     **/
    public EnvelopeWorldEnv(int dim, String FileEnvelopes) {
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
                String[] envelopes = data.split(" ");
                Collections.addAll(envelopeLoc,envelopes);
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error opening Envelopes file");
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
                ans = new AMessage("movedto",msg.getComp(1),msg.getComp(2), (new Integer(envelope)).toString());
            } else
                ans = new AMessage("notmovedto",msg.getComp(1),msg.getComp(2), "" );

        } else {
            if(msg.getComp(0).equals("detectsat")){
                int nx = Integer.parseInt(msg.getComp(1));
                int ny = Integer.parseInt(msg.getComp(2));
                String detectorRange = metalSensorReading(nx,ny);
                ans = new AMessage(detectorRange, msg.getComp(1), msg.getComp(2),"");
            }else if(msg.getComp(0).equals("treasureup")){
                    int ny = Integer.parseInt(msg.getComp(2));
                    String isup = IsTreasureUp(ny);
                    ans = new AMessage(isup, msg.getComp(1), msg.getComp(2),"");
            }else if(msg.getComp(0).equals("treasuredown")){
                int ny = Integer.parseInt(msg.getComp(2));
                String isdown = IsTreasureDown(ny);
                ans = new AMessage(isdown, msg.getComp(1), msg.getComp(2),"");
            }else if(msg.getComp(0).equals("treasureleft")){
                int nx = Integer.parseInt(msg.getComp(1));
                String isleft = IsTreasureLeft(nx);
                ans = new AMessage(isleft, msg.getComp(1), msg.getComp(2),"");
            }else if(msg.getComp(0).equals("treasureright")){
                int nx = Integer.parseInt(msg.getComp(1));
                String isright = IsTreasureRight(nx);
                ans = new AMessage(isright, msg.getComp(1), msg.getComp(2),"");
            }
        }
        return ans;

    }

    private String IsTreasureUp(int y){
        if(EnvelopeY > y){
            return "yes";
        }
        return  "no";
    }

    private String IsTreasureDown(int y){
        if(EnvelopeY < y){
            return "yes";
        }
        return  "no";
    }

    private String IsTreasureLeft(int x){
        if(EnvelopeX < x){
            return "yes";
        }
        return  "no";
    }

    private String IsTreasureRight(int x){
        if(EnvelopeX > x){
            return "yes";
        }
        return  "no";
    }

    /**
     * Check if there is an envelope in position (x,y)
     *
     * @param x x coordinate of agent position
     * @param y y coordinate of agent position
     * @return 1  if (x,y) contains an envelope, 0 otherwise
     **/
    public int isEnvelopeInMyCell(int x, int y) {
        String coord = x + "," + y;
        for (String p: envelopeLoc) {
            if(p.equals(coord)){
                return 1;
            }
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
        if(x == EnvelopeX && y == EnvelopeY){
            return "5";
        }else if(EnvelopeX-x == -1){
            return "3";
        }else if(EnvelopeX-x == 1){
            return "1";
        }else if(EnvelopeY-y == -1){
            return "4";
        }else if(EnvelopeY-y == 1){
            return "2";
        }else{
            return "0";
        }
    }
}
