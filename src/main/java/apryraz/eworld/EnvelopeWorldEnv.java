package apryraz.eworld;

import apryraz.eworld.AMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class EnvelopeWorldEnv {
    /**
     * X,Y position of Envelope and world dimension
     **/
    int EnvelopeX, EnvelopeY, WorldDim;
    ArrayList<Position> envelopeLoc = new ArrayList<>();


    /**
     * Class constructor
     *
     * @param dim dimension of the world
     * @param envelopesFile File with list of pirates locations<
     **/
    public EnvelopeWorldEnv(int dim, String envelopesFile) {
        WorldDim = dim;
        loadEnvelopesLocations(envelopesFile);
    }

    /**
     * Load the list of envelopes locations
     *
     * @param envelopesFile name of the file that should contain a
     * set of envelopes locations in a single line.
     **/
    public void loadEnvelopesLocations(String envelopesFile) {
        try {
            File myObj = new File(envelopesFile);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] envelopes = data.split(" ");
                for(int i = 0; i< envelopes.length; i ++) {
                    String[] cord = envelopes[i].split(",");
                    Position envelopePosition = new Position(Integer.parseInt(cord[0]), Integer.parseInt(cord[1]));
                    envelopeLoc.add(envelopePosition);
                }
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
     * This version only process answers to moveto and detectsat messages
     *
     * @param msg message sent by the Agent
     * @return a msg with the answer to return to the agent
     **/
    public AMessage acceptMessage(AMessage msg) {
        AMessage ans = new AMessage("voidmsg", "", "", "");

        msg.showMessage();
        if (msg.getComp(0).equals("moveto")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));

            if (withinLimits(nx, ny)) {
                int envelopes = 0;

                ans = new AMessage("movedto", msg.getComp(1), msg.getComp(2),
                        (Integer.valueOf(envelopes).toString()));
            } else
                ans = new AMessage("notmovedto", msg.getComp(1), msg.getComp(2), "");

        } else {
            if(msg.getComp(0).equals("detectsat")){
                int nx = Integer.parseInt(msg.getComp(1));
                int ny = Integer.parseInt(msg.getComp(2));
                String detectorRange = metalSensorReading(nx,ny);
                ans = new AMessage(detectorRange, msg.getComp(1), msg.getComp(2),"");
            }
        }
        return ans;

    }

    /**
     * Check if position x,y is within the limits of the
     * WorldDim x WorldDim   world
     *
     * @param x x coordinate of agent position
     * @param y y coordinate of agent position
     * @return true if (x,y) is within the limits of the world
     **/
    public boolean withinLimits(int x, int y) {
        return (x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
    }


    private String metalSensorReading(int x, int y){
        String sensorsActive = "";
        for(int i = 1; i <= 5; i++) {
            for (Position position : envelopeLoc) {
                if((i == 1 && position.x - x == 1 && Math.abs(position.y - y) <= 1 || (x + 1 > WorldDim)&& i ==1)){
                    sensorsActive += "1,";
                }else if((i == 2 && Math.abs(position.x - x) <= 1 && position.y - y == 1 || (y + 1 > WorldDim) && i ==2)){
                    sensorsActive += "2,";
                }else if((i == 3 && position.x - x == -1 && Math.abs(position.y - y) <= 1 || (x - 1 <= 0) && i ==3)){
                    sensorsActive += "3,";
                }else if((i == 4 && Math.abs(position.x - x) <= 1 && position.y - y == -1 || (y - 1 <= 0) && i == 4)){
                    sensorsActive += "4,";
                }else if(i == 5 && position.x == x && position.y == y){
                    sensorsActive += "5,";
                }
            }
        }
        return sensorsActive;
    }


}