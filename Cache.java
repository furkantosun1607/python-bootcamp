public class Cache {
    private static final int NUM_BLOCKS = 8;
    private static final int BLOCK_SIZE = 2;
    private static final int CACHE_SIZE = NUM_BLOCKS * BLOCK_SIZE;
    
    private byte[] cache;
    private int[] tags;
    private boolean[] valid;
    private int hits;
    private int misses;
    private Memory memory;

    public Cache(Memory memory) {
        this.memory = memory;
        this.cache = new byte[CACHE_SIZE];
        this.tags = new int[NUM_BLOCKS];
        this.valid = new boolean[NUM_BLOCKS];
        this.hits = 0;
        this.misses = 0;
    }

    private int getBlockIndex(int address) {
        return (address / BLOCK_SIZE) % NUM_BLOCKS;
    }

    private int getTag(int address) {
        return address / (NUM_BLOCKS * BLOCK_SIZE);
    }

    private int getOffset(int address) {
        return address % BLOCK_SIZE;
    }

    public byte read(int address) {
        int blockIndex = getBlockIndex(address);
        int tag = getTag(address);
        int offset = getOffset(address);

        if (valid[blockIndex] && tags[blockIndex] == tag) {
            hits++;
            return cache[blockIndex * BLOCK_SIZE + offset];
        }

        // Cache miss
        misses++;
        int blockStart = (address / BLOCK_SIZE) * BLOCK_SIZE;
        for (int i = 0; i < BLOCK_SIZE; i++) {
            cache[blockIndex * BLOCK_SIZE + i] = memory.read(blockStart + i);
        }
        tags[blockIndex] = tag;
        valid[blockIndex] = true;

        return cache[blockIndex * BLOCK_SIZE + offset];
    }

    public void write(int address, byte value) {
        int blockIndex = getBlockIndex(address);
        int tag = getTag(address);
        int offset = getOffset(address);

        // Write-through policy: write to both cache and memory
        memory.write(address, value);

        if (valid[blockIndex] && tags[blockIndex] == tag) {
            hits++;
            cache[blockIndex * BLOCK_SIZE + offset] = value;
        } else {
            misses++;
            // Update cache block
            int blockStart = (address / BLOCK_SIZE) * BLOCK_SIZE;
            for (int i = 0; i < BLOCK_SIZE; i++) {
                cache[blockIndex * BLOCK_SIZE + i] = memory.read(blockStart + i);
            }
            tags[blockIndex] = tag;
            valid[blockIndex] = true;
            cache[blockIndex * BLOCK_SIZE + offset] = value;
        }
    }

    // 16-bit word read (little-endian)
    public short readWord(int address) {
        int low = read(address) & 0xFF;
        int high = read(address + 1) & 0xFF;
        return (short) ((high << 8) | low);
    }

    // 16-bit word write (little-endian)
    public void writeWord(int address, short value) {
        write(address, (byte) (value & 0xFF));
        write(address + 1, (byte) ((value >> 8) & 0xFF));
    }

    public double getHitRatio() {
        int total = hits + misses;
        return total == 0 ? 0 : (double) hits / total * 100;
    }
} 