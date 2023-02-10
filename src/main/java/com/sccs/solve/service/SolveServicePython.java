package com.sccs.solve.service;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
//import org.python.jline.internal.InputStreamReader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SolveServicePython {
    // 윈도우
//    private final String SOLUTIONFILEROOTDIR = ".\\src\\main\\resources\\usercode\\";
//    private final String INPUTFILEROOTDIR = ".\\src\\main\\resources\\"; // 유형 / 문제 번호 / intput /
//    private final String OUTPUTFILEROOTDIR = ".\\src\\main\\resources\\"; // 유형 / 문제 번호 / output /
    private final String SOLUTIONFILEROOTDIR = File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator;
    private final String INPUTFILEROOTDIR = File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge"+ File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator;
    private final String OUTPUTFILEROOTDIR = File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge"+ File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator;

    public SolveResult solve(SolveInfo solveInfo, String type, String no, String INTEXT, String OUTTEXT) throws IOException, InterruptedException{
        if (checkSystemCallInCode(solveInfo.getCode())) {
            System.out.println("시스템 콜 함수 사용");
            return new SolveResult(0, "시스템 콜 함수 사용", 0);
        }
        System.out.println("codeExecutor 실행 !");
        return codeExecutor(solveInfo, type, no,INTEXT, OUTTEXT);
    }

    public boolean checkSystemCallInCode(String code) {
        Pattern pattern = Pattern.compile("(?<!\\w)Runtime\\.getRuntime\\(\\)\\.exec\\(\"[^\"]+\"\\)");
        Matcher matcher = pattern.matcher(code);

        // 만약 코드에서 SystemCall 패턴을 확인하면 True를 반환합니다.
        return (matcher.find());
    }

    public void deleteUserCode() {
        File file = new File(SOLUTIONFILEROOTDIR + "Solution.py");
        if (file != null)
            file.delete();
        file = new File(SOLUTIONFILEROOTDIR + "Solution.class");
        if (file != null)
            file.delete();
    }

    public SolveResult codeExecutor(SolveInfo solveInfo, String type, String no, String INTEXT, String OUTTEXT) throws IOException, InterruptedException{
        // Solution.java 파일을 생성하고 받아온 코드를 파일에 적습니다.
        File file = new File(SOLUTIONFILEROOTDIR  + "Solution.py");
        FileWriter writer = new FileWriter(file);
        writer.write(solveInfo.getCode());
        writer.close();

        //ProcessBuilder pb;
        Process process;

        ProcessBuilder pb = new ProcessBuilder("python3", "-u", "-W", "ignore::ResourceWarning", "-c", "import resource; resource.setrlimit(resource.RLIMIT_AS, ("+ solveInfo.getMemorySize() +" * 1024 * 1024, -1))", SOLUTIONFILEROOTDIR + "Solution.py");
        //ProcessBuilder pb = new ProcessBuilder("python", SOLUTIONFILEROOTDIR + "Solution.py");
        //Process process = pb.start();
        //process.waitFor();

//        if (process.exitValue() != 0) {
//            deleteUserCode();
//            return new SolveResult(0, "컴파일 에러", 0);
//        }

        //pb = new ProcessBuilder("python","-Xmx" + solveInfo.getMemorySize() + "m",SOLUTIONFILEROOTDIR, "main");
        //pb = new ProcessBuilder("python", SOLUTIONFILEROOTDIR + "Solution.py", "main");
        //pb = new ProcessBuilder("python", SOLUTIONFILEROOTDIR + "Solution.py");
        pb.redirectInput(new File(INPUTFILEROOTDIR + type + File.separator + no + File.separator + "input" + File.separator + INTEXT));

        long startTime = System.nanoTime();

        process = pb.start();
        process.waitFor(); // 현재 실행한 프로세스가 종료될 때까지 블록 처리

        long finishMemory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 /2);

        // 만약 문제에서 설정된 시간제한을 초과한다면 이를 체크하여 코드를 강제로 종료합니다.
        boolean finished = process.waitFor(solveInfo.getTimeLimit(), TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;

//        if (!finished) {
//            process.destroyForcibly();
//            deleteUserCode();
//            return new SolveResult((int)(elapsedTime / (long)1000000), "시간 초과", (int) finishMemory);
//        }
//
//        // 메모리 초과 체크
//        int exitValue = process.exitValue();
//        if (exitValue != 0) {
//            deleteUserCode();
//            return new SolveResult((int)(elapsedTime / (long)1000000), "런타임 에러", (int) finishMemory);
//        }

        // 비교를 위해 코드의 출력을 StringBuilder로 합칩니다.
//        StringBuilder output = new StringBuilder();
//        try (Scanner sc = new Scanner(process.getInputStream())) { // 인풋스트림이 비어있다.. (파이썬 프로세스에 인풋을 넘겨주는 로직을 건들여야 할듯)
//            System.out.println("채점 시작 !!!!");
//            while (sc.hasNextLine()) {
//                output.append(sc.nextLine()).append("\n");
//            }
//        }

        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 실제 정답도 동일한 과정을 거칩니다.
        StringBuilder expectedOutput = new StringBuilder();
        //try (Scanner sc = new Scanner(new File(OUTPUTFILEROOTDIR + type + "\\" + no + "\\output\\" + "out1.txt"))) {
        try (Scanner sc = new Scanner(new File(OUTPUTFILEROOTDIR + type + File.separator + no + File.separator + "output" + File.separator + OUTTEXT))) {
            while (sc.hasNextLine()) {
                //System.out.println("무언가 더하는 중 ");
                expectedOutput.append(sc.nextLine()).append("\n");
            }
        }

        if (output.toString().equals(expectedOutput.toString())) {
            deleteUserCode();
            System.out.println(output);
            System.out.println(expectedOutput);
            System.out.println();
            return new SolveResult((int) (elapsedTime / (long) 1000000), "맞았습니다", (int) finishMemory);
        }
        else {
            deleteUserCode();
            return new SolveResult((int) (elapsedTime / (long) 1000000), "틀렸습니다", (int) finishMemory);
        }
    }

}
