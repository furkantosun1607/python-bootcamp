public class CPUEmulator {
    private Memory memory;
    private Cache cache;
    private int pc;  // Program Counter
    private short ac;  // Accumulator
    private boolean flag;  // Comparison flag
    private int loadAddress;

    public CPUEmulator(int loadAddress, int initialPC) {
        this.memory = new Memory();
        this.cache = new Cache(memory);
        this.loadAddress = loadAddress;
        this.pc = initialPC;
        this.ac = 0;
        this.flag = false;
    }

    public void loadProgram(String[] instructions) {
        for (int i = 0; i < instructions.length; i++) {
            int instruction = Integer.parseUnsignedInt(instructions[i], 2);
            short instructionShort = (short) (instruction & 0xFFFF);
            memory.writeWord(loadAddress + i * 2, instructionShort);
        }
    }

    public void execute() {
        while (true) {
            short instruction = memory.readWord(pc);
            int opcode = (instruction >> 12) & 0xF;
            int operand = instruction & 0xFFF;

            switch (opcode) {
                case 0x0: // START
                    pc += 2;
                    break;
                case 0x1: // LOAD
                    ac = (short) operand;
                    pc += 2;
                    break;
                case 0x2: // LOADM
                    ac = cache.readWord(loadAddress + operand * 2);
                    pc += 2;
                    break;
                case 0x3: // STORE
                    cache.writeWord(loadAddress + operand * 2, ac);
                    pc += 2;
                    break;
                case 0x4: // CMPM
                    short memValue = cache.readWord(loadAddress + operand * 2);
                    flag = ac > memValue;
                    pc += 2;
                    break;
                case 0x5: // CJMP (program offset addressing)
                    if (flag) {
                        pc = loadAddress + operand * 2;
                    } else {
                        pc += 2;
                    }
                    break;
                case 0x6: // JMP (program offset addressing)
                    pc = loadAddress + operand * 2;
                    break;
                case 0x7: // ADD
                    ac += operand;
                    pc += 2;
                    break;
                case 0x8: // ADDM
                    ac += cache.readWord(loadAddress + operand * 2);
                    pc += 2;
                    break;
                case 0x9: // SUB
                    ac -= operand;
                    pc += 2;
                    break;
                case 0xA: // SUBM
                    ac -= cache.readWord(loadAddress + operand * 2);
                    pc += 2;
                    break;
                case 0xB: // MUL
                    ac *= operand;
                    pc += 2;
                    break;
                case 0xC: // MULM
                    ac *= cache.readWord(loadAddress + operand * 2);
                    pc += 2;
                    break;
                case 0xD: // DISP
                    System.out.println("Value in AC: " + ac);
                    pc += 2;
                    break;
                case 0xE: // HALT
                    return;
                default:
                    throw new IllegalStateException("Invalid opcode: " + opcode);
            }
        }
    }

    public double getCacheHitRatio() {
        return cache.getHitRatio();
    }
} 