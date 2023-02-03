package com.sccs.solve.dto;

public class SolveResult {
    int time;
    String result;
    int memory;

    public SolveResult(int time, String result, int memory) {
        this.time = time;
        this.result = result;
        this.memory = memory;
    }

    public int getTime() {
        return time;
    }

    public String getResult() {
        return result;
    }

    public int getMemory() { return memory;}

    @Override
    public String toString() {
        return "SolveResult{" +
                "time=" + time +
                ", result='" + result + '\'' +
                ", memory=" + memory +
                '}';
    }
}
