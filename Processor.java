import java.util.Scanner;

public class Processor {
   private StringBuilder res;
   private String accumulator;
   private Scanner scan;
   private int counter;
   private Memory memory; 
   
   public Processor(Memory mem) {
      this(mem, 0); // Default to starting at address 0
   }
   
   public Processor(Memory mem, int startAddress) {
      this.memory = mem;
      this.accumulator = "0000";
      this.counter = startAddress;
      this.scan = new Scanner(System.in);
      this.res = new StringBuilder();
   }

   public String getAcc() {
      return accumulator;
   }

   public void setAcc(String value) {
      int num = Integer.parseInt(value);
      if (num < 0)
         this.accumulator = String.format("-%04d", Math.abs(num));
      else
         this.accumulator = String.format("%04d", num);
   }

   public int getCounter() {
      return counter;
   }

   public String getRes() {
      return res.toString();
   }
   
   public void execute() {
      String instruction = memory.getItem(counter);
      int intInstr = Integer.parseInt(instruction);
      int opCode = intInstr / 100;    
      int operand = intInstr % 100;

      switch (opCode) {
            case 10: 
               readAdd(operand);
               break;
            case 11: 
               write(operand);
               break;
            case 20:
            case 22:
               load(opCode, operand);
               break;
            case 21:
               store(operand);
               break;
            case 30:
            case 35:
               add(opCode, operand);
               break;
            case 31:
            case 36:
               subt(opCode, operand);
               break;
            case 32:
            case 37:
               div(opCode, operand);
               break;
            case 33:
            case 38:
               mod(opCode, operand);
               break;
            case 34:
            case 39:     
               mult(opCode, operand);
               break;
            case 40:
            case 41:
            case 42:
               jump(opCode, operand);
               break;
            case 43: 
               System.exit(0);
            default:
               System.out.println("Unknown opcode: " + opCode);
      }
      counter++;
   }
   
   public void dumpDirect() {
      while (counter < memory.getMemSize()) {
         execute();
      }
   }

   public void dumpStep() {
      while (counter < memory.getMemSize()) {
         String[] addresses = memory.getAdds();
         int currAcc = Integer.parseInt(getAcc());
         int currCounter = getCounter();
         String currReg = memory.getItem(currCounter);
         // Load registers
         System.out.println();
         System.out.println("REGISTERS:");
         if (currAcc < 0)
            System.out.printf("accumulator:%11s%05d%n", " ", currAcc);
         else 
            System.out.printf("accumulator:%11s+%04d%n", " ", currAcc);
         System.out.printf("programCounter:%11s%02d%n", " ", currCounter);
         System.out.printf("instructionRegister:%3s%5s%n", " ", currReg);
         System.out.printf("operationCode:%12s%02d%n", " ", Integer.parseInt(currReg) / 100);
         System.out.printf("operand:%18s%02d%n%n", " ", Integer.parseInt(currReg) % 100);

         // Load memory addresses   
         System.out.println("MEMORY:");
         System.out.printf("%-6s", " ");
         for (int i = 0; i < 10; i++) {
               System.out.printf("%5d%4s", i, " ");
         }
         System.out.println();

         for (int i = 0; i < 100; i++) {
            if (i % 10 == 0) {
               System.out.printf("%02d", i);
               System.out.printf("%4s", " ");
            }
            if (addresses[i].startsWith("-"))
               System.out.printf("%05d", Integer.parseInt(addresses[i]));
            else
               System.out.print("+" + addresses[i]);
            System.out.printf("%4s", " ");
            if (i % 10 == 9)
               System.out.println();
         }
         execute();
         System.out.println();
         System.out.print("Press Enter to proceed to next step...");
         scan.nextLine();
      }
   }
   
   public void readAdd(int address) {
      System.out.print("Enter value: ");
      String val = scan.next();
      scan.nextLine(); 

      if (val.matches("[+-]?\\d+")) {
         int num = Integer.parseInt(val);

         if (num > 9999 || num < -9999) {
               System.out.println("ERROR: Input exceeds value limits. Terminating Program...");
               System.exit(1);
         }

         String formatNum = String.format("%04d", Math.abs(num));
         if (num < 0) {
               formatNum = "-" + formatNum;
         }
         memory.addItem(address, formatNum);
      } else {
         memory.addItem(address, val);
      }
   }
   
   public void write(int address) {
      String value = memory.getItem(address);

      if (value.matches("[+-]?\\d+")) {
         int num = Integer.parseInt(value);
         System.out.print(Integer.toString(num));
      } else {
         System.out.print("RESULT: " + value);
      }  
   }
   
   public void load(int opCode, int operand) {
      String result = " ";
      if (opCode == 20)
         result = memory.getItem(operand);
      else 
         result = String.format("%04d", operand);
      setAcc(result);
   }

   public void store (int address) {
      String currAcc = getAcc();
      memory.addItem(address, currAcc);
   }

   public void add(int opCode, int operand) {
      int val2, result = 0;
      if (opCode == 30)
         val2 = Integer.parseInt(memory.getItem(operand));
      else 
         val2 = operand;
      result = Integer.parseInt(accumulator) + val2;

      if (result > 9999) {
         System.out.println("ERROR: Result exceeds upper value limit. Terminating Program...");
         System.exit(1);
      }
      setAcc(String.format("%04d", result));   
   }

   public void subt(int opCode, int operand) {
      int val2, result = 0;
      if (opCode == 31)
         val2 = Integer.parseInt(memory.getItem(operand));
      else 
         val2 = operand;
      result = Integer.parseInt(accumulator) - val2;

      if (result < -9999) {
         System.out.println("ERROR: Result exceeds lower value limit. Terminating Program...");
         System.exit(1);
      }
      setAcc(String.format("%04d", result));
   }

   public void div(int opCode, int operand) {
      int val2, result = 0;
      if (opCode == 32)
         val2 = Integer.parseInt(memory.getItem(operand));
      else 
         val2 = operand;

      if (val2 == 0) {
         System.out.println("ERROR: Cannot divide by 0.");
         result = 0;
      } else {
         result = Integer.parseInt(accumulator) / val2;
      }

      if (result > 9999) {
         System.out.println("ERROR: Result exceeds upper value limit. Terminating Program...");
         System.exit(1);
      }
      setAcc(String.format("%04d", result));
   }

   public void mod(int opCode, int operand) {
      int val2, result = 0;
      if (opCode == 33)
         val2 = Integer.parseInt(memory.getItem(operand));
      else 
         val2 = operand;
      result = Integer.parseInt(accumulator) % val2;

      setAcc(String.format("%04d", result));
   }

   public void mult(int opCode, int operand) {
      int val2, result = 0;
      if (opCode == 34)
         val2 = Integer.parseInt(memory.getItem(operand));
      else 
         val2 = operand;
      result = Integer.parseInt(accumulator) * val2;
      if (result > 9999) {
         System.out.println("ERROR: Result exceeds upper value limit. Terminating Program...");
         System.exit(1);
      }
      setAcc(String.format("%04d", result));
   }

   public void jump(int opCode, int operand) {
      int accVal = Integer.parseInt(getAcc());

      if (opCode == 40) { 
         counter = operand - 1;  
      } 
      else if (opCode == 41 && accVal < 0) { 
         counter = operand - 1;
      } 
      else if (opCode == 42 && accVal == 0) {
         counter = operand - 1;
      }
   }
}