package com.sccs.solve.service;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MBeanServerConnection;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;
// "." + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file"
@Service
public class SolveService {
//    private final String SOLUTIONFILEROOTDIR = ".\\src\\main\\resources\\usercode\\";
//    private final String INPUTFILEROOTDIR = ".\\src\\main\\resources\\"; // 유형 / 문제 번호 / intput /
//    private final String OUTPUTFILEROOTDIR = ".\\src\\main\\resources\\"; // 유형 / 문제 번호 / output /
    private final String SOLUTIONFILEROOTDIR = "." + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "usercode" + File.separator;
    private final String INPUTFILEROOTDIR = "." + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator;
    private final String OUTPUTFILEROOTDIR = "." + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator;

    public SolveResult solve(SolveInfo solveInfo, String type, String no) throws IOException, InterruptedException{
        if (checkSystemCallInCode(solveInfo.getCode())) {
            System.out.println("시스템 콜 함수 사용");
            return new SolveResult(0, "시스템 콜 함수 사용", 0);
        }
        System.out.println("codeExecutor 실행 !");
        return codeExecutor(solveInfo, type, no);
    }

    public boolean checkSystemCallInCode(String code) {
        Pattern pattern = Pattern.compile("(?<!\\w)Runtime\\.getRuntime\\(\\)\\.exec\\(\"[^\"]+\"\\)");
        Matcher matcher = pattern.matcher(code);

        // 만약 코드에서 SystemCall 패턴을 확인하면 True를 반환합니다.
        return (matcher.find());
    }

    public void deleteUserCode() {
        File file = new File(SOLUTIONFILEROOTDIR + "Solution.java");
        if (file != null)
            file.delete();
        file = new File(SOLUTIONFILEROOTDIR + "Solution.class");
        if (file != null)
            file.delete();
    }
    public SolveResult codeExecutor(SolveInfo solveInfo, String type, String no) throws IOException, InterruptedException{
        // Solution.java 파일을 생성하고 받아온 코드를 파일에 적습니다.
        File file = new File(SOLUTIONFILEROOTDIR  + "Solution.java");
        System.out.println("파일이름 : " + file.getName());
        FileWriter writer = new FileWriter(file);
        writer.write(solveInfo.getCode());
        writer.close();

        // 사용자가 제출한 Solution.java를 컴파일합니다.
        // Shell Script : javac Solution.java
        ProcessBuilder pb = new ProcessBuilder("javac", SOLUTIONFILEROOTDIR + "Solution.java");
        Process process = pb.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            deleteUserCode();
            return new SolveResult(0, "컴파일 에러", 0);
        }

        // 코드의 수행 시간을 계산합니다.
        // Shell Script : java -Xmx128 Solution < input.txt
        // java = 명령어 (Executable file) <<<<<<<<<<<<<
        // -Xmx128m Solution = java라는 명령어의 매개변수 < X
        // redirectInput을 이용하면 Solution에 redirection을 전달할 수 있습니다.
        // input.txt = 리다이렉션의 매개변수
        pb = new ProcessBuilder("java","-Xmx" + solveInfo.getMemorySize() + "m", "-cp",SOLUTIONFILEROOTDIR, "Solution");
        pb.redirectInput(new File(INPUTFILEROOTDIR + type + "\\" + no + "\\input\\" + "in1.txt"));


        long startTime = System.nanoTime();

        process = pb.start();

        long finishMemory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000 / 1000 /2);

        // 만약 문제에서 설정된 시간제한을 초과한다면 이를 체크하여 코드를 강제로 종료합니다.
        boolean finished = process.waitFor(solveInfo.getTimeLimit(), TimeUnit.SECONDS);
        System.out.println("실행 메모리 : " + finishMemory);

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;

        if (!finished) {
            process.destroyForcibly();
            deleteUserCode();
            return new SolveResult((int)(elapsedTime / (long)1000000), "시간 초과", (int) finishMemory);
        }

        // 메모리 초과 체크
        int exitValue = process.exitValue();
        if (exitValue != 0) {
            deleteUserCode();
            return new SolveResult((int)(elapsedTime / (long)1000000), "런타임 에러", (int) finishMemory);
        }

        // 비교를 위해 코드의 출력을 StringBuilder로 합칩니다.
        StringBuilder output = new StringBuilder();
        try (Scanner sc = new Scanner(process.getInputStream())) {
            while (sc.hasNextLine()) {
                output.append(sc.nextLine()).append("\n");
            }
        }

        // 실제 정답도 동일한 과정을 거칩니다.
        StringBuilder expectedOutput = new StringBuilder();
        try (Scanner sc = new Scanner(new File(OUTPUTFILEROOTDIR + type + "\\" + no + "\\output\\" + "out1.txt"))) {
            while (sc.hasNextLine()) {
                expectedOutput.append(sc.nextLine()).append("\n");
            }
        }

        if (output.toString().equals(expectedOutput.toString())) {
            deleteUserCode();
            return new SolveResult((int) (elapsedTime / (long) 1000000), "맞았습니다", (int) finishMemory);
        }
        else {
            deleteUserCode();
            return new SolveResult((int) (elapsedTime / (long) 1000000), "틀렸습니다", (int) finishMemory);
        }
    }

}
