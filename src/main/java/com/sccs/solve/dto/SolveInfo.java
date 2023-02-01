package com.sccs.solve.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class SolveInfo {
    private final String user;
    private final String code;
    private final int memorySize;
    private final int timeLimit;

    public SolveInfo(String user, String code, int memorySize, int timeLimit) {
        this.user = user;
        this.code = code;
        this.memorySize = memorySize;
        this.timeLimit = timeLimit;
    }

    public String getUser() {
        return user;
    }

    public String getCode() {
        return code;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public int getTimeLimit() {
        return timeLimit;
    }
}
