/**
 * Treball realitzat per:
 *
 * Arnau Molins Carbelo 48254845V
 * Rubén Querol Cervantes 39939067G
 * Joel Romia Aribau 73210823Y
 **/
package apryraz.eworld;



import java.io.IOException;
import java.util.ArrayList;
import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


/**
  The class for the main program of the Barcenas World

**/
public class EnvelopeWorld {


   /**
   * This function should set the environment of the agent and execute the sequence of steps stored in the file fileSteps
   * Each step is executed with the function runNextStep() agent.
   *
   * @param wDim the dimension of world
   * @param numSteps num of steps to perform
   * @param fileSteps file name with sequence of steps to perform
   * @param fileEnvelopes file name with sequence of steps to perform
   *
   *
   * @throws IOException Signals that an I/O exception of some sort has occurred.
   * @throws ContradictionException if inserting contradictory information to solver.
   * @throws TimeoutException if runNextStep operation spends more
   * 	                       time computing than a certain timeout.
   **/


public static void runStepsSequence( int wDim, int numSteps, String fileSteps, String fileEnvelopes ) throws
                               IOException,  ContradictionException, TimeoutException {
   // Make instances of EnvelopeFinder agent and environment object classes
   EnvelopeFinder EAgent = new EnvelopeFinder(wDim);
   EnvelopeWorldEnv EnvAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);

  

    // Set environment object into EAgent
    EAgent.setEnvironment(EnvAgent);

    // Load list of steps into the Finder Agent
    EAgent.loadListOfSteps(numSteps, fileSteps);
    
    // Execute sequence of steps with the Agent
    for (int step = 0; step < numSteps; step++) {
        EAgent.runNextStep();
    }
}

    /**
    *  This function should load five arguments from the command line:
    *  arg[0] = dimension of the word
    *  arg[1] = num of steps to perform
    *  arg[2] = file name with sequence of steps to perform
    *  arg[3] = file name with list of envelopes positions
    *
    *
    * @throws IOException Signals that an I/O exception of some sort has occurred.
    * @throws ContradictionException if inserting contradictory information to solver.
    * @throws TimeoutException if opening file operation spends more
    * 	                       time computing than a certain timeout.
    **/
public static void main ( String[] args) throws IOException,  ContradictionException, TimeoutException {

    if (args.length < 3) {
        System.out.println("You must specify all arguments needed");
    } else {
        int wDim = Integer.parseInt(args[0]);
        int numSteps = Integer.parseInt(args[1]);
        String fileSteps = args[2];
        String fileEnvelopes = args[3];
        runStepsSequence(wDim,numSteps,fileSteps,fileEnvelopes);
    }
}
}