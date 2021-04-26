package apryraz.eworld;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import static java.lang.System.exit;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


/**
 * This agent performs a sequence of movements, and after each
 * movement it "senses" from the evironment the resulting position
 * and then the outcome from the smell sensor, to try to locate
 * the position of Treasure
 **/
public class EnvelopeFinder {


    /**
     * The list of steps to perform
     **/
    ArrayList<Position> listOfSteps;
    /**
     * index to the next movement to perform, and total number of movements
     **/
    int idNextStep, numMovements;
    /**
     * Array of clauses that represent conclusiones obtained in the last
     * call to the inference function, but rewritten using the "past" variables
     **/
    ArrayList<VecInt> futureToPast = new ArrayList<>();
    /**
     * the current state of knowledge of the agent (what he knows about
     * every position of the world)
     **/
    EFState efstate;
    /**
     * The object that represents the interface to the Treasure World
     **/
    EnvelopeWorldEnv EnvAgent;
    /**
     * SAT solver object that stores the logical boolean formula with the rules
     * and current knowledge about not possible locations for Treasure
     **/
    ISolver solver;
    /**
     * Agent position in the world and variable to record if there is a agent
     * at that current position
     **/
    int agentX, agentY, envelopeFound;
    /**
     * Dimension of the world and total size of the world (Dim^2)
     **/
    int WorldDim, WorldLinealDim;

    /**
     * This set of variables CAN be use to mark the beginning of different sets
     * of variables in your propositional formula (but you may have more sets of
     * variables in your solution).
     **/
    int EnvelopePastOffset;
    int EnvelopeFutureOffset;
    int Detector1Offset = 0;
    int Detector2Offset = 0;
    int Detector3Offset = 0;
    int Detector4Offset = 0;
    int Detector5Offset = 0;
    int actualLiteral;

    /**
     * The class constructor must create the initial Boolean formula with the
     * rules of the Envelope World, initialize the variables for indicating
     * that we do not have yet any movements to perform, make the initial state.
     *
     * @param WDim the dimension of the Envelope World
     **/
    public EnvelopeFinder(int WDim) {

        WorldDim = WDim;
        WorldLinealDim = WorldDim * WorldDim;

        try {
            solver = buildGamma();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ContradictionException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        numMovements = 0;
        idNextStep = 0;
        System.out.println("STARTING ENVELOPE FINDER AGENT...");


        efstate = new EFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        efstate.printState();

    }


    /**
     * Store a reference to the Environment Object that will be used by the
     * agent to interact with the Envelope World, by sending messages and getting
     * answers to them. This function must be called before trying to perform any
     * steps with the agent.
     *
     * @param environment the Environment object
     **/
    public void setEnvironment(EnvelopeWorldEnv environment) {
        EnvAgent = environment;
    }


    /**
     * Load a sequence of steps to be performed by the agent. This sequence will
     * be stored in the listOfSteps ArrayList of the agent.  Steps are represented
     * as objects of the class Position.
     *
     * @param numSteps  number of steps to read from the file
     * @param stepsFile the name of the text file with the line that contains
     *                  the sequence of steps: x1,y1 x2,y2 ...  xn,yn
     **/
    public void loadListOfSteps(int numSteps, String stepsFile) {
        String[] stepsList;
        String steps = ""; // Prepare a list of movements to try with the FINDER Agent
        try {
            BufferedReader br = new BufferedReader(new FileReader(stepsFile));
            System.out.println("STEPS FILE OPENED ...");
            steps = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(numSteps);
        for (int i = 0; i < numSteps; i++) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     * Returns the current state of the agent.
     *
     * @return the current state of the agent, as an object of class EFState
     **/
    public EFState getState() {
        return efstate;
    }

    /**
     * Execute the next step in the sequence of steps of the agent, and then
     * use the agent sensor to get information from the environment. In the
     * original Envelope World, this would be to use the Smelll Sensor to get
     * a binary answer, and then to update the current state according to the
     * result of the logical inferences performed by the agent with its formula.
     *
     * @throws IOException            when opening states or steps file.
     * @throws ContradictionException if inserting contradictory information to solver.
     * @throws TimeoutException       if solver's isSatisfiable operation spends more
     * 	                                 time computing than a certain timeout.
     **/
    public void runNextStep() throws
            IOException, ContradictionException, TimeoutException {
        // Add the conclusions obtained in the previous step
        // but as clauses that use the "past" variables
        addLastFutureClausesToPastClauses();

        // Ask to move, and check whether it was successful
        // Also, record if a agent was found at that position
        processMoveAnswer(moveToNext());

        // Next, use Detector sensor to discover new information
        processDetectorSensorAnswer(DetectsAt());
        // If was found at new agent position, ask question and process Answer to discover new information

        // Perform logical consequence questions for all the positions
        // of the Envelope World
        performInferenceQuestions();
        efstate.printState();      // Print the resulting knowledge matrix
    }


    /**
     * Ask the agent to move to the next position, by sending an appropriate
     * message to the environment object. The answer returned by the environment
     * will be returned to the caller of the function.
     *
     * @return the answer message from the environment, that will tell whether the
     * movement was successful or not.
     **/
    public AMessage moveToNext() {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return (new AMessage("NOMESSAGE", "", "",""));
        }
    }

    /**
     * Use agent "actuators" to move to (x,y)
     * We simulate this why telling to the World Agent (environment)
     * that we want to move, but we need the answer from it
     * to be sure that the movement was made with success
     *
     * @param x horizontal coordinate of the movement to perform
     * @param y vertical coordinate of the movement to perform
     * @return returns the answer obtained from the environment object to the
     * moveto message sent
     **/
    public AMessage moveTo(int x, int y) {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", (Integer.valueOf(x).toString()), (Integer.valueOf(y).toString()), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

    /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
     **/
    public void processMoveAnswer(AMessage moveans) {
        if (moveans.getComp(0).equals("movedto")) {
            agentX = Integer.parseInt(moveans.getComp(1));
            agentY = Integer.parseInt(moveans.getComp(2));
            System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")");
        }
    }

    /**
     * Send to the environment object the question:
     * "Does the detector sense something around(agentX,agentY) ?"
     *
     * @return return the answer given by the environment
     **/
    public AMessage DetectsAt() {
        AMessage msg, ans;

        msg = new AMessage("detectsat", (Integer.valueOf(agentX).toString()),
                (Integer.valueOf(agentY).toString()), "");        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ")");
        return ans;
    }


    /**
     * Process the answer obtained for the query "Detects at (x,y)?"
     * by adding the appropriate evidence clause to the formula
     *
     * @param ans message obtained to the query "Detects at (x,y)?".
     *            It will a message with three fields: [0,1,2,3] x y
     **/
    public void processDetectorSensorAnswer(AMessage ans) throws
            ContradictionException,TimeoutException, IOException {

        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String[] detector = ans.getComp(0).split(",");
        Boolean[] sensorValues = {false,false,false,false,false};
        for(int i = 0; i < detector.length; i++) {
            if (detector[i].equals("1")) {
                sensorValues[0] = true;
            } else if (detector.equals("2")) {
                sensorValues[1] = true;
            } else if (detector.equals("3")) {
                sensorValues[2] = true;
            } else if (detector.equals("4")) {
                sensorValues[3] = true;
            } else if (detector.equals("5")) {
                sensorValues[4] = true;
            }
        }
        for(int i = 0; i < sensorValues.length; i++) {
            VecInt clause = new VecInt();
            if(sensorValues[i] == true){
                clause.push(coordToLineal(x,y, Detector1Offset) + (i));
            }else{
                clause.push(-1*(coordToLineal(x,y, Detector1Offset) + (i)));
            }
            solver.addClause(clause);
        }
    }


    /**
     * This function should add all the clauses stored in the list
     * futureToPast to the formula stored in solver.
     * Use the function addClause( VecInt ) to add each clause to the solver
     *
     * @throws IOException            when opening states or steps file.
     * @throws ContradictionException if inserting contradictory information to solver.
     * @throws TimeoutException       if solver's isSatisfiable operation spends more
     * 	                                 time computing than a certain timeout.
     **/
    public void addLastFutureClausesToPastClauses() throws ContradictionException, IOException, TimeoutException {
        for(int i = 0; !futureToPast.isEmpty() && i < futureToPast.size(); i++){
            if(!futureToPast.get(i).isEmpty()){
                solver.addClause(futureToPast.get(i));
            }
        }
        futureToPast.clear();
    }

    /**
     * This function should check, using the future variables related
     * to possible positions of Treasure, whether it is a logical consequence
     * that Treasure is NOT at certain positions. This should be checked for all the
     * positions of the Envelope World.
     * The logical consequences obtained, should be then stored in the futureToPast list
     * but using the variables corresponding to the "past" variables of the same positions
     * <p>
     * An efficient version of this function should try to not add to the futureToPast
     * conclusions that were already added in previous steps, although this will not produce
     * any bad functioning in the reasoning process with the formula.
     *
     * @throws IOException            when opening states or steps file.
     * @throws ContradictionException if inserting contradictory information to solver.
     * @throws TimeoutException       if solver's isSatisfiable operation spends more
     * 	                                 time computing than a certain timeout.
     **/
    public void performInferenceQuestions() throws TimeoutException, IOException, ContradictionException {
        EnvelopePastOffset = WorldLinealDim * 5 + 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        VecInt future = new VecInt();
        VecInt past = new VecInt();
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = 1; j <= WorldDim; j++) {
                int linealIndexPast = coordToLineal(i, j,EnvelopePastOffset);
                int linealIndex = coordToLineal(i, j, EnvelopeFutureOffset);
                future.insertFirst(linealIndex);

                //It checks if Γ + positiveVar it is unsatisfiable
                //Then it adds the conclusion to the list but regarding to variables from the past
                if (!(solver.isSatisfiable(future))) {
                    past.insertFirst(-(linealIndexPast));
                    futureToPast.add(past);
                    past.clear();
                    efstate.set(i, j, "X");
                }
                future.clear();
            }
        }
    }

    /**
     * This function builds the initial logical formula of the agent and stores it
     * into the solver object.
     *
     * @return returns the solver object where the formula has been stored
     *
     * @throws IOException            when opening states or steps file.
     * @throws ContradictionException if inserting contradictory information to solver.
     * @throws UnsupportedOperationException  if solver's isSatisfiable operation spends more
     * 	                                 time computing than a certain timeout.
     * @throws FileNotFoundException if we don't found the file.
     **/
    public ISolver buildGamma() throws ContradictionException, UnsupportedOperationException, FileNotFoundException, IOException {

        int totalNumVariables;

        // You must set this variable to the total number of boolean variables
        // in your formula Gamma
        totalNumVariables = WorldLinealDim*7;
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // This variable is used to generate, in a particular sequential order,
        // the variable indentifiers of all the variables
        actualLiteral = 1;

        // call here functions to add the differen sets of clauses
        // of Gamma to the solver object

        createGoodClauses();
        createALOClauses();
        createSensor1();
        createSensor2();
        createSensor3();
        createSensor4();
        createSensor5();

        return solver;
    }

    /**
     * This function should add all the good clauses stored in the list
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/

    public void createGoodClauses() throws ContradictionException {
        EnvelopePastOffset = WorldLinealDim * 5 + 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        int linealIndexPast = 0;
        int linealIndex = 0;
        for (int i = 1; i <= this.WorldDim; i++) {
            for (int j = 1; j <= this.WorldDim; j++) {
                VecInt c = new VecInt();
                linealIndexPast = coordToLineal(i, j, EnvelopePastOffset);
                linealIndex = coordToLineal(i, j, EnvelopeFutureOffset);
                c.insertFirst(-(linealIndexPast));
                c.insertFirst(-(linealIndex));
                solver.addClause(c);
            }
        }
    }

    /**
     * This function should add all the ALO clauses in the solver
     * with their pertinent clause time.
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/

    public void createALOClauses() throws ContradictionException {
        VecInt past = new VecInt();
        VecInt future = new VecInt();
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        EnvelopePastOffset = WorldLinealDim * 5 + 1;
        int linealIndexPast = 0;
        int linealIndex = 0;
        for (int i = 1; i <= this.WorldDim; i++){
            for (int j = 1; j <= this.WorldDim; j++) {
                linealIndexPast = coordToLineal(i, j, EnvelopePastOffset);
                linealIndex = coordToLineal(i, j, EnvelopeFutureOffset);
                past.insertFirst(linealIndexPast);
                future.insertFirst(linealIndex);
            }
        }
        solver.addClause(past);
        solver.addClause(future);

    }


    /**
     * This function should manage the sensor reading 1 and their clauses.
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/
    public void createSensor1() throws ContradictionException{
        Detector1Offset = 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        int linealIndexSensor= 0;
        int linealIndex1 = 0;
        int linealIndex2 = 0;
        int linealIndex3 = 0;
        for(int i = 1; i <= this.WorldDim; i++) {
            for (int j = 1; j <= this.WorldDim; j++) {
                VecInt badClause = new VecInt();
                linealIndexSensor = coordToLineal(i, j, Detector1Offset);
                if(i + 1 <= WorldDim && j - 1 > 0){
                    linealIndex1 = coordToLineal(i + 1, j - 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex1));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim){
                    linealIndex2 = coordToLineal(i + 1, j, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex2));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim && j + 1 <= WorldDim){
                    linealIndex3 = coordToLineal(i + 1, j + 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex3));
                    solver.addClause(badClause);
                }
            }
        }

    }


    /**
     * This function should manage the sensor reading 2 and their clauses.
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/
    public void createSensor2() throws ContradictionException{
        Detector2Offset = 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        int linealIndexSensor= 0;
        int linealIndex1 = 0;
        int linealIndex2 = 0;
        int linealIndex3 = 0;
        for(int i = 1; i <= this.WorldDim; i++) {
            for (int j = 1; j <= this.WorldDim; j++) {
                VecInt badClause = new VecInt();
                linealIndexSensor = coordToLineal(i, j, Detector2Offset);
                if(i + 1 <= WorldDim && j - 1 > 0){
                    linealIndex1 = coordToLineal(i + 1, j + 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex1));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim){
                    linealIndex2 = coordToLineal(i , j + 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex2));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim && j + 1 <= WorldDim){
                    linealIndex3 = coordToLineal(i - 1, j + 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex3));
                    solver.addClause(badClause);
                }
            }
        }

    }

    /**
     * This function should manage the sensor reading 3 and their clauses.
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/
    public void createSensor3() throws ContradictionException{
        Detector3Offset = 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        int linealIndexSensor= 0;
        int linealIndex1 = 0;
        int linealIndex2 = 0;
        int linealIndex3 = 0;
        for(int i = 1; i <= this.WorldDim; i++) {
            for (int j = 1; j <= this.WorldDim; j++) {
                VecInt badClause = new VecInt();
                linealIndexSensor = coordToLineal(i, j, Detector3Offset);
                if(i + 1 <= WorldDim && j - 1 > 0){
                    linealIndex1 = coordToLineal(i - 1, j - 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex1));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim){
                    linealIndex2 = coordToLineal(i - 1, j, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex2));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim && j + 1 <= WorldDim){
                    linealIndex3 = coordToLineal(i - 1, j + 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex3));
                    solver.addClause(badClause);
                }
            }
        }

    }

    /**
     * This function should manage the sensor reading 4 and their clauses.
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/
    public void createSensor4() throws ContradictionException{
        Detector4Offset = 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        int linealIndexSensor= 0;
        int linealIndex1 = 0;
        int linealIndex2 = 0;
        int linealIndex3 = 0;
        for(int i = 1; i <= this.WorldDim; i++) {
            for (int j = 1; j <= this.WorldDim; j++) {
                VecInt badClause = new VecInt();
                linealIndexSensor = coordToLineal(i, j, Detector4Offset);
                if(i + 1 <= WorldDim && j - 1 > 0){
                    linealIndex1 = coordToLineal(i + 1, j - 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex1));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim){
                    linealIndex2 = coordToLineal(i, j - 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex2));
                    solver.addClause(badClause);
                }
                if(i + 1 <= WorldDim && j + 1 <= WorldDim){
                    linealIndex3 = coordToLineal(i - 1, j - 1, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex3));
                    solver.addClause(badClause);
                }
            }
        }

    }


    /**
     * This function should manage the sensor reading 5 and their clauses.
     *
     *
     * @throws ContradictionException if inserting contradictory information to solver.
     *
     **/
    public void createSensor5() throws ContradictionException{
        Detector5Offset = 1;
        EnvelopeFutureOffset = WorldLinealDim * 6 + 1;
        int linealIndexSensor= 0;
        int linealIndex1 = 0;
        for(int i = 1; i <= this.WorldDim; i++) {
            for (int j = 1; j <= this.WorldDim; j++) {
                VecInt badClause = new VecInt();
                linealIndexSensor = coordToLineal(i, j, Detector5Offset);
                if(i <= WorldDim && j > 0){
                    linealIndex1 = coordToLineal(i, j, EnvelopeFutureOffset);
                    badClause.insertFirst(-(linealIndexSensor));
                    badClause.insertFirst(-(linealIndex1));
                    solver.addClause(badClause);
                }
            }
        }

    }

    /**
     * Convert a coordinate pair (x,y) to the integer value  t_[x,y]
     * of variable that stores that information in the formula, using
     * offset as the initial index for that subset of position variables
     * (past and future position variables have different variables, so different
     * offset values)
     *
     * @param x      x coordinate of the position variable to encode
     * @param y      y coordinate of the position variable to encode
     * @param offset initial value for the subset of position variables
     *               (past or future subset)
     * @return the integer indentifer of the variable  b_[x,y] in the formula
     **/
    public int coordToLineal(int x, int y, int offset) {
        return ((x - 1) * WorldDim) + (y - 1) + offset;
    }

    /**
     * Perform the inverse computation to the previous function.
     * That is, from the identifier t_[x,y] to the coordinates  (x,y)
     * that it represents
     *
     * @param lineal identifier of the variable
     * @param offset offset associated with the subset of variables that
     *               lineal belongs to
     * @return array with x and y coordinates
     **/
    public int[] linealToCoord(int lineal, int offset) {
        lineal = lineal - offset + 1;
        int[] coords = new int[2];
        coords[1] = ((lineal - 1) % WorldDim) + 1;
        coords[0] = (lineal - 1) / WorldDim + 1;
        return coords;
    }
}