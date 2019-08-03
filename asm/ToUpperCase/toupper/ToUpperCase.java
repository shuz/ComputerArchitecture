package toupper;

import java.io.*;

public class ToUpperCase {
  static BufferedReader m_in;
  static PrintWriter m_out;
  
  public static void main(String[] args) {
    try {
      if (args.length != 2) {
      	throw new IllegalArgumentException("useage: MyAssembler infile outfile");
      }

      m_in = new BufferedReader(new FileReader(args[0]));
      m_out = new PrintWriter(new BufferedWriter(new FileWriter(args[1])));
      
      String line;
      try {
        while ((line = m_in.readLine()) != null) {
          m_out.println(line.toUpperCase());
        }
      } catch (IOException ioe) {
        System.err.println(ioe.getMessage());
        System.exit(1);
      }
      
      m_in.close();
      m_out.close();
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
      System.exit(1);
    }
  }
  
}
