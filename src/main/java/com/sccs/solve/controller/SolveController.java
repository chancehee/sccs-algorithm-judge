package com.sccs.solve.controller;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import com.sccs.solve.service.SolveServiceJava;

import com.sccs.solve.service.SolveServiceMacPython;
import com.sccs.solve.service.SolveServicePython;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/solve")
@RequiredArgsConstructor
public class SolveController {
    private final Logger logger = LoggerFactory.getLogger(SolveController.class);
    private final SolveServiceJava solveServiceJava; // RequiredConstructor : final이나 @NonNull인 필드값만 파라미터로 받는 생성자를 만들어준다.
    private final SolveServicePython solveServicePython;
    private final SolveServiceMacPython solveServiceMacPython;
    @PostMapping("/python/submission")
    public ResponseEntity<?> solveWithPython(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 클라이언트가 넘긴 정보

        runtime += 2;
        try {
            logger.info(mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            System.out.println("file is null [mintChoco Python]"); // 클라이언트에게 넘어온 파일이 null인 경우
        }

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        File convFile = new File(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.py"); // 리눅스 서버 절대 경로

        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            Path path = Paths.get(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.py");
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
        for (int i=1; i<=5; i++) {
            SolveResult solveResult = solveServicePython.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");

            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);

            HashMap<String, Object> fiveMap = new HashMap<>();
            fiveMap.put("result", solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

            resultList.add(fiveMap);

            if (!solveResult.getResult().equals("맞았습니다")) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }
        avgRuntime = String.format("%.2f",sumRuntime / 5.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 5;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("avgRuntime", avgRuntime);
        tempMap.put("isAnswer", isAnswer);
        tempMap.put("avgMemory", avgMemory);
        resultList.add(tempMap);

        return new ResponseEntity<>(
                resultList
                , HttpStatus.OK);
    }

    @PostMapping("/python/test")
    public ResponseEntity<?> solveTestCaseWithPython(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        HashMap<String, Object> resultMap = new HashMap<>(); // 결과를 담는 자료구조
        SolveInfo solveInfo = null; // 클라이언트가 넘긴 정보

        runtime += 2;
        try {
            logger.info(mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            System.out.println("file is null [mintChoco Python]"); // 클라이언트에게 넘어온 파일이 null인 경우
        }

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        // mfile to file (변환)
        // 윈도우
        //File convFile = new File(".\\src\\main\\resources\\file\\Solution.py");
        // 리눅스
        File convFile = new File(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.py"); // 리눅스 서버 절대 경로

        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            // 윈도우
            //Path path = Paths.get(".\\src\\main\\resources\\file\\Solution.py");
            // 리눅스
            Path path = Paths.get(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.py");
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
            fiveMap.put("result", solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

//            resultMap.put("data"+i, fiveMap); // 5개의 소스코드 채점 결과를 data1, data2, data3 .. 이런식으로 resulMap에 저장
            resultList.add(fiveMap);

            if (!solveResult.getResult().equals("맞았습니다")) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }
        avgRuntime = String.format("%.2f",sumRuntime / 3.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 3;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("avgRuntime", avgRuntime);
        tempMap.put("isAnswer", isAnswer);
        tempMap.put("avgMemory", avgMemory);
        //resultList.add(tempMap);

        return new ResponseEntity<>(
                resultList
                , HttpStatus.OK);
    }


    @PostMapping("/java/submission") // 5개 테스트 케이스 채점
    public ResponseEntity<?> solveWithJava(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 사용자가 제출한 정보
        HashMap<String, Object> resultMap = new HashMap<>(); // 결과값 저장 자료구조

        logger.debug("Java Submission Controller Start");
        try {
            logger.debug("file name is {}", mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            logger.debug("file is null"); // 클라이언트에게 넘어온 파일이 null인 경우
            resultMap.put("message", "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        File convFile = new File(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.java"); // 리눅스 서버 절대 경로
        //File convFile = new File(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "usercode" + File.separator + "Solution.java"); // 리눅스 서버 절대 경로
        logger.info("리눅스 서버 파일 존재 위치 절대 경로 : {}", convFile.getPath());
        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            Path path = Paths.get(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.java");
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
        for (int i=1; i<=5; i++) {
            SolveResult solveResult = solveServiceJava.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");
            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);

            HashMap<String, Object> fiveMap = new HashMap<>();
            fiveMap.put("result", solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

            resultList.add(fiveMap);

            if (!solveResult.getResult().equals("맞았습니다")) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }
        avgRuntime = String.format("%.2f",sumRuntime / 5.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 5;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("avgRuntime", avgRuntime);
        tempMap.put("isAnswer", isAnswer);
        tempMap.put("avgMemory", avgMemory);
        resultList.add(tempMap);

        return new ResponseEntity<>(
                //resultMap
                resultList
                , HttpStatus.OK);
    }


    @PostMapping("/java/test") // 3개 테스트 케이스 채점
    public ResponseEntity<?> solveTestCaseWithJava(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 사용자가 제출한 정보
        HashMap<String, Object> resultMap = new HashMap<>(); // 결과값 저장 자료구조

        logger.debug("Java Submission Controller Start");
        try {
            logger.debug("file name is {}", mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            logger.debug("file is null"); // 클라이언트에게 넘어온 파일이 null인 경우
            resultMap.put("message", "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND); // 404
        }

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        // 리눅스
        File convFile = new File(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.java"); // 리눅스 서버 절대 경로
        logger.info("리눅스 서버 파일 존재 위치 절대 경로 : {}", convFile.getPath());
        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            // 윈도우
            //Path path = Paths.get(".\\src\\main\\resources\\file\\Solution.java");
            // 리눅스
            Path path = Paths.get(File.separator + "home" + File.separator + "project" + File.separator + "judgeonline" + File.separator + "sccs-online-judge" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.java");
            Stream<String> lines = Files.lines(path);

            String content = lines.collect(Collectors.joining(System.lineSeparator())); // 생성한 파일에서 String 형태를 라인 단위로 가져오기
            logger.info("소스코드 : \n {}", content); // 한줄 단위로 소스코드 출력
            solveInfo = new SolveInfo("chan", content, Integer.parseInt(memory), Integer.parseInt(runtime)); // 사용자아이디, 소스코드, 메모리, 실행시간 Dto에 세팅
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // SolveResult solveResult = solveServiceJava.solve(solveInfo, type, no); // 사용자 소스코드 Dto 정보로 문제풀이 -> 결과 받아오기

//        resultMap.put("result", solveResult.getResult()); // 채점 결과
//        resultMap.put("runtime", solveResult.getTime());  // 실행 시간
//        resultMap.put("memory", solveResult.getMemory()); // 메모리

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
            fiveMap.put("result", solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리

            //resultMap.put("data"+i, fiveMap); // 5개의 소스코드 채점 결과를 data1, data2, data3 .. 이런식으로 resulMap에 저장

            resultList.add(fiveMap);

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }

        avgRuntime = String.format("%.2f",sumRuntime / 3.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 3;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("avgRuntime", avgRuntime);
        tempMap.put("isAnswer", isAnswer);
        tempMap.put("avgMemory", avgMemory);
        //resultList.add(tempMap);

        return new ResponseEntity<>(
                resultList
                , HttpStatus.OK);
    }


    @PostMapping("/postPython")
    public ResponseEntity<?> postPython(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        HashMap<String, Object> resultMap = new HashMap<>(); // 결과를 담는 자료구조
        SolveInfo solveInfo = null; // 클라이언트가 넘긴 정보

        runtime += 2;
        try {
            logger.info(mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            resultMap.put("message", "file is null");
            return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND);
        }

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        File convFile = new File( "."  + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.py"); // 리눅스 서버 절대 경로

        convFile.createNewFile(); // 변환한 파일 위에서 지정한 경로에 생성
        FileOutputStream fos = new FileOutputStream(convFile); // 파일 입력 출력 스트림
        fos.write(mfile.getBytes()); // 파일에서 넘어온 정보 -> 내가 생성한 파일에 입력 (소스코드 넣기)
        fos.close();

        // 파일에서 String 추출
        try {
            Path path = Paths.get("."  + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.py");
            Stream<String> lines = Files.lines(path);
            String content = lines.collect(Collectors.joining(System.lineSeparator())); // 생성한 파일에서 String 형태를 라인 단위로 가져오기
            logger.info("소스코드 : \n {}", content); // 한줄 단위로 소스코드 출력
            solveInfo = new SolveInfo("chan", content, Integer.parseInt(memory), Integer.parseInt(runtime)); // 사용자아이디, 소스코드, 메모리, 실행시간 Dto에 세팅
            lines.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<HashMap<String, Object>> resultList = new ArrayList<>();

        String avgRuntime;
        double sumRuntime = 0;
        int sumMemory = 0;
        int avgMemory;
        boolean isAnswer = true;
        for (int i=1; i<=5; i++) {
            SolveResult solveResult = solveServiceMacPython.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");

            logger.info(" {} 번 문제 solveResult : {}", i, solveResult);
            HashMap<String, Object> fiveMap = new HashMap<>();
            fiveMap.put("result", solveResult.getResult()); // 채점 결과
            fiveMap.put("runtime", String.format("%.2f",solveResult.getTime() / 1000.0));  // 실행 시간
            fiveMap.put("memory", solveResult.getMemory()); // 메모리
            fiveMap.put("problemNo", i+"번");

            resultList.add(fiveMap);

            if (!solveResult.getResult().equals("맞았습니다")) {
                isAnswer = false;
            }

            sumRuntime += solveResult.getTime();
            sumMemory  += solveResult.getMemory();
        }
        avgRuntime = String.format("%.2f",sumRuntime / 5.0 / 1000.0); // 소수점 2자리
        avgMemory  = sumMemory / 5;
        logger.info("평균 실행 시간 : {}", avgRuntime);
        logger.info("정답 여부 : {}", isAnswer);
        logger.info("평균 메모리 : {}", avgMemory);
        HashMap<String, Object> tempMap = new HashMap<>();
        tempMap.put("avgRuntime", avgRuntime);
        tempMap.put("isAnswer", isAnswer);
        tempMap.put("avgMemory", avgMemory);
        resultList.add(tempMap);

        return new ResponseEntity<>(
                resultList
                , HttpStatus.OK);
    }


    @GetMapping("/py")
    public void py() throws IOException, InterruptedException {
        List<String> commands = new ArrayList<>();
        commands.add("python3");
        commands.add("/Users/leechanhee/Desktop/solve.py");
        commands.add("Solution");

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.redirectInput(new File("/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/1/1/input/in1.txt"));
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }


        // 실제 정답도 동일한 과정을 거칩니다.
        StringBuilder expectedOutput = new StringBuilder();
        //try (Scanner sc = new Scanner(new File(OUTPUTFILEROOTDIR + type + "\\" + no + "\\output\\" + "out1.txt"))) {
        try (Scanner sc = new Scanner(new File("/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/1/1/output/out1.txt"))) {
            while (sc.hasNextLine()) {
                expectedOutput.append(sc.nextLine()).append("\n");
            }
        }

        System.out.print("결과 : " + output.toString());
        System.out.print("예상 결과 : " + expectedOutput.toString());

        if (output.toString().equals(expectedOutput.toString())) {
            System.out.println("맞음");
        }
        else {
            System.out.println("틀림");
        }

        process.waitFor();

    }

}
