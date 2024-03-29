package com.sccs.solve.controller;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import com.sccs.solve.service.SolveServiceJava;

import com.sccs.solve.service.SolveServicePython;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sccs.solve.util.ParamCheckUtil;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/solve")
@RequiredArgsConstructor
public class SolveController {
    private final Logger logger = LoggerFactory.getLogger(SolveController.class);
    private final SolveServiceJava solveServiceJava; // RequiredConstructor : final이나 @NonNull인 필드값만 파라미터로 받는 생성자를 만들어준다.
    private final SolveServicePython solveServicePython;
    private final ParamCheckUtil paramCheckUtil;
    private final String SUCCESS = "맞았습니다";
    private final String MESSAGE = "message";

    // EC2 경로
    private final String EC2_ROOT_DIR = File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator;
    // 윈도우 경로
    // private final String EC2_ROOT_DIR = "." + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator;


    @PostMapping("/python/submission")
    public ResponseEntity<?> solveWithPython(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 클라이언트가 넘긴 정보
        HashMap<String, Object> resultMap = new HashMap<>();

        runtime += 2; // '파이썬' 언어는 실행시간 ++2

        if (!paramCheckUtil.isValidFile(mfile)) { // 넘어온 파일 null 체크
            resultMap.put(MESSAGE, "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        if (!paramCheckUtil.isValidParameter(type, no, runtime, memory)) { // 넘어온 파라미터 null 체크
            resultMap.put(MESSAGE, "parameter is not valid");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        File convFile = new File(EC2_ROOT_DIR + "Solution.py"); // 소스코드 실행을 위한 'Solution.py' 파일 생성

        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 넘어온 데이터를 DTO로 변환
        try {
            Path path = Paths.get(EC2_ROOT_DIR + "Solution.py");
            Stream<String> lines = Files.lines(path);
            String content = lines.collect(Collectors.joining(System.lineSeparator())); // 생성한 파일에서 String 형태를 라인 단위로 가져오기
            logger.info("소스코드 : \n {}", content); // 한줄 단위로 소스코드 출력
            solveInfo = new SolveInfo("chan", content, Integer.parseInt(memory), Integer.parseInt(runtime)); // 사용자아이디, 소스코드, 메모리, 실행시간 Dto에 세팅
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
            resultMap.put(MESSAGE, "I/O ERROR");
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR); // 500
        }

        List<HashMap<String, Object>> resultList = new ArrayList<>();

        // 여러개 인풋 파일 돌리는 로직
        String avgRuntime; // 평균 실행 시간
        double sumRuntime = 0; // 실행 시간 합
        int sumMemory = 0; // 실행 메모리 합
        int avgMemory; // 평균 실행 메모리
        boolean isAnswer = true; // 정답 여부 (모든 케이스가 맞아야 true)
        for (int i=1; i<=5; i++) {
            String INTEXT = "in" + i + ".txt";
            String OUTTEXT = "out" + i + ".txt";
            SolveResult solveResult = solveServicePython.solve(solveInfo, type, no, INTEXT, OUTTEXT);

            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);

            HashMap<String, Object> fiveMap = new HashMap<>();
            if (solveResult.getResult().equals(SUCCESS)) {
                fiveMap.put("result", true);
            } else {
                fiveMap.put("result", false);
            }

            fiveMap.put(MESSAGE, solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

            resultList.add(fiveMap);

            if (!solveResult.getResult().equals(SUCCESS)) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }

        resultMap.put("resultList", resultList);

        avgRuntime = String.format("%.2f",sumRuntime / 5.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 5;

        resultMap.put("avgRuntime", avgRuntime);
        resultMap.put("isAnswer", isAnswer);
        resultMap.put("avgMemory", avgMemory);

        return new ResponseEntity<>(
                resultMap
                , HttpStatus.OK);
    }

    @PostMapping("/python/test")
    public ResponseEntity<?> solveTestCaseWithPython(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 클라이언트가 넘긴 정보
        HashMap<String, Object> resultMap = new HashMap<>();

        runtime += 2; // '파이썬' 언어는 실행시간 ++2

        if (!paramCheckUtil.isValidFile(mfile)) { // 넘어온 파일 null 체크
            resultMap.put(MESSAGE, "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        if (!paramCheckUtil.isValidParameter(type, no, runtime, memory)) { // 넘어온 파라미터 null 체크
            resultMap.put(MESSAGE, "parameter is not valid");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        File convFile = new File(EC2_ROOT_DIR + "Solution.py");

        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            Path path = Paths.get(  EC2_ROOT_DIR + "Solution.py");
            Stream<String> lines = Files.lines(path);
            String content = lines.collect(Collectors.joining(System.lineSeparator())); // 생성한 파일에서 String 형태를 라인 단위로 가져오기
            logger.info("소스코드 : \n {}", content); // 한줄 단위로 소스코드 출력
            solveInfo = new SolveInfo("chan", content, Integer.parseInt(memory), Integer.parseInt(runtime)); // 사용자아이디, 소스코드, 메모리, 실행시간 Dto에 세팅
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<HashMap<String, Object>> resultList = new ArrayList<>();

        // 여러개 인풋 파일 돌리는 로직
        String avgRuntime;
        double sumRuntime = 0;
        int sumMemory = 0;
        int avgMemory;
        boolean isAnswer = true;
        for (int i=1; i<=3; i++) {
            SolveResult solveResult = solveServicePython.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");

            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);

            HashMap<String, Object> fiveMap = new HashMap<>();
            if (solveResult.getResult().equals(SUCCESS)) {
                fiveMap.put("result", true);
            } else {
                fiveMap.put("result", false);
            }
            fiveMap.put(MESSAGE, solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

            resultList.add(fiveMap);

            if (!solveResult.getResult().equals(SUCCESS)) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }

        resultMap.put("resultList", resultList);

        avgRuntime = String.format("%.2f",sumRuntime / 3.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 3;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);

        resultMap.put("avgRuntime", avgRuntime);
        resultMap.put("isAnswer", isAnswer);
        resultMap.put("avgMemory", avgMemory);

        return new ResponseEntity<>(
                resultMap
                , HttpStatus.OK);
    }


    @PostMapping("/java/submission") // 5개 테스트 케이스 채점
    public ResponseEntity<?> solveWithJava(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 사용자가 제출한 정보
        HashMap<String, Object> resultMap = new HashMap<>(); // 결과값 저장 자료구조

        if (!paramCheckUtil.isValidFile(mfile)) { // 넘어온 파일 null 체크
            resultMap.put(MESSAGE, "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        if (!paramCheckUtil.isValidParameter(type, no, runtime, memory)) { // 넘어온 파라미터 null 체크
            resultMap.put(MESSAGE, "parameter is not valid");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        File convFile = new File(EC2_ROOT_DIR + "Solution.java"); // EC2 폴더 경로

        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            Path path = Paths.get(EC2_ROOT_DIR + "Solution.java");
            Stream<String> lines = Files.lines(path);
            String content = lines.collect(Collectors.joining(System.lineSeparator())); // 생성한 파일에서 String 형태를 라인 단위로 가져오기
            logger.info("소스코드 : \n {}", content); // 한줄 단위로 소스코드 출력
            convFile.delete();
            solveInfo = new SolveInfo("chan", content, Integer.parseInt(memory), Integer.parseInt(runtime)); // 사용자아이디, 소스코드, 메모리, 실행시간 Dto에 세팅
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
            convFile.delete();
            resultMap.put(MESSAGE, "I/O ERROR");
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR); // 500
        }

        List<HashMap<String, Object>> resultList = new ArrayList<>();

        // 여러개 인풋 파일 돌리는 로직
        String avgRuntime;
        double sumRuntime = 0;
        int sumMemory = 0;
        int avgMemory;
        boolean isAnswer = true;
        for (int i=1; i<=5; i++) {
            SolveResult solveResult = solveServiceJava.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");
            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);

            HashMap<String, Object> fiveMap = new HashMap<>();
            if (solveResult.getResult().equals(SUCCESS)) {
                fiveMap.put("result", true);
            } else {
                fiveMap.put("result", false);
            }
            fiveMap.put(MESSAGE, solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

            resultList.add(fiveMap);

            if (!solveResult.getResult().equals(SUCCESS)) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }

        resultMap.put("resultList", resultList);

        avgRuntime = String.format("%.2f",sumRuntime / 5.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 5;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);

        resultMap.put("avgRuntime", avgRuntime);
        resultMap.put("isAnswer", isAnswer);
        resultMap.put("avgMemory", avgMemory);


        return new ResponseEntity<>(
                resultMap
                , HttpStatus.OK);
    }


    @PostMapping("/java/test") // 3개 테스트 케이스 채점
    public ResponseEntity<?> solveTestCaseWithJava(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 사용자가 제출한 정보
        HashMap<String, Object> resultMap = new HashMap<>(); // 결과값 저장 자료구조

        logger.debug("Java Submission Controller Start");

        if (!paramCheckUtil.isValidFile(mfile)) { // 넘어온 파일 null 체크
            resultMap.put(MESSAGE, "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        if (!paramCheckUtil.isValidParameter(type, no, runtime, memory)) { // 넘어온 파라미터 null 체크
            resultMap.put(MESSAGE, "parameter is not valid");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        File convFile = new File(EC2_ROOT_DIR + "Solution.java"); // EC2 폴더 경로
        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            Path path = Paths.get(EC2_ROOT_DIR + "Solution.java");
            Stream<String> lines = Files.lines(path);

            String content = lines.collect(Collectors.joining(System.lineSeparator())); // 생성한 파일에서 String 형태를 라인 단위로 가져오기
            logger.info("소스코드 : \n {}", content); // 한줄 단위로 소스코드 출력
            solveInfo = new SolveInfo("chan", content, Integer.parseInt(memory), Integer.parseInt(runtime)); // 사용자아이디, 소스코드, 메모리, 실행시간 Dto에 세팅
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<HashMap<String, Object>> resultList = new ArrayList<>();

        // 여러개 인풋 파일 돌리는 로직
        String avgRuntime;
        double sumRuntime = 0;
        int sumMemory = 0;
        int avgMemory;
        boolean isAnswer = true;
        for (int i=1; i<=3; i++) {
            SolveResult solveResult = solveServiceJava.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");

            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);

            HashMap<String, Object> fiveMap = new HashMap<>();
            if (solveResult.getResult().equals(SUCCESS)) {
                fiveMap.put("result", true);
            } else {
                fiveMap.put("result", false);
            }
            fiveMap.put(MESSAGE, solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리

            if (!solveResult.getResult().equals(SUCCESS)) {
                isAnswer = false;
            }

            resultList.add(fiveMap);

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }

        resultMap.put("resultList", resultList);

        avgRuntime = String.format("%.2f",sumRuntime / 3.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 3;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);

        resultMap.put("avgRuntime", avgRuntime);
        resultMap.put("isAnswer", isAnswer);
        resultMap.put("avgMemory", avgMemory);

        return new ResponseEntity<>(
                resultMap
                , HttpStatus.OK);
    }

}
