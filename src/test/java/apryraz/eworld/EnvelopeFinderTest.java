package apryraz.eworld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;

import org.sat4j.core.VecInt;
import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;


import apryraz.eworld.*;

import static org.junit.Assert.assertEquals;
import org.junit.*;

/**
*  Class for testing the EnvelopeFinder agent
**/
public class EnvelopeFinderTest {


 /**
 *  This function should execute the next step of the agent, and the assert
 *  whether the resulting state is equal to the targetState
 *
 *  @param eAgent       EnvelopeFinder agent
 *  @param targetState  the state that should be equal to the resulting state of
 *                      the agent after performing the next step
 **/
  public void testMakeSimpleStep(  EnvelopeFinder eAgent,
                                   EFState targetState )  throws
                                           IOException,  ContradictionException, TimeoutException {
    // Check (assert) whether the resulting state is equal to
    //  the targetState after performing action runNextStep with bAgent
      eAgent.runNextStep();
  }


/**
*  Read an state from the current position of the file trough the
*  BufferedReader object
*
*  @param br BufferedReader object interface to the opened file of states
*  @param wDim dimension of the world
**/
  public EFState readTargetStateFromFile( BufferedReader br, int wDim )  throws
          IOException {
       EFState efstate = new EFState(wDim);
       String row;
       String[] rowvalues;

       for (int i = wDim; i >= 1; i--) {
           row = br.readLine();
           rowvalues = row.split(" ");
           for (int j = 1; j <= wDim; j++) {
              efstate.set(i,j,rowvalues[j-1]);
           }
        }
        return efstate;
  }

/**
*  Load a sequence of states from a file, and return the list
*
*  @param wDim        dimension of the world
*  @param numStates   num of states to read from the file
*  @param statesFile file name with sequence of target states, that should
*                      be the resulting states after each movement in fileSteps
*
*  @return returns an ArrayList of EFState with the resulting list of states
**/
  ArrayList<EFState> loadListOfTargetStates( int wDim, int numStates, String statesFile ) {

     ArrayList<EFState> listOfStates = new ArrayList<EFState>(numStates);

     try {
         BufferedReader br = new BufferedReader(new FileReader(statesFile));
         String row;

         // steps = br.readLine();
         for (int s = 0; s < numStates ; s++ ) {
           listOfStates.add(readTargetStateFromFile(br,wDim));
            // Read a blank line between states
            row = br.readLine();
         }
         br.close();
     } catch (FileNotFoundException ex) {
         System.out.println("MSG.   => States file not found");
         exit(1);
     } catch (IOException ex) {
         Logger.getLogger(EnvelopeFinderTest.class.getName()).log(Level.SEVERE, null, ex);
         exit(2);
     }

     return listOfStates;
  }


  /**
  *   This function should run the sequence of steps stored in the file fileSteps,
  *   but only up to numSteps steps.
  *
  *   @param wDim the dimension of world
  *   @param numSteps num of steps to perform
  *   @param fileSteps file name with sequence of steps to perform
  *   @param fileStates file name with sequence of target states, that should
  *                      be the resulting states after each movement in fileSteps
  *   @param fileEnvelopes
  *
  **/
  public void testMakeSeqOfSteps( int wDim, int numSteps, String fileSteps, String fileStates, String fileEnvelopes  )
       throws   IOException,  ContradictionException, TimeoutException {
      // You should make EnvelopeFinder and EnvelopeWorldEnv objects to  test.
      // Then load sequence of target states, load sequence of steps into the eAgent
      // and then test the sequence calling testMakeSimpleStep once for each step.
     EnvelopeFinder eAgent = new EnvelopeFinder(wDim);
     // load information about the World into the EnvAgent
     EnvelopeWorldEnv envAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);
     // Load list of states
     ArrayList<EFState> seqOfStates = loadListOfTargetStates(wDim,numSteps,fileStates);;


     // Set environment agent and load list of steps into the finder agent
     eAgent.loadListOfSteps(  numSteps, fileSteps ) ;
     eAgent.setEnvironment( envAgent );
  
     // Test here the sequence of steps and check the resulting states with the
     // ones in seqOfStates
     for (int i = 0; i < numSteps; i++) { testMakeSimpleStep(eAgent,seqOfStates.get(i)); }
  }

    /**
     *   This functions should run the test to check the good execution
     *   of the programm
     *
     *   @param wDim the dimension of world
     *   @param numSteps num of steps to perform
     *   @param fileSteps file name with sequence of steps to perform
     *   @param fileStates file name with sequence of target states, that should
     *                      be the resulting states after each movement in fileSteps
     *   @param fileEnvelopes
     *
     **/

  /**
  *   This is an example test. You must replicate this method for each different
  *    test sequence, or use some kind of parametric tests with junit
  **/
  @Test public void EFinderTest1()   throws
          IOException,  ContradictionException, TimeoutException {

    testMakeSeqOfSteps(5, 5, "tests/steps1.txt", "tests/states1.txt", "tests/envelopes1.txt");
  }

  /**
  * Tests the specific configuration of: "steps2.txt" , "states2.txt", "envelopes2.txt"
  *                        5x5 world, Envelope at 3,2 and 7 steps.
  *
  * @throws IOException            Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
  *                                exceeds the timeout.
  **/
  @Test public void EFinderTest2()   throws
        IOException,  ContradictionException, TimeoutException {
    testMakeSeqOfSteps(  5,7, "tests/steps2.txt", "tests/states2.txt", "tests/envelopes2.txt" );
  }

  /**
  * Tests the specific configuration of: "steps3.txt" , "states3.txt", "envelopes3.txt"
  *                        7x7 world, Envelope at 3,2 and 6 steps.
  *
  * @throws IOException            Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
  *                                exceeds the timeout.
  **/
  @Test public void EFinderTest3()   throws IOException,  ContradictionException, TimeoutException {
    testMakeSeqOfSteps(  7, 6, "tests/steps3.txt", "tests/states3.txt", "tests/envelopes3.txt" );
  }
  /**
  * Tests the specific configuration of: "steps4.txt" , "states4.txt", "envelopes4.txt"
  *                        7x7 world, Envelope at 4,4 and 12 steps.
  *
  * @throws IOException            Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException       needed for solver.isSatisfiable method, its thrown if
  *                                 exceeds the timeout.
  **/
  @Test public void EFinderTest4()   throws IOException,  ContradictionException, TimeoutException {
    testMakeSeqOfSteps(  7,  12, "tests/steps4.txt", "tests/states4.txt", "tests/envelopes4.txt" );
  }
}
