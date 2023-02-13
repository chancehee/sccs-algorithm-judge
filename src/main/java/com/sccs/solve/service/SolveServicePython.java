package com.sccs.solve.service;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger = LoggerFactory.getLogger(SolveServicePython.class);
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

        // 컴파일
        ProcessBuilder pb = new ProcessBuilder("python3", "-m", "py_compile",  SOLUTIONFILEROOTDIR + "Solution.py");
        Process process = pb.start();
        process.waitFor(); // 현재 실행한 프로세스가 종료될 때까지 블록 처리

        // 컴파일 체크
        if (process.exitValue() != 0) {
            deleteUserCode();
            return new SolveResult(0, "컴파일 에러", 0);
        }

        pb = new ProcessBuilder("python3", SOLUTIONFILEROOTDIR + "Solution.py", "Solution");
        pb.redirectInput(new File(INPUTFILEROOTDIR + type + File.separator + no + File.separator + "input" + File.separator + INTEXT));

        long startTime = System.nanoTime();

        process = pb.start();

        long finishMemory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);

        // 만약 문제에서 설정된 시간제한을 초과한다면 이를 체크하여 코드를 강제로 종료합니다.
        boolean finished = process.waitFor(solveInfo.getTimeLimit(), TimeUnit.SECONDS);

        long endTime = System.nanoTime();
        long elapsedTime = endTime - startTime;

        // 시간 초과 체크
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

        // 실행 결과 값
        StringBuilder output = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 실제 정답
        StringBuilder expectedOutput = new StringBuilder();
        try (Scanner sc = new Scanner(new File(OUTPUTFILEROOTDIR + type + File.separator + no + File.separator + "output" + File.separator + OUTTEXT))) {
            while (sc.hasNextLine()) {
                expectedOutput.append(sc.nextLine()).append("\n");
            }
        }

        // 실행 결과와 실제 정답 비교
        if (output.toString().equals(expectedOutput.toString())) {
            deleteUserCode();
            logger.debug("=================================================================");
            logger.debug("=========================SUCCESS=================================");
            logger.debug("judge result : {}", output);
            logger.debug("expected result : {}", expectedOutput);
            logger.debug("==========================================================");
            return new SolveResult((int) (elapsedTime / (long) 1000000), "맞았습니다", (int) finishMemory);
        }
        else {
            deleteUserCode();
            logger.debug("==============================================================");
            logger.debug("=========================FAIL=================================");
            logger.debug("==============================================================");
            logger.debug("judge result : {}", output);
            logger.debug("expected result : {}", expectedOutput);
            logger.debug("==========================================================");
            return new SolveResult((int) (elapsedTime / (long) 1000000), "틀렸습니다", (int) finishMemory);
        }
    }

}
