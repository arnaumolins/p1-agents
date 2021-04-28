/**
 * Treball realitzat per:
 *
 * Arnau Molins Carbelo 48254845V
 * Rubén Querol Cervantes 39939067G
 * Joel Romia Aribau 73210823Y
 **/
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
 *  This function should execute the next step of the agent, and the assertEqual
 *  whether the resulting agent state is equal to the targetState
 *
 *  @param eAgent       EnvelopeFinder agent
 *  @param targetState  the state that should be equal to the resulting state of
 *                      the agent after performing the next step
 **/
  public void testMakeSimpleStep(  EnvelopeFinder eAgent,
                                   EFState targetState )  throws
                                           IOException,  ContradictionException, TimeoutException {
      // Do next step of the agent
      eAgent.runNextStep();
      // Check (assertEquals) if the resulting state is equal to agent state
      assertEquals(eAgent.getState(), targetState);
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
  *   @param fileEnvelopes file name with sequence of envelopes positions that should appear in the world
  *
  * @throws IOException  Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException if inserting contradictory information to solver.
  * @throws TimeoutException if running any operation spends more
  * 	                     time computing than a certain timeout.
  **/
  public void testMakeSeqOfSteps( int wDim, int numSteps, String fileSteps, String fileStates, String fileEnvelopes  )
       throws   IOException,  ContradictionException, TimeoutException {
     // Load information about the world dimension in the agent
     EnvelopeFinder eAgent = new EnvelopeFinder(wDim);
     // Load information about the World into the envAgent
     EnvelopeWorldEnv envAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);
     // Load list of states
     ArrayList<EFState> seqOfStates = loadListOfTargetStates(wDim,numSteps,fileStates);;


     // Load list of steps into the finder agent
     // Set environment agent
     eAgent.loadListOfSteps(numSteps, fileSteps);
     eAgent.setEnvironment(envAgent);
  
     // For every step in the sequence of steps we check the resulting states with the ones in seqOfStates
     for (int i = 0; i < numSteps; i++) { testMakeSimpleStep(eAgent,seqOfStates.get(i)); }
  }

  /**
  * Tests the specific configuration of: "steps1.txt" , "states1.txt", "envelopes1.txt"
  * 5x5 world, Envelopes at ((2,2),(4,4)) and 5 steps.
  *
  * @throws IOException Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException needed for solver.isSatisfiable method, its thrown if exceeds the timeout.
  **/
  @Test public void EFinderTest1()   throws
          IOException,  ContradictionException, TimeoutException {

    testMakeSeqOfSteps(5, 5, "tests/steps1.txt", "tests/states1.txt", "tests/envelopes1.txt");
  }

  /**
  * Tests the specific configuration of: "steps2.txt" , "states2.txt", "envelopes2.txt"
  * 5x5 world, Envelope at ((3,2),(3,4)) and 7 steps.
  *
  * @throws IOException Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException needed for solver.isSatisfiable method, its thrown if
  *                          exceeds the timeout.
  **/
  @Test public void EFinderTest2()   throws
        IOException,  ContradictionException, TimeoutException {
    testMakeSeqOfSteps(  5,7, "tests/steps2.txt", "tests/states2.txt", "tests/envelopes2.txt" );
  }

  /**
  * Tests the specific configuration of: "steps3.txt" , "states3.txt", "envelopes3.txt"
  * 7x7 world, Envelope at ((3,2),(4,4),(2,6)) and 6 steps.
  *
  * @throws IOException Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException needed for solver.isSatisfiable method, its thrown if
  *                          exceeds the timeout.
  **/
  @Test public void EFinderTest3()   throws IOException,  ContradictionException, TimeoutException {
    testMakeSeqOfSteps(  7, 6, "tests/steps3.txt", "tests/states3.txt", "tests/envelopes3.txt" );
  }
  /**
  * Tests the specific configuration of: "steps4.txt" , "states4.txt", "envelopes4.txt"
  * 7x7 world, Envelope at ((6,2),(4,4),(2,6)) and 12 steps.
  *
  * @throws IOException Signals that an I/O exception of some sort has occurred.
  * @throws ContradictionException it must be included when adding clauses to a solver,
  *                           it prevents from inserting contradictory clauses in the formula.
  * @throws TimeoutException needed for solver.isSatisfiable method, its thrown if
  *                          exceeds the timeout.
  **/
  @Test public void EFinderTest4()   throws IOException,  ContradictionException, TimeoutException {
    testMakeSeqOfSteps(  7,  12, "tests/steps4.txt", "tests/states4.txt", "tests/envelopes4.txt" );
  }
}
