package myasm;

import java.util.*;
import java.io.*;

public class MyAssembler {
  int m_linenum;
  int m_orglinenum;
  HashSet m_keywords;
  Hashtable m_labelTable;
  
  BufferedReader m_in;
  BufferedWriter m_out;
  
  public static void main(String[] args) {
    MyAssembler assembler = new MyAssembler();
    try {
      if (args.length != 2) {
      	throw new IllegalArgumentException("useage: MyAssembler infile outfile");
      }

      assembler.m_in = new BufferedReader(new FileReader(args[0]));
      assembler.m_out = new BufferedWriter(new FileWriter(args[1]));
      
      assembler.translate(assembler.new FirstPassTranslater());
      assembler.m_in.close();
      assembler.m_in = new BufferedReader(new FileReader(args[0]));
      assembler.translate(assembler.new ToHexTranslater());
      assembler.m_in.close();
      assembler.m_out.close();
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
      System.exit(1);
    }
  }
  
  MyAssembler() {
    m_keywords = new HashSet();
    m_labelTable = new Hashtable();
    
    m_keywords.add(".db");
    m_keywords.add("add");
    m_keywords.add("sub");
    m_keywords.add("mov");
    m_keywords.add("dec");
    m_keywords.add("inc");
    m_keywords.add("and");
    m_keywords.add("lda");
    m_keywords.add("sta");
    m_keywords.add("rrc");
    m_keywords.add("rlc");
    m_keywords.add("jz");
    m_keywords.add("jc");
    m_keywords.add("ja0");
    m_keywords.add("jmp");
    m_keywords.add("halt");
  }
  
  int getLabelValue(String str) {
	  try {
      return ((Integer)m_labelTable.get(str + ":")).intValue();
    } catch (NullPointerException e) {
      myAssert(false);
      return 0;
    }
  }
  
  Token lexan(String line) {
    if (line == null || line.equals("")) return null;
    
    Token token = new Token();
    String[] str;
    do {
      str = line.split("\\s+", 2);
      if (str.length == 2) line = str[1];
      else line = "";
    } while (!line.matches("^\\s*$") && str[0].matches("^\\s*$"));
    
    if (str[0].matches("^\\s*$")) return null;
    
    token.value = str[0];
    if (str.length == 2) token.rest = str[1];
    else token.rest = null;
    
    return token;
  }
  
  boolean isLabel(String str) {
    return str.charAt(str.length() - 1) == ':';
  }
  
  boolean isVerb(String str) {
    return m_keywords.contains(str);
  }
  
  boolean isRegister(String str) {
    return  str.length() == 2 && 
    				str.charAt(0) == 'r' && (
    				  str.charAt(1) == '0' ||
    				  str.charAt(1) == '1' ||
    				  str.charAt(1) == '2' ||
    				  str.charAt(1) == '3'
    				);
  }
  
  Statement parse(String line) {
    Statement stat = new Statement();
    stat.linenum = m_linenum;
    stat.originStr = line;
    
    try {
      Token token = lexan(line);
      if (token == null) return stat;
      if (isLabel(token.value)) {
        stat.label = token.value;
        token = lexan(token.rest);
        if (token == null) return stat;
      }
      
      if (isVerb(token.value)) {
        stat.verb = token.value;
        if (token.rest != null)
          stat.params = token.rest.split("\\s*,\\s*");
      } else {
        myAssert(false);
      }
    } catch (NullPointerException e) {
      myAssert(false);
    }
    return stat;
  }

  class MyAssertFailure extends RuntimeException {
    public MyAssertFailure(String str) {
      super(str);
    }
  }
  
  void myAssert(boolean bool) {
    if (!bool) throw new MyAssertFailure("invalid line: " + m_orglinenum);
  }
  
  void translate(Translater t) {
    m_linenum = 0;
    m_orglinenum = 0;
    String line;
    try {
      while ((line = m_in.readLine()) != null) {
        m_orglinenum++;
        if (line.equals("")) continue;
        try {
          Statement stmt = parse(line.split("\\s*;", 2)[0].toLowerCase());
        
          t.process(stmt);
        } catch (MyAssertFailure e) {
          System.err.println(e.getMessage());
        }
      }
    } catch (IOException ioe) {
      System.err.println(ioe.getMessage());
      System.exit(1);
    }
  }
  
  class FirstPassTranslater implements Translater {
    public void process(Statement stmt) {
      if (stmt.label != null) {
        myAssert(m_labelTable.get(stmt.label) == null);
        m_labelTable.put(stmt.label, new Integer(m_linenum));
      }
      
      if (stmt.verb == null) return;
      
      if (stmt.verb.equals(".db")) {                          // .db
        m_linenum++;
      } else 
      if (stmt.verb.equals("add")) {                          // add a, ri
        m_linenum++;
        myAssert(stmt.params[0].equals("a"));
        myAssert(isRegister(stmt.params[1]));
      } else 
      if (stmt.verb.equals("sub")) {                          // sub a, ri
        m_linenum++;
        myAssert(stmt.params[0].equals("a"));
        myAssert(isRegister(stmt.params[1]));
      } else 
      if (stmt.verb.equals("mov")) {
        if (isRegister(stmt.params[0])) {
          if (stmt.params[1].equals("a")) {                   // mov ri, a
            m_linenum++;
          } else {  																					// mov ri, data
            m_linenum += 2;
          }
        } else
        if (stmt.params[0].equals("a")) {
          if (isRegister(stmt.params[1])) {			              // mov a, ri
            m_linenum++;
          } else
          if (stmt.params[1].charAt(0) == '@') {							// mov a, @ri
            m_linenum++;
          } else {																						// mov a, data
            m_linenum += 2;
          }
        }
      } else 
      if (stmt.verb.equals("and")) {                          // and a, ri
        m_linenum++;
        myAssert(stmt.params[0].equals("a"));
        myAssert(isRegister(stmt.params[1]));
      } else 
      if (stmt.verb.equals("dec")) {                          // dec a, ri
        m_linenum++;
        myAssert(stmt.params[0].equals("a"));
      } else 
      if (stmt.verb.equals("inc")) {													// inc a, ri
        m_linenum++;
        myAssert(stmt.params[0].equals("a"));
      } else
      if (stmt.verb.equals("lda")) {                          // lda addr
        m_linenum += 2;
      } else 
      if (stmt.verb.equals("sta")) {
        m_linenum += 2;
      } else 
      if (stmt.verb.equals("rrc")) {                          // rrc a
        m_linenum++;
        myAssert(stmt.params[0].equals("a"));
      } else 
      if (stmt.verb.equals("jz")) {                           // jz addr
        if (stmt.params[0].charAt(0) == '(')
          m_linenum++;
        else
          m_linenum += 2;
      } else 
      if (stmt.verb.equals("jc")) {                           // jz addr
        if (stmt.params[0].charAt(0) == '(')
          m_linenum++;
        else
          m_linenum += 2;
      } else 
      if (stmt.verb.equals("ja0")) {                          // ja0 addr
        if (stmt.params[0].charAt(0) == '(')
          m_linenum++;
        else
          m_linenum += 2;
      } else 
      if (stmt.verb.equals("jmp")) {                          // jmp addr
        if (stmt.params[0].charAt(0) == '(')
          m_linenum++;
        else
          m_linenum += 2;
      } else 
      if (stmt.verb.equals("halt")) {                         // halt
        m_linenum++;
      } else {
        myAssert(false);
      }

    }
  }
  
  String toHex(int val, int len) {
    char[] table = new char[] {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };     // lower case
  //  '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };     // UPPER CASE
    StringBuffer str = new StringBuffer();
    while (val != 0) {
      int rem = val % 0x10;
      val /= 0x10;
      str.insert(0, String.valueOf(table[rem]));
    }
    while (str.length() < len) {
      str.insert(0, "0");
    }
    return str.toString();
  }
  
  String formatByte(int val) {
    return toHex(val, 2);
  }
  
  String formattedLineNumber() {
    return toHex(m_linenum, 3);
  }
  
  int myParseInt(String str) {
    return myParseInt(str, 0);
  }
  
  int myParseInt(String str, int pos) {
    if (Character.isDigit(str.charAt(0))) {
	    int radix = 10;
	    if (str.length() >= 2 && str.substring(0, 2).equals("0x")) {
	      radix = 16;
	      pos += 2;
	    }
	    return Integer.parseInt(str.substring(pos), radix);
    } else {
      return getLabelValue(str);
    }
  }
  
  class ToHexTranslater implements Translater {
    public void process(Statement stmt) {
      StringBuffer outline = new StringBuffer();
      if (stmt.verb == null) return;
      if (stmt.verb.equals(".db")) {                          // .db
        outline.append(formattedLineNumber()).append("\t");
        outline.append(formatByte(myParseInt(stmt.params[0])));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("add")) {                          // add a, ri
        int opcode = 0x00;
        
        outline.append(formattedLineNumber()).append("\t");
        int ri = stmt.params[1].charAt(1) - '0';
        myAssert(0 <= ri && ri < 4);
        outline.append(formatByte(opcode + ri));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("sub")) {                          // sub a, ri
        int opcode = 0x04;
        
        outline.append(formattedLineNumber()).append("\t");
        int ri = stmt.params[1].charAt(1) - '0';
        myAssert(0 <= ri && ri < 4);
        outline.append(formatByte(opcode + ri));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("mov")) {
        if (isRegister(stmt.params[0])) {
          if (stmt.params[1].equals("a")) {                   // mov ri, a
	          int opcode = 0x40;
	          
	          outline.append(formattedLineNumber()).append("\t");
	          int ri = stmt.params[0].charAt(1) - '0';
	          myAssert(0 <= ri && ri < 4);
	          outline.append(formatByte(opcode + ri));
	          outline.append("\t; ").append(stmt.originStr).append("\n");
	          
            m_linenum++;
          } else {																				   // mov ri, data
	          int opcode = 0x60;
	          
	          int ri = stmt.params[0].charAt(1) - '0';
	          myAssert(0 <= ri && ri < 4);
	          
	          int data = myParseInt(stmt.params[1]);
	          
	          outline.append(formattedLineNumber()).append("\t");
	          outline.append(formatByte(opcode + ri));
	          outline.append("\t; ").append(stmt.originStr).append("\n");
	          m_linenum++;
	          
	          outline.append(formattedLineNumber()).append("\t");
	          outline.append(formatByte(data));
	          outline.append("\t; \n");
	          
	          m_linenum++;
          }
        } else
        if (stmt.params[0].equals("a")) {
          if (isRegister(stmt.params[1])) {              // mov a, ri
	          int opcode = 0xc4;
	          
	          outline.append(formattedLineNumber()).append("\t");
	          int ri = stmt.params[1].charAt(1) - '0';
	          myAssert(0 <= ri && ri < 4);
	          outline.append(formatByte(opcode + ri));
	          outline.append("\t; ").append(stmt.originStr).append("\n");
	          
	          m_linenum++;
          } else
          if (stmt.params[1].charAt(0) == '@') {							// mov a, @ri
	          int opcode = 0x20;
	          
	          outline.append(formattedLineNumber()).append("\t");
	          int ri = stmt.params[1].charAt(2) - '0';
	          myAssert(0 <= ri && ri < 4);
	          outline.append(formatByte(opcode + ri));
	          outline.append("\t; ").append(stmt.originStr).append("\n");
	          
	          m_linenum++;
          } else {   																					// mov a, data
	          int opcode = 0x44;
	          
	          int data = myParseInt(stmt.params[1]);
	          data &= 0xff;		// allow one byte only
	          
	          outline.append(formattedLineNumber()).append("\t");
	          outline.append(formatByte(opcode));
	          outline.append("\t; ").append(stmt.originStr).append("\n");
	          m_linenum++;
	          
	          outline.append(formattedLineNumber()).append("\t");
	          outline.append(formatByte(data));
	          outline.append("\t; \n");
	          
	          m_linenum++;
          }
        }
      } else 
      if (stmt.verb.equals("and")) {                          // and a, ri
        int opcode = 0x24;
        
        outline.append(formattedLineNumber()).append("\t");
        int ri = stmt.params[1].charAt(1) - '0';
        myAssert(0 <= ri && ri < 4);
        outline.append(formatByte(opcode + ri));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("dec")) {                          // dec a, ri
        int opcode = 0x64;
        
        outline.append(formattedLineNumber()).append("\t");
        int ri = 0;
        if (stmt.params.length > 1) {			// allow both 'dec a' and 'dec a, ri'
	        ri = stmt.params[1].charAt(1) - '0';
	        myAssert(0 <= ri && ri < 4);
        }
        outline.append(formatByte(opcode + ri));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("inc")) {                          // inc a, ri
	      int opcode = 0xa4;
	      
	      outline.append(formattedLineNumber()).append("\t");
	      int ri = 0;
	      if (stmt.params.length > 1) {			// allow both 'dec a' and 'dec a, ri'
	        ri = stmt.params[1].charAt(1) - '0';
	        myAssert(0 <= ri && ri < 4);
	      }
	      outline.append(formatByte(opcode + ri));
	      outline.append("\t; ").append(stmt.originStr).append("\n");
	      
	      m_linenum++;
	    } else 
      if (stmt.verb.equals("lda")) {                          // lda addr
        int opcode = 0x80;
        
        int addr = myParseInt(stmt.params[0]);
        int addrh = addr >> 8;
        int addrl = addr & 0xff;
        
        outline.append(formattedLineNumber()).append("\t");
        outline.append(formatByte(opcode + addrh));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        m_linenum++;
        
        outline.append(formattedLineNumber()).append("\t");
        outline.append(formatByte(addrl));
        outline.append("\t; \n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("sta")) {                          // sta addr
        int opcode = 0xa0;
        
        int addr = myParseInt(stmt.params[0]);
        int addrh = addr >> 8;
        int addrl = addr & 0xff;
        
        outline.append(formattedLineNumber()).append("\t");
        outline.append(formatByte(opcode + addrh));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        m_linenum++;
        
        outline.append(formattedLineNumber()).append("\t");
        outline.append(formatByte(addrl));
        outline.append("\t; \n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("rrc")) {                          // rrc a, ri
        int opcode = 0xc0;
        
        outline.append(formattedLineNumber()).append("\t");
        int ri = 0;
        if (stmt.params.length > 1) {			// allow both 'rrc a' and 'rrc a, ri'
	        ri = stmt.params[1].charAt(1) - '0';
	        myAssert(0 <= ri && ri < 4);
        }
        outline.append(formatByte(opcode + ri));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        
        m_linenum++;
      } else 
      if (stmt.verb.equals("jz")) {                           // jz addr
        int opcode = 0xe0;
        if (stmt.params[0].charAt(0) == '(') {
          String straddrh = stmt.params[0];
          int addrh = ((straddrh.charAt(1) - '0') << 2) +
                      ((straddrh.charAt(2) - '0') << 1) + 
                       (straddrh.charAt(3) - '0');
          myAssert(0 <= addrh && addrh < 8);
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
        } else {
          int addr = myParseInt(stmt.params[0]);
          int addrh = addr >> 8;
          int addrl = addr & 0xff;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(addrl));
          outline.append("\t; \n");
                    
          m_linenum++;
        }
      } else 
      if (stmt.verb.equals("jc")) {                           // jz addr
        int opcode = 0xe8;
        if (stmt.params[0].charAt(0) == '(') {
          String straddrh = stmt.params[0];
          int addrh = ((straddrh.charAt(1) - '0') << 2) +
                      ((straddrh.charAt(2) - '0') << 1) + 
                       (straddrh.charAt(3) - '0');
          myAssert(0 <= addrh && addrh < 8);
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
        } else {
          int addr = myParseInt(stmt.params[0]);
          int addrh = addr >> 8;
          int addrl = addr & 0xff;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(addrl));
          outline.append("\t; \n");
                    
          m_linenum++;
        }
      } else 
      if (stmt.verb.equals("ja0")) {                          // ja0 addr
        int opcode = 0xf0;
        if (stmt.params[0].charAt(0) == '(') {
          String straddrh = stmt.params[0];
          int addrh = ((straddrh.charAt(1) - '0') << 2) +
                      ((straddrh.charAt(2) - '0') << 1) + 
                       (straddrh.charAt(3) - '0');
          myAssert(0 <= addrh && addrh < 8);
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
        } else {
          int addr = myParseInt(stmt.params[0]);
          int addrh = addr >> 8;
          int addrl = addr & 0xff;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(addrl));
          outline.append("\t; \n");
                    
          m_linenum++;
        }
      } else 
      if (stmt.verb.equals("jmp")) {                          // jmp addr
        int opcode = 0xf8;
        if (stmt.params[0].charAt(0) == '(') {
          String straddrh = stmt.params[0];
          int addrh = ((straddrh.charAt(1) - '0') << 2) +
                      ((straddrh.charAt(2) - '0') << 1) + 
                       (straddrh.charAt(3) - '0');
          myAssert(0 <= addrh && addrh < 8);
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
        } else {
          int addr = myParseInt(stmt.params[0]);
          int addrh = addr >> 8;
          int addrl = addr & 0xff;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(opcode + addrh));
          outline.append("\t; ").append(stmt.originStr).append("\n");
          m_linenum++;
          
          outline.append(formattedLineNumber()).append("\t");
          outline.append(formatByte(addrl));
          outline.append("\t; \n");
                    
          m_linenum++;
        }
      } else 
      if (stmt.verb.equals("halt")) {                         // halt
        int opcode = 0xff;
        outline.append(formattedLineNumber()).append("\t");
        outline.append(formatByte(opcode));
        outline.append("\t; ").append(stmt.originStr).append("\n");
        m_linenum++;
      } else {
        myAssert(false);
      }

      try {
        m_out.write(outline.toString());
      } catch (IOException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
    }
  }
}

interface Translater {
  void process(Statement stmt);
}

class Token {
  String value;
  String rest;
}

class Statement {
  int linenum;
  String label;
  String verb;
  String []params;
  String type;
  String originStr;
}
