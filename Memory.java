public class Memory {
    private byte[] memory;
    private static final int MEMORY_SIZE = 65536; // 64KB

    public Memory() {
        memory = new byte[MEMORY_SIZE];
    }

    public byte read(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Memory address out of bounds: " + address);
        }
        return memory[address];
    }

    public void write(int address, byte value) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Memory address out of bounds: " + address);
        }
        memory[address] = value;
    }

    public void writeWord(int address, short value) {
        // Little-endian byte ordering
        write(address, (byte) (value & 0xFF));
        write(address + 1, (byte) ((value >> 8) & 0xFF));
    }

    public short readWord(int address) {
        // Little-endian byte ordering
        return (short) ((read(address + 1) << 8) | (read(address) & 0xFF));
    }
} 