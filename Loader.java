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
    
    // Load from List<String> (for use with Compiler output)
    public static void loadProgramFromStrings(Memory memory, List<String> program) {
        int i = 0;
        for (String instruction : program) {
            memory.addItem(i++, instruction);
        }
    }
    
    // Load from machine code file (handles comments and gets start address)
    public static int loadProgramFromFile(Memory memory, String filename) {
        int startAddress = 0;
        boolean foundInstructions = false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int address = 0;
            
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                
                // Skip empty lines
                if (trimmed.isEmpty())
                    continue;
                
                // Check for instruction start marker
                if (trimmed.startsWith("// Instructions")) {
                    foundInstructions = true;
                    startAddress = address;
                    continue;
                }
                
                // Skip comment lines
                if (trimmed.startsWith("//"))
                    continue;
                
                // Remove inline comments and extract instruction
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