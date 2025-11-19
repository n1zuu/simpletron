import java.io.*;
import java.util.*;

public class Compiler {
    private static Map<String, Integer> symbolTable = new HashMap<>();
    private static Map<String, String> variableInitValues = new HashMap<>();
    private static Map<String, Integer> labelTable = new HashMap<>();
    private static List<String> sourceLines = new ArrayList<>();
    private static int nextDataAddress = 0; 
    private static int instructionStartAddress = 0; 
    
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java Compiler <filename> [-d]");
            return;
        }

        String inputFile = args[0];
        boolean directRun = args.length > 1 && args[1].equals("-d");
        boolean stepRun = args.length > 1 && args[1].equals("-s");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sourceLines.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        firstPass();
        
        List<String> machineCode = secondPass();
        
        if (machineCode == null) {
            System.out.println("Compilation failed.");
            return;
        }

        String outputFile = inputFile.replace(".sml", ".mach");
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // Write a header comment
            writer.println("// Variables (addresses 0-" + (instructionStartAddress - 1) + ")");
            
            // Write variable initial values in order
            for (int i = 0; i < instructionStartAddress; i++) {
                // Find which variable has this address
                String value = "0000";
                for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
                    if (entry.getValue() == i) {
                        value = variableInitValues.get(entry.getKey());
                        writer.println(String.format("%04d", Integer.parseInt(value)) + "  // " + entry.getKey());
                        break;
                    }
                }
            }
            
            writer.println("// Instructions (starting at address " + instructionStartAddress + ")");
            // Write instructions
            for (String code : machineCode) {
                writer.println(code);
            }
        } catch (IOException e) {
            System.out.println("Error writing machine code: " + e.getMessage());
            return;
        }

        System.out.println("Compilation successful! Machine code saved to: " + outputFile);
        System.out.println("\nSymbol Table:");
        symbolTable.forEach((var, addr) -> System.out.println("  " + var + " -> " + addr));
        System.out.println("\nLabel Table:");
        labelTable.forEach((label, addr) -> System.out.println("  " + label + " -> " + addr));

        // If -d or -s flag provided, load into memory and run
        if (directRun || stepRun) {
            System.out.println("\nExecuting program...\n");

            Memory mem = new Memory(100);
            
            for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
                String varName = entry.getKey();
                int address = entry.getValue();
                String initValue = variableInitValues.get(varName);
                mem.addItem(address, initValue);
            }
            
            for (int i = 0; i < machineCode.size(); i++) {
                mem.addItem(instructionStartAddress + i, machineCode.get(i));
            }

            Processor cpu = new Processor(mem, instructionStartAddress);
            
            if (stepRun) {
                cpu.dumpStep();
            } else {
                cpu.dumpDirect();
            }
        }
    }

    private static void firstPass() {
        for (String line : sourceLines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("//") || line.endsWith(":"))
                continue;
            if (!isVariableLine(line))
                continue;
            if (line.matches("[a-zA-Z_][a-zA-Z0-9_]*\\s*[=]\\s*-?\\d+")) {
                String[] parts = line.split("\\s*[=]\\s*");
                String varName = parts[0].trim();
                String initValue = parts[1].trim();
                
                if (!symbolTable.containsKey(varName)) {
                    symbolTable.put(varName, nextDataAddress++);
                    variableInitValues.put(varName, initValue);
                }
            }
            else if (line.matches("[a-zA-Z_][a-zA-Z0-9_]*\\s+\\d+")) {
                String[] parts = line.split("\\s+");
                String varName = parts[0].trim();
                String initValue = parts[1].trim();
                
                if (!symbolTable.containsKey(varName)) {
                    symbolTable.put(varName, nextDataAddress++);
                    variableInitValues.put(varName, initValue);
                }
            }
            else if (isVariableDeclaration(line)) {
                String varName = line.trim();
                if (!symbolTable.containsKey(varName)) {
                    symbolTable.put(varName, nextDataAddress++);
                    variableInitValues.put(varName, "0");
                }
            }
        }
        
        instructionStartAddress = nextDataAddress;
        int instructionAddress = instructionStartAddress;
        
        for (String line : sourceLines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("//"))
                continue;
            if (line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                labelTable.put(label, instructionAddress);
            }
            else if (isVariableLine(line)) {
                continue;
            }
            else {
                instructionAddress++;
            }
        }
    }

    private static boolean isVariableDeclaration(String line) {
        line = line.trim();
        if (!line.matches("[a-zA-Z_][a-zA-Z0-9_]*") || line.endsWith(":")) {
            return false;
        }
        
        String upper = line.toUpperCase();
        Set<String> keywords = Set.of(
            "READ", "WRITE", "LOAD", "LOADM", "LOADI", "STORE",
            "ADD", "ADDM", "ADDI", "SUBT", "SUBTM", "SUBTI",
            "DIV", "DIVM", "DIVI", "MOD", "MODM", "MODI",
            "MULT", "MULTM", "MULTI", "JUMP", "JMP", "JUMPN", "JMPN",
            "JUMPZ", "JMPZ", "HALT"
        );
        
        return !keywords.contains(upper);
    }
    
    private static boolean isVariableLine(String line) {
        line = line.trim();
        
        if (line.matches("[a-zA-Z_][a-zA-Z0-9_]*\\s*[=]\\s*-?\\d+") ||
            line.matches("[a-zA-Z_][a-zA-Z0-9_]*\\s+\\d+")) {
            return true;
        }
        
        return isVariableDeclaration(line);
    }

    private static List<String> secondPass() {
        List<String> machineCode = new ArrayList<>();
        
        for (String line : sourceLines) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("//"))
                continue;
            
            if (line.endsWith(":") || isVariableLine(line))
                continue;
            
            String instruction = compileInstruction(line);
            if (instruction == null) {
                return null;
            }
            machineCode.add(instruction);
        }
        
        return machineCode;
    }

    private static String compileInstruction(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length == 0) return null;
        
        String instr = parts[0].toUpperCase();
        int opcode = 0;
        int operand = 0;
        
        // Determine opcode
        switch (instr) {
            case "READ":
                opcode = 10;
                operand = resolveOperand(parts[1]);
                break;
            case "WRITE":
                opcode = 11;
                operand = resolveOperand(parts[1]);
                break;
            case "LOAD":
            case "LOADM":
                opcode = 20;
                operand = resolveOperand(parts[1]);
                break;
            case "LOADI":
                opcode = 22;
                operand = resolveOperand(parts[1]);
                break;
            case "STORE":
                opcode = 21;
                operand = resolveOperand(parts[1]);
                break;
            case "ADD":
            case "ADDM":
                opcode = 30;
                operand = resolveOperand(parts[1]);
                break;
            case "ADDI":
                opcode = 35;
                operand = resolveOperand(parts[1]);
                break;
            case "SUBT":
            case "SUBTM":
                opcode = 31;
                operand = resolveOperand(parts[1]);
                break;
            case "SUBTI":
                opcode = 36;
                operand = resolveOperand(parts[1]);
                break;
            case "DIV":
            case "DIVM":
                opcode = 32;
                operand = resolveOperand(parts[1]);
                break;
            case "DIVI":
                opcode = 37;
                operand = resolveOperand(parts[1]);
                break;
            case "MOD":
            case "MODM":
                opcode = 33;
                operand = resolveOperand(parts[1]);
                break;
            case "MODI":
                opcode = 38;
                operand = resolveOperand(parts[1]);
                break;
            case "MULT":
            case "MULTM":
                opcode = 34;
                operand = resolveOperand(parts[1]);
                break;
            case "MULTI":
                opcode = 39;
                operand = resolveOperand(parts[1]);
                break;
            case "JUMP":
            case "JMP":
                opcode = 40;
                operand = resolveOperand(parts[1]);
                break;
            case "JUMPN":
            case "JMPN":
                opcode = 41;
                operand = resolveOperand(parts[1]);
                break;
            case "JUMPZ":
            case "JMPZ":
                opcode = 42;
                operand = resolveOperand(parts[1]);
                break;
            case "HALT":
                opcode = 43;
                operand = 0;
                break;
            default:
                System.out.println("Unknown instruction: " + instr);
                return null;
        }

        return String.format("%02d%02d", opcode, operand);
    }

    private static int resolveOperand(String operand) {
        if (operand == null) return 0;
        
        operand = operand.trim();
        
        if (operand.matches("\\d+")) {
            return Integer.parseInt(operand);
        }
        
        if (labelTable.containsKey(operand)) {
            return labelTable.get(operand);
        }
        
        if (symbolTable.containsKey(operand)) {
            return symbolTable.get(operand);
        }
        
        System.out.println("Error: Undefined symbol '" + operand + "'");
        return 0;
    }
}