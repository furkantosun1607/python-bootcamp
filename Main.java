import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Main program.txt config.txt");
            return;
        }

        try {
            // Read config file
            BufferedReader configReader = new BufferedReader(new FileReader(args[1]));
            String loadAddrStr = configReader.readLine().trim();
            String initialPCStr = configReader.readLine().trim();
            configReader.close();

            // Remove "0x" prefix if present and parse hex values
            int loadAddress = Integer.parseInt(loadAddrStr.replace("0x", ""), 16);
            int initialPC = Integer.parseInt(initialPCStr.replace("0x", ""), 16);

            // Read program file
            BufferedReader programReader = new BufferedReader(new FileReader(args[0]));
            List<String> instructions = new ArrayList<>();
            String line;
            while ((line = programReader.readLine()) != null) {
                instructions.add(line.trim());
            }
            programReader.close();

            // Create and run emulator
            CPUEmulator emulator = new CPUEmulator(loadAddress, initialPC);
            emulator.loadProgram(instructions.toArray(new String[0]));
            emulator.execute();

            // Print cache statistics
            System.out.printf("Cache hit ratio: %.2f%%\n", emulator.getCacheHitRatio());

        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numbers: " + e.getMessage());
        }
    }
} 