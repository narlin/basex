package org.basex.test.cs;

import java.io.IOException;
import org.basex.server.ClientSession;

/**
 * Testing the semaphore.
 * 
 * Prerequisites:
 * Run BaseXServer...
 * Create Factbook DB...
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Andreas Weiler
 */
public class SemaphoreTest {

  /**
   * Main method of the example class.
   * @param args (ignored) command-line arguments
   */
  public static void main(String[] args) {
    new SemaphoreTest().run();
  }
  
  /**
   * Runs the test
   */
  void run() {
    System.out.println("=== Semaphore Test ===");
    final String [] queries = {"xquery for $n in doc('factbook')//city return insert node <test/> into $n",
        "xquery for $n in 1 to 1000000 where $n = 999999 return $n",
        "xquery for $n in doc('factbook')//city return insert node <test/> into $n",
        "xquery for $n in 1 to 1000000 where $n = 999999 return $n"};
    
    for (int n = 1; n <= 4; n++) {
      final int j = n;
      try {
        Thread.sleep(2000);
      } catch(InterruptedException e1) {
        e1.printStackTrace();
      }
      new Thread() {
        @Override
        public void run() {
          try {
            ClientSession session = new ClientSession("localhost", 1984, "admin", "admin");
            session.execute(queries[j - 1]);
            System.out.println("=== Client Done: " + j + " ===");
          } catch(IOException e) {
            e.printStackTrace();
          }
        }
      }.start();
    }
  }
}
