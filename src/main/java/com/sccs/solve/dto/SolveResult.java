package com.sccs.solve.dto;

public class SolveResult {
    int time;
    String result;

    public SolveResult(int time, String result) {
        this.time = time;
        this.result = result;
    }

    public int getTime() {
        return time;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "SolveResult{" +
                "time=" + time +
                ", result='" + result + '\'' +
                '}';
    }
}
