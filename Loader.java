import java.util.*;
import java.io.*;

public class Loader {

    // Load from List<Integer>
    public static void loadProgram(Memory memory, List<Integer> program) {
        int i = 0;
        for (int instruction : program) {
            String formatted = String.format("%04d", instruction);
            memory.addItem(i++, formatted);
        }
    }
    
    // Load from List<String>
    public static void loadProgramFromStrings(Memory memory, List<String> program) {
        int i = 0;
        for (String instruction : program) {
            memory.addItem(i++, instruction);
        }
    }
    
    // Load from machine code file
    public static int loadProgramFromFile(Memory memory, String filename) {
        int startAddress = 0;
        boolean foundInstructions = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int address = 0;
            
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                
                if (trimmed.isEmpty())
                    continue;
                
                if (trimmed.startsWith("// Instructions")) {
                    foundInstructions = true;
                    startAddress = address;
                    continue;
                }
                
                if (trimmed.startsWith("//"))
                    continue;
                
                String instruction = trimmed.split("//")[0].trim();
                
                if (!instruction.isEmpty()) {
                    memory.addItem(address++, instruction);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading program: " + e.getMessage());
        }
        
        return startAddress;
    }
}