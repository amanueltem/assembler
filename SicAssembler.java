import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SicAssembler {
    private static Map<String, String> OPTAB;
    private static Map<String, String> SYMTAB;
    private static Integer LOCCTR=0;
    private static String sourceFile;
    private static String lenInstruction;

    public static void intializeOPTAB() {
        OPTAB = new HashMap<>();
        OPTAB.put("ADD", "18");
        OPTAB.put("AND", "40");
        OPTAB.put("COMP", "28");
        OPTAB.put("DIV", "24");
        OPTAB.put("J", "3C");
        OPTAB.put("JEQ", "30");
        OPTAB.put("JGT", "34");
        OPTAB.put("JLT", "38");
        OPTAB.put("JSUB", "48");
        OPTAB.put("LDA", "00");
        OPTAB.put("LDCH", "50");
        OPTAB.put("LDL", "08");
        OPTAB.put("LDX", "04");
        OPTAB.put("MUL", "20");
        OPTAB.put("OR", "44");
        OPTAB.put("RD", "D8");
        OPTAB.put("RSUB", "4C");
        OPTAB.put("STA", "0C");
        OPTAB.put("STCH", "54");
        OPTAB.put("STL", "14");
        OPTAB.put("STSW", "E8");
        OPTAB.put("STX", "10");
        OPTAB.put("SUB", "1C");
        OPTAB.put("TD", "E0");
        OPTAB.put("TIX", "2C");
        OPTAB.put("WD", "DC");
    }

    public static void pass1() {
        SYMTAB = new HashMap<>();
        Integer startingAddress = 0;
         System.out.println("\n==========================================================================");
         System.out.printf("%-10s%-20s%-20s%-20s\n","  LOC","Laebel","Opcode","operand");
         System.out.println("===========================================================================\n");
        try {
            File inputFile = new File(sourceFile);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            File outputFile = new File("intermidate.asm");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            String line;

            while ((line = reader.readLine()) != null) {

                if (line.contains(".")) {
                    line = line.strip();

                    if (line.startsWith("."))
                        continue;
                    else {
                        System.out.println("Invalid comment: " + line);
                        System.out.println(" Comments must start with . ");
                        break;

                    }
                }
                // spliting a line into three parts(label,opcode,operand)
                String[] parts = line.split("\\s+");
                if (line.isEmpty() || parts.length == 0) {

                    continue;
                }

                String label = parts[0];
                String opcode = parts.length > 1 ? parts[1] : null;
                String operand = "";
                for (int i = 2; i < parts.length; i++) {
                    if (parts.length == 3) {
                        operand = parts[2];
                        break;
                    }
                    operand +=parts[i]+" ";
                }

                if ("START".equals(opcode)) {
                    startingAddress = Integer.parseInt(operand, 16);
                    if(startingAddress<0|| startingAddress>65535)
                    throw new IllegalStateException("invalid starting address: "+startingAddress);
                    LOCCTR = startingAddress;
                    writer.write(String.format("%-20s%-20s%-20s\n", label,opcode, String.format("%4s",operand).replace(' ', '0')));
                } else if ("END".equals(opcode)) {
                     writer.write(String.format("%-20s%-20s%-20s\n", label,opcode,String.format("%4s",operand).replace(' ', '0')));
                    break;
                } else {
                   System.out.printf("%-10s%-20s%-20s%-20s\n",
                    String.format("%4s", Integer.toHexString(LOCCTR).toUpperCase()).replace(' ', '0'), label, opcode, operand);

                    writer.write(String.format("%-20s%-20s%-20s%-10s\n",
                    label,opcode,operand,  String.format("%4s", Integer.toHexString(LOCCTR).toUpperCase()).replace(' ', '0')));
                    // assign SYMTAB
                    if (!label.isEmpty()) {
                        if (SYMTAB.get(label) == null) {
                            SYMTAB.put(label, Integer.toHexString(LOCCTR).toUpperCase());
                        } else {
                            throw new IllegalStateException("Error dupulicate symbol: " + label);
                        }
                    }
                    // assign OPCODE
                    if (OPTAB.get(opcode) == null && !isDirective(opcode)) {
                        throw new IllegalStateException("invalid operation code " + opcode);
                    } else {
                        if (opcode.equals("WORD")) {
                            if (operand.isEmpty()) {
                                throw new IllegalStateException(opcode + " must have value");
                            }
                            LOCCTR += 3;
                        } else if (opcode.equals("RESW")) {
                            if (operand.isEmpty()) {
                               throw new IllegalStateException(opcode + " must have value");
                            }
                            LOCCTR += 3 * Integer.parseInt(operand);
                        } else if (opcode.equals("RESB")) {
                            if (operand.isEmpty()) {
                                throw new IllegalStateException(opcode + " must have value");
                            }
                            LOCCTR += Integer.parseInt(operand);
                        } else if (opcode.equals("BYTE")) {
                            if (operand.isEmpty()) {
                               throw new IllegalStateException(opcode + " must have value");
                            }
                            int constantLength = calculateConstantLength(operand);
                            if (constantLength == 0) {
                                throw new IllegalStateException("unsupported constant format:" + operand);
                            }
                            LOCCTR += constantLength;
                        } else {
                            LOCCTR += 3;
                        }
                    }
                }

            }

            String lengthOfInstruction = Integer.toHexString(LOCCTR - startingAddress).toUpperCase();
            System.out.println("\ntotal length of instruction:" + lengthOfInstruction);
            writer.write("length=" + lengthOfInstruction + "\n");
            lenInstruction = lengthOfInstruction + "";
            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void pass2() {
        System.out.println("\n======================================================================================================");
        System.out.printf("%-20s%-20s%-20s%-10s\t\t\t%s\n","LABEL","OPCODE","OPERAND","LOC","OBJECT CODE");
        System.out.println("======================================================================================================\n");
        try {
            File inputFile = new File("intermidate.asm");
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            File outputFile = new File("object_program.txt");

            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            String line;
            String textObject = "";
            Integer objectLength = 70;
            String intialAddres = "";
            Integer symbolCounter = 0;
            while ((line = reader.readLine()) != null) {

                // spliting a line into three parts(label,opcode,operand)
                String[] parts = line.split("\\s+");

                if (line.isEmpty()) {
                    continue;
                }

                String label = parts[0];
                String opcode = parts.length > 1 ? parts[1] : null;
                String operand = "";
                String machineCode = "";
                String targetAddress = "";
                String startAddress = "";
                boolean isModifiable=true;

                if (opcode != null && (opcode.equals("START") || opcode.equals("END"))) {
                    operand = parts[parts.length - 1];
                }

                else {
                    for (int i = 2; i < parts.length - 1; i++) {
                        if (parts.length == 3) {
                            operand = parts[2];
                            break;
                        }
                        operand += " " + parts[i];
                    }
                }

                startAddress = parts[parts.length - 1];
                operand = operand.stripLeading();
                if ("START".equals(opcode)) {
                    writer.write("H^" + label + "    ^" + String.format("%6s",operand).replace(' ', '0') + "^" +  String.format("%6s",lenInstruction).replace(' ', '0')+ "\n");

                }
                if ("END".equals(opcode)) {
                    symbolCounter--;
                    String textRecord = "T^" +String.format("%6s",intialAddres).replace(' ', '0')  + "^"
                            + String.format("%2s",Integer.toHexString((textObject.length() - symbolCounter) / 2)
                                    .toUpperCase()).replace(' ','0');
                    textRecord += textObject + "\n";
                    writer.write(textRecord);
                    writer.write("E^" + String.format("%6s",SYMTAB.get(operand)).replace(' ', '0') + "\n");
                } else if (line.startsWith("length")) {
                    break;
                } else {

                    if (opcode == null)
                        continue;
                    else if (opcode.isEmpty()) {
                        machineCode = "";
                        targetAddress = "";
                    } else if (OPTAB.get(opcode) != null) {
                        machineCode = OPTAB.get(opcode);
                        targetAddress = SYMTAB.get(operand);
                        if (operand.contains(",")) {
                            String[] arr = operand.split(",");
                            operand = arr[0];
                            String index = arr[1].strip();
                            if (index.equals("X")) {
                                char[] addresses = SYMTAB.get(operand).toCharArray();
                                String x = addresses[0] + "";
                                int y = Integer.parseInt(x, 16);
                                y = y + 8;
                                targetAddress = Integer.toHexString(y).toUpperCase() + "";
                                for (int i = 1; i < addresses.length; i++) {
                                    targetAddress += addresses[i];
                                   
                                }

                            }
                        }

                        if (operand.isEmpty()) {
                            targetAddress = "0000";
                        }
                    } else if (opcode.equals("WORD")) {
                        machineCode = "";
                        targetAddress = Integer.toHexString(Integer.parseInt(operand)).toUpperCase();
                        targetAddress=String.format("%6s",targetAddress).replace(' ', '0');
                    }
                    else if(opcode.equals("BYTE")){
                        machineCode="";
                        if(operand.startsWith("X'")&&operand.endsWith("'")){
                              isModifiable=false;
                            String[] arr=operand.split("'");
                            targetAddress=arr[1];
                        }
                    }
                    if (targetAddress != null) {
                        if (targetAddress.length() > 4&&!machineCode.isEmpty()) {
                            throw new IllegalStateException("address is out of range please adjust the starting address.");
                        }
                        else{
                        if(!targetAddress.isEmpty()&&isModifiable)
                         targetAddress=String.format("%4s",targetAddress).replace(' ', '0');
                        }
                    }

                    if ((textObject.length() + 6) < objectLength) {
                        if (textObject.length() == 0) {
                            intialAddres = startAddress;
                        }

                        if (machineCode.isEmpty() && targetAddress.isEmpty()) {
                            if (opcode == "RESB" || opcode == "RESW" || opcode == "BYTE")
                                textObject += "^000000";
                        } else {
                            textObject += ("^" + machineCode + targetAddress);
                        }
                        symbolCounter += 1;
                    } else {

                        symbolCounter--;
                        String textRecord = "T^" +String.format("%6s",intialAddres).replace(' ', '0')  + "^"
                            + String.format("%2s",Integer.toHexString((textObject.length() - symbolCounter) / 2)
                                    .toUpperCase()).replace(' ','0');
                        textRecord += textObject + "\n";
                        writer.write(textRecord);
                        textObject = "";
                        symbolCounter = 0;
                    }

                    System.out.println(line + "\t\t\t" + machineCode + targetAddress);

                }
            }
            writer.close();
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
public static void printObjectcode(){
try{
 File inputFile=new File("object_program.txt");
 BufferedReader reader = new BufferedReader(new FileReader(inputFile));
 String line;
 while((line=reader.readLine())!=null){
    System.out.println(line);
 }
 reader.close();
}
catch(IOException e){
    e.printStackTrace();
}
}
    public static void main(String[] args) {
        if (args.length==0){
            sourceFile = "test1.asm";
        } else {
            sourceFile = args[0];
        }
        intializeOPTAB();
        System.out.println("Pass1 algorithm processing............");
        pass1();
      
        System.out.println("\n\n\nSYMBOL TABLE");
        System.out.println("\n==================================================================");
        System.out.printf("%-20s%-20s\n","  Key","Value");
        System.out.println("==================================================================\n");
        for (Map.Entry<String, String> entry : SYMTAB.entrySet()) {
            System.out.printf("%-20s%-20s\n","  "+entry.getKey(),entry.getValue());
        }
        System.out.println("\n\n\n\n pass2 algorithm processing..................");
        pass2();
        System.out.println("\n\n\n\nObject program  result  .....................\n\n");
        printObjectcode();
    }

    private static boolean isDirective(String opString) {
        if (opString.equals("START") || opString.equals("END") ||
                opString.equals("WORD")
                || opString.equals("BYTE") || opString.equals("RESW")
                || opString.equals("RESB")) {
            return true;
        }
        return false;
    }

    private static int calculateConstantLength(String operand) {
        operand = operand.stripLeading();
        operand = operand.stripTrailing();
        if (operand.startsWith("C'") &&
                operand.endsWith("'")) {
            return operand.length() - 3;
        } else if (operand.startsWith("X'") &&
                operand.endsWith("'")) {
            return 1;
        } else {
            // Unsupported constant format
            return 0;
        }
    }
}
