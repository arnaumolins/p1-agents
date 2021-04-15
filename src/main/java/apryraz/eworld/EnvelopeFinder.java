

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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;




/**
*  This agent performs a sequence of movements, and after each
*  movement it "senses" from the evironment the resulting position
*  and then the outcome from the smell sensor, to try to locate
*  the position of Envelope
*
**/
public class EnvelopeFinder  {


/**
  * The list of steps to perform
**/
    ArrayList<Position> listOfSteps;
/**
* index to the next movement to perform, and total number of movements
**/
    int idNextStep, numMovements;
/**
*  Array of clauses that represent conclusiones obtained in the last
* call to the inference function, but rewritten using the "past" variables
**/
    ArrayList<VecInt> futureToPast = null;
/**
* the current state of knowledge of the agent (what he knows about
* every position of the world)
**/
    EFState efstate;
/**
*   The object that represents the interface to the Envelope World
**/
   EnvelopeWorldEnv EnvAgent;
/**
*   SAT solver object that stores the logical boolean formula with the rules
*   and current knowledge about not possible locations for Envelope
**/
    ISolver solver;
/**
*   Agent position in the world 
**/
    int agentX, agentY, envelopeFound;
/**
*  Dimension of the world and total size of the world (Dim^2)
**/
    int WorldDim, WorldLinealDim;

/**
*    This set of variables CAN be use to mark the beginning of different sets
*    of variables in your propositional formula (but you may have more sets of
*    variables in your solution).
**/
    int EnvelopePastOffset;
    int EnvelopeFutureOffset;
    int Detector1Offset=0;
    int Detector2Offset=0;
    int Detector3Offset=0;
    int Detector4Offset=0;
    int Detector5Offset=0;
    int envelopeAboveOffset=0;
    int envelopeBelowOffset=0;
    int actualLiteral;


   /**
     The class constructor must create the initial Boolean formula with the
     rules of the Envelope World, initialize the variables for indicating
     that we do not have yet any movements to perform, make the initial state.

     @param WDim the dimension of the Envelope World

   **/
    public   EnvelopeFinder(int WDim)
    {

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
        System.out.println("STARTING Envelope FINDER AGENT...");


        efstate = new EFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        efstate.printState();
    }

    /**
      Store a reference to the Environment Object that will be used by the
      agent to interact with the Envelope World, by sending messages and getting
      answers to them. This function must be called before trying to perform any
      steps with the agent.

      @param environment the Environment object

    **/
    public void setEnvironment( EnvelopeWorldEnv environment ) {

         EnvAgent =  environment;
    }


    /**
      Load a sequence of steps to be performed by the agent. This sequence will
      be stored in the listOfSteps ArrayList of the agent.  Steps are represented
      as objects of the class Position.

      @param numSteps number of steps to read from the file
      @param stepsFile the name of the text file with the line that contains
                       the sequence of steps: x1,y1 x2,y2 ...  xn,yn

    **/
    public void loadListOfSteps( int numSteps, String stepsFile )
    {
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
        for (int i = 0 ; i < numSteps ; i++ ) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     *    Returns the current state of the agent.
     *
     *    @return the current state of the agent, as an object of class EFState
    **/
    public EFState getState()
    {
        return efstate;
    }

    /**
    *    Execute the next step in the sequence of steps of the agent, and then
    *    use the agent sensor to get information from the environment. In the
    *    original Envelope World, this would be to use the Smelll Sensor to get
    *    a binary answer, and then to update the current state according to the
    *    result of the logical inferences performed by the agent with its formula.
    *
    **/
    public void runNextStep() throws
            IOException,  ContradictionException, TimeoutException
    {
          
          // Add the conclusions obtained in the previous step
          // but as clauses that use the "past" variables
          addLastFutureClausesToPastClauses();

          // Ask to move, and check whether it was successful
          // Also, record if a envelope was found at that position
          processMoveAnswer( moveToNext( ) );


          // Next, use Detector sensor to discover new information
          processDetectorSensorAnswer( DetectsAt() );
           

          // Perform logical consequence questions for all the positions
          // of the Envelope World
          performInferenceQuestions();
          efstate.printState();      // Print the resulting knowledge matrix
    }


    /**
    *   Ask the agent to move to the next position, by sending an appropriate
    *   message to the environment object. The answer returned by the environment
    *   will be returned to the caller of the function.
    *
    *   @return the answer message from the environment, that will tell whether the
    *           movement was successful or not.
    **/
    public AMessage moveToNext()
    {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return (new AMessage("NOMESSAGE","",""));
        }
    }

    /**
    * Use agent "actuators" to move to (x,y)
    * We simulate this by telling to the World Agent (environment)
    * that we want to move, but we need the answer from it
    * to be sure that the movement was made with success
    *
    *  @param x  horizontal coordinate (row) of the movement to perform
    *  @param y  vertical coordinate (column) of the movement to perform
    *
    *  @return returns the answer obtained from the environment object to the
    *           moveto message sent
    **/
    public AMessage moveTo( int x, int y )
    {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", (new Integer(x)).toString(), (new Integer(y)).toString(), "" );
        ans = EnvAgent.acceptMessage( msg );
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

   /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
   **/
    public void processMoveAnswer ( AMessage moveans )
    {
        if ( moveans.getComp(0).equals("movedto") ) {
          agentX = Integer.parseInt( moveans.getComp(1) );
          agentY = Integer.parseInt( moveans.getComp(2) );
          envelopeFound = Integer.parseInt(moveans.getComp(3));
          System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")" + " Envelope found : "+envelopeFound );
        }
    }

    /**
     *   Send to the environment object the question:
     *   "Does the detector sense something around(agentX,agentY) ?"
     *
     *   @return return the answer given by the environment
    **/
    public AMessage DetectsAt( )
    {
        AMessage msg, ans;

        msg = new AMessage( "detectsat", (new Integer(agentX)).toString(),
                                       (new Integer(agentY)).toString(), "" );
        ans = EnvAgent.acceptMessage( msg );
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ")");
        return ans;
    }


    /**
    *   Process the answer obtained for the query "Detects at (x,y)?"
    *   by adding the appropriate evidence clause to the formula
    *
    *   @param ans message obtained to the query "Detects at (x,y)?".
    *          It will a message with three fields: DetectorValue x y
    *
    *    DetectorValue must be a number that encodes all the valid readings 
    *    of the sensor given the envelopes in the 3x3 square around (x,y)
    **/
    public void processDetectorSensorAnswer( AMessage ans ) throws
            IOException, ContradictionException,  TimeoutException
    {

        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String detects = ans.getComp(0);

         // Call your function/functions to add the evidence clauses
         // to Gamma to then be able to infer new NOT possible positions
         addDetectorEvidenceClauses(x,y,detects);

    }

    /** We need to add the clauses reggarding to the evidences we get from the metal detector.
     *
     * @param x coordinate for the possition x
     * @param y coordinate for the possition y
     * @param detects it correspounds to the range detected by the agent, will be between
     *                the ranges: 0,1,2,3.
     */
    private void addDetectorEvidenceClauses(int x, int y, String detects) throws ContradictionException{
        System.out.println("Detector returned: " + detects);
        System.out.println("Inserting detector evidence");
        switch (detects){
            case "1":
                addClause(x,y,+1,Detector1Offset);
                for (int i = 1; i <= WorldDim; i++) {
                    for (int j = 1; j <= WorldDim; j++) {
                        if(!(x==i && y==j)){
                            addClause(i,j,-1,EnvelopeFutureOffset);
                        }
                    }
                }
                System.out.println("Treasure found!");
                break;
            case "2":
                addClause(x,y,+1,Detector2Offset);
                for (int i = 1; i <= WorldDim; i++) {
                    for (int j = 1; j <= WorldDim; j++) {
                        if(pitagor(Math.abs(i-x),Math.abs(j-y)) != 1){
                            addClause(i,j,-1,EnvelopeFutureOffset);
                        }
                    }
                }
                break;
            case "3":
                addClause(x,y,+1,Detector3Offset);
                for (int i = 1; i <= WorldDim; i++) {
                    for (int j = 1; j <= WorldDim; j++) {
                        if(pitagor(Math.abs(i-x),Math.abs(j-y)) != 2){
                            addClause(i,j,-1,EnvelopeFutureOffset);
                        }
                    }
                }
                break;
            case "4":
                addClause(x,y,+1,Detector4Offset);
                for (int i = 1; i <= WorldDim; i++) {
                    for (int j = 1; j <= WorldDim; j++) {
                        if(pitagor(Math.abs(i-x),Math.abs(j-y)) != 2){
                            addClause(i,j,-1,EnvelopeFutureOffset);
                        }
                    }
                }
                break;
            case "5":
                addClause(x,y,+1,Detector5Offset);
                for (int i = 1; i <= WorldDim; i++) {
                    for (int j = 1; j <= WorldDim; j++) {
                        if(pitagor(Math.abs(i-x),Math.abs(j-y)) != 2){
                            addClause(i,j,-1,EnvelopeFutureOffset);
                        }
                    }
                }
                break;
        }
    }

    public static double pitagor (int x , int y){
        double c = Math.sqrt((x*x)+(y*y));
        return Math.floor(c);
    }

    /**
     * It models the information using solver's vector and adds it to the solver.
     *
     * @param x coordinate of the possition x
     * @param y coordinate for the possition y
     * @param sign it indicates the sign of an specific literal, may be -1 or 1
     * @param offset it is the offset that corresponds to the subset of variables
     *               that contains that literal.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */
    private void addClause(int x, int y, int sign, int offset) throws ContradictionException {
        int lc;
        VecInt evidence = new VecInt();
        if(sign == -1){
            lc = -(coordToLineal(x,y,offset));
        }else{
            lc = coordToLineal(x,y,offset);
        }
        evidence.insertFirst(lc);
        solver.addClause(evidence);
    }




    /**
     * Send to the envelope (using the environment object) the question:
     * "Is the treasure up or down of (agentX,agentY)  ?"
     *
     * @return return the answer given by the envelope
     **/
    public AMessage IsTreasureUpOrDown() {
        AMessage msg, ans;

        msg = new AMessage("treasureup", (new Integer(agentX)).toString(),
                (new Integer(agentY)).toString(), "");
        ans = EnvAgent.acceptMessage(msg);
        System.out.println("FINDER => checking treasure up of : (" + agentX + "," + agentY + ")");
        return ans;
    }

    /**
     * We need to add the clauses reggarding to the evidences we get from the envelope.
     *
     * @param ans message obtained from the query to the envelope.
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */
    public void processEnvelopeAnswer(AMessage ans) throws ContradictionException{
        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String isup = ans.getComp(0);

        if(isup.equals("yes")){
            for (int i = 1; i <= WorldDim; i++) {
                for (int j = y; j > 0; j--) {
                    addClause(i,j, -1, EnvelopeFutureOffset);
                }
            }
        }else{
            for (int i = 1; i <= WorldDim; i++) {
                for (int j = y+1; j <= WorldDim; j++) {
                    addClause(i,j,-1,EnvelopeFutureOffset);
                }
            }
        }
    }




    /**
    *  This function should add all the clauses stored in the list
    *  futureToPast to the formula stored in solver.
    *   Use the function addClause( VecInt ) to add each clause to the solver
    *
    **/
    public void addLastFutureClausesToPastClauses() throws  IOException, ContradictionException, TimeoutException {
        if(futureToPast != null){
            for(VecInt v: futureToPast){
                solver.addClause(v);
            }
        }
    }


    /**
    * This function should check, using the future variables related
    * to possible positions of Envelope, whether it is a logical consequence
    * that an envelope is NOT at certain positions. This should be checked for all the
    * positions of the Envelope World.
    * The logical consequences obtained, should be then stored in the futureToPast list
    * but using the variables corresponding to the "past" variables of the same positions
    *
    * An efficient version of this function should try to not add to the futureToPast
    * conclusions that were already added in previous steps, although this will not produce
    * any bad functioning in the reasoning process with the formula.
    **/
    public void  performInferenceQuestions() throws  IOException,
            ContradictionException, TimeoutException
    {
        futureToPast = new ArrayList<>();
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = 1; j <= WorldDim; j++) {
                // Get variable number for position 2,3 in past variables
                int linealIndex = coordToLineal(2, 3, EnvelopeFutureOffset);
                // Get the same variable, but in the past subset
                int linealIndexPast = coordToLineal(2, 3, EnvelopePastOffset);

                VecInt variablePositive = new VecInt();
                variablePositive.insertFirst(linealIndex);

                // Check if Gamma + variablePositive is unsatisfiable:
                // This is only AN EXAMPLE for a specific position: (2,3)
                if (!(solver.isSatisfiable(variablePositive))) {
                    // Add conclusion to list, but rewritten with respect to "past" variables
                    VecInt concPast = new VecInt();
                    concPast.insertFirst(-(linealIndexPast));
                    futureToPast.add(concPast);
                    efstate.set( 2 , 3 , "X" );
        }

    }

    /**
    * This function builds the initial logical formula of the agent and stores it
    * into the solver object.
    *
    *  @return returns the solver object where the formula has been stored
    **/
    public ISolver buildGamma() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException
    {
        int totalNumVariables;

        // You must set this variable to the total number of boolean variables
        // in your formula Gamma
        totalNumVariables = WorldLinealDim*4 + WorldLinealDim*2 + WorldLinealDim*2;
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // This variable is used to generate, in a particular sequential order,
        // the variable indentifiers of all the variables
        actualLiteral = 1;

        // call here functions to add the differen sets of clauses
        // of Gamma to the solver object
        pastState(); //Treasure state t-1
        futureState(); //Treasure state t+1
        pastTofutureState(); //Treasure state t-1 to Treasure state t+1

        detectorClauses(); //Implications from the metal sensor
        envelopeClauses();   //envelope implications

        notInInitialPos(); //Implicates that the treasure is not in the initial position


        return solver;
    }

    /**
     * We need to add all the clauses for the possible implications a envelope may have.
     *
     *  @throws ContradictionException it must be included when adding clauses to a solver,
     *      *      * it prevents from inserting contradictory clauses in the formula.
     */
    private void envelopeClauses() throws ContradictionException {
        for (int k = 0; k < 2; k++) {
            for (int i = 1; i <= WorldDim; i++) {
                for (int j = 1; j <= WorldDim; j++) {
                    if (k == 0) {
                        if (envelopeAboveOffset == 0) {
                            envelopeAboveOffset = actualLiteral;
                        }
                        envelopeAboveImpl(i, j);
                    } else {
                        if (envelopeBelowOffset == 0) {
                            envelopeBelowOffset = actualLiteral;
                        }
                        envelopeBelowImpl(i, j);
                    }
                    actualLiteral++;
                }
            }
        }
    }

    /**
     * We need to add all the clauses for the possible implications the detector may have
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *      * it prevents from inserting contradictory clauses in the formula.
     */
    private void detectorClauses() throws ContradictionException {
        for (int k = 1; k < 5; k++) { //Possible values of our detector
            for (int i = 1; i <= WorldDim; i++) {
                for (int j = 1; j <= WorldDim; j++) {
                    switch (k){
                        case 1:
                            if(Detector1Offset == 0){ Detector1Offset = actualLiteral;}
                            detectorImplications(i,j,0,Detector1Offset);
                            break;
                        case 2:
                            if(Detector2Offset == 0){ Detector2Offset = actualLiteral;}
                            detectorImplications(i,j,1,Detector2Offset);
                            break;
                        case 3:
                            if(Detector3Offset == 0){ Detector3Offset = actualLiteral;}
                            detectorImplications(i,j,2,Detector3Offset);
                            break;
                        case 4:
                            if(Detector3Offset == 0){ Detector3Offset = actualLiteral;}
                            detectorImplications(i,j,3,Detector3Offset);
                            break;
                        case 5:
                            if(Detector3Offset == 0){ Detector3Offset = actualLiteral;}
                            detectorImplications(i,j,4,Detector3Offset);
                            break;
                    }
                    actualLiteral++;
                }
            }
        }
    }

    /**Adds the implications between detector cases 1,2,3 and the locations where we are sure
     * the treasure can not be.
     *
     * @param x detector x coord
     * @param y detector y coord
     * @param range it specifies the range  that the detector returns
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *      * it prevents from inserting contradictory clauses in the formula.
     */
    private void detectorImplications(int x, int y, int range, int offset) throws ContradictionException {
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = 1; j <= WorldDim; j++) {
                if(Math.abs(i-x)==range || Math.abs(j-y)==range){}
                else{
                    VecInt implication = new VecInt();
                    implication.insertFirst(-(coordToLineal(x,y,offset)));
                    implication.insertFirst(-(coordToLineal(i,j,TreasureFutureOffset)));
                    solver.addClause(implication);
                }
            }
        }
    }
    /**
     *Case0 is a bit different from the other ranges implications because the others just look at a certain distance
     * from the actual coord while case0 says; the treasure it's beyond Math.abs(i,j-x,y)>=3.
     *
     * @param x detector x coord
     * @param y detector y coord
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *      * it prevents from inserting contradictory clauses in the formula.
     */
    private void detectorImplicationsCase0(int x, int y, int offset) throws ContradictionException {
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = 1; j <= WorldDim; j++) {
                if(Math.abs(i-x)>=3 || Math.abs(j-y)>=3){}
                else{
                    VecInt implication = new VecInt();
                    implication.insertFirst(-(coordToLineal(x,y,offset)));
                    implication.insertFirst(-(coordToLineal(i,j,EnvelopeFutureOffset)));
                    solver.addClause(implication);
                }
            }
        }
    }

    /**Adds the implications between envelope saying treasure is above and locations
     * were the trasure cannot be located.
     *
     * @param x detector x coord
     * @param y detector y coord
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *      * it prevents from inserting contradictory clauses in the formula.
     */
    private void envelopeAboveImpl(int x, int y) throws ContradictionException {
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = y; j >0; j--) {
                VecInt implication = new VecInt();
                implication.insertFirst(-(coordToLineal(x, y, envelopeAboveOffset)));
                implication.insertFirst(-(coordToLineal(i, j, TreasureFutureOffset)));
                solver.addClause(implication);
            }
        }
    }

    /**Adds the implications between detector cases 1,2,3 and the locations where we are sure
     * the treasure can not be.
     *
     * @param x detector x coord
     * @param y detector y coord
     * @throws ContradictionException it must be included when adding clauses to a solver,
     *      * it prevents from inserting contradictory clauses in the formula.
     */
    private void envelopeBelowImpl(int x, int y) throws ContradictionException {
        for (int i = 1; i <= WorldDim; i++) {
            for (int j = y; j <= WorldDim; j++) {
                VecInt implication = new VecInt();
                implication.insertFirst(-(coordToLineal(x, y, envelopeBelowOffset)));
                implication.insertFirst(-(coordToLineal(i, j, EnvelopeFutureOffset)));
                solver.addClause(implication);
            }
        }
    }

    /**
     *It add a clause to the solver that implies that the envelope must be
     * in a position considering past information.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     *
     **/
    private void pastState() throws ContradictionException {
        // VecInt its the vector that use the solver for primitive integers.
        VecInt pastInf = new VecInt();
        EnvelopeFutureOffset = actualLiteral;
        for (int i = 0; i < WorldLinealDim; i++) {
            pastInf.insertFirst(actualLiteral);
            actualLiteral+=1;
        }
        solver.addClause(pastInf);
    }

    /**
     *It add a clause to the solver that implies that the envelope must be
     * in a position considering future information.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     *
     **/
    private void futureState() throws ContradictionException {
        VecInt futureInf = new VecInt();
        EnvelopeFutureOffset = actualLiteral;
        for (int i = 0; i < WorldLinealDim; i++) {
            futureInf.insertFirst(actualLiteral);
            actualLiteral+=1;
        }
        solver.addClause(futureInf);
    }

    /**Adds the clauses which say that if we have concluded that the Treasure was not in an specific
     * location, we have to keep these conclusions and those will still be true in the future
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
     */

    private void pastTofutureState() throws  ContradictionException{
        for (int i = 0; i < WorldLinealDim ; i++) {
            VecInt clause = new VecInt();
            clause.insertFirst(i+1);
            clause.insertFirst(-(EnvelopeFutureOffset+i));
            solver.addClause(clause);
        }
    }

    /**Adds the clauses which say that the Treasure can't be found at (1,1) position.
     * We need to add one clause for the future and one for the past.
     *
     * @throws ContradictionException it must be included when adding clauses to a solver,
     * it prevents from inserting contradictory clauses in the formula.
             */
    private void notInInitialPos() throws ContradictionException{
        VecInt clause = new VecInt();
        clause.insertFirst(-EnvelopeFutureOffset);
        solver.addClause(clause);
        clause.clear();
        clause.insertFirst(-EnvelopePastOffset);
        solver.addClause(clause);
    }


     /**
     * Convert a coordinate pair (x,y) to the integer value  t_[x,y]
     * of variable that stores that information in the formula, using
     * offset as the initial index for that subset of position variables
     * (past and future position variables have different variables, so different
     * offset values)
     *
     *  @param x x coordinate of the position variable to encode
     *  @param y y coordinate of the position variable to encode
     *  @param offset initial value for the subset of position variables
     *         (past or future subset)
     *  @return the integer indentifer of the variable  b_[x,y] in the formula
    **/
    public int coordToLineal(int x, int y, int offset) {
        return ((x - 1) * WorldDim) + (y - 1) + offset;
    }

    /**
     * Perform the inverse computation to the previous function.
     * That is, from the identifier t_[x,y] to the coordinates  (x,y)
     *  that it represents
     *
     * @param lineal identifier of the variable
     * @param offset offset associated with the subset of variables that
     *        lineal belongs to
     * @return array with x and y coordinates
    **/
    public int[] linealToCoord(int lineal, int offset){
        lineal = lineal - offset + 1;
        int[] coords = new int[2];
        coords[1] = ((lineal-1) % WorldDim) + 1;
        coords[0] = (lineal - 1) / WorldDim + 1;
        return coords;
    }



}
