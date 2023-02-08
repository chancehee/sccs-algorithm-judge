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
    private static PythonInterpreter interpreter;
    @PostMapping("/python/submission")
    public ResponseEntity<?> solveWithPython(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
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
        for (int i=1; i<=5; i++) {
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



//        interpreter = new PythonInterpreter(); // 파이썬 실행기
//        interpreter.execfile("C:\\Users\\workspace\\sccs-online-judge\\src\\main\\resources\\usercode\\test.py"); // 실행시킬 파일경로 명시
//
//        Instant beforeTime = Instant.now(); // Time (1)
//        System.gc();
//        Runtime.getRuntime().gc(); // GC(1)
//
//        PyFunction pyFunction = interpreter.get("main", PyFunction.class); // main이라는 함수 실행
//        PyObject pyObject = pyFunction.__call__();
//
//        System.gc();
//        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); // GC(2)
//
//        Instant afterTime = Instant.now(); // Time(2)
//
//        long secDiffTime = Duration.between(beforeTime, afterTime).toNanos(); // Time(3)
//
//        System.out.println("실행 시간 : " + ((double)secDiffTime) / 1000000000); // Time(4)
//        System.out.println("[GC]사용 메모리 : " + (usedMemory) + " kb"); // 1kb = 0.001 mb // GC(3)
//        System.out.println(pyObject.toString());
//
//
//        SolveResult solveResult = new SolveResult((int) ((secDiffTime) / 1000000000), "정답", (int) usedMemory);
//
//        resultMap.put("result", solveResult.getResult());
//        resultMap.put("runtime", solveResult.getTime());
//        resultMap.put("memory", solveResult.getMemory());

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


    @GetMapping("/getPython")
    public String getPy() {
        String scriptPath = "/Users/leechanhee/Desktop/sccs-online-judge/src/main/test1.py";
        String memoryLimit = "512m";

        ProcessBuilder builder = new ProcessBuilder("python", scriptPath);
        try {
            System.out.println("실행 시작 ");
            Process process = builder.start();
            System.out.println("실행 중...");
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "성공";
    }

    @PostMapping("/testPython")
    public ResponseEntity<?> testPython(MultipartFile mfile) throws IOException {
        HashMap<String, String> resultMap = new HashMap<>(); // 응답 결과 자료구조

        if (mfile == null) { // 프록시서버로 부터 파일이 넘어오지 않은 경우
            resultMap.put("message", "멀티파일 응답 실패.. 다시 보내주세요");
            resultMap.put("result", "fail");
            return new ResponseEntity<>(resultMap, HttpStatus.OK);
        }

        logger.info("멀티파트 파일 원본 이름 : {}", mfile.getOriginalFilename());

        String fullPath = "C:\\Users\\workspace\\sccs-online-judge\\src\\main\\resources\\usercode" + mfile.getOriginalFilename(); // [윈도우 기준] 파일 생성 경로
        mfile.transferTo(new File(fullPath)); // 멀티파트 파일 convert to 파일
        logger.debug("소스코드 파일 경로 : {}", fullPath);

        interpreter = new PythonInterpreter(); // 파이썬 실행기
        interpreter.execfile(fullPath); // 실행위치 지정
        PyFunction pyFunction = interpreter.get("main", PyFunction.class); // 메인 함수 실행
        PyObject pyObject = pyFunction.__call__(); // 리턴 값
        String result = pyObject.toString();

        // 실행 결과
        StringBuilder output = new StringBuilder();

        // 정답
        StringBuilder expectedOutput = new StringBuilder();
        try (Scanner sc = new Scanner(new File("C:\\Users\\workspace\\sccs-online-judge\\src\\main\\resources\\output.txt"))) {
            System.out.println(sc);
            while (sc.hasNextLine()) {
                expectedOutput.append(sc.nextLine()).append("\n");
            }
        }

        logger.debug("expectedOutput : {}", expectedOutput);

        if (result.equals("2")) resultMap.put("result", "정답");
        else resultMap.put("result", "오답");

        resultMap.put("message", "소스코드 실행 성공");
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

    @PostMapping("/java/submission") // 5개 테스트 케이스 채점
    public ResponseEntity<?> solveWithJava(MultipartFile mfile , String type, String no, String memory, String runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null; // 사용자가 제출한 정보

        logger.debug("자바 채점 컨트롤러");

        try {
            logger.info(mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            System.out.println("file is null [mintChoco Java]"); // 클라이언트에게 넘어온 파일이 null인 경우
        }

//        type     = "1"; // 클라이언트에게 넘겨받을 값
//        no       = "1"; // 문제 번호
//        memory   = 256; // 메모리
//        runtime   = 2; // 시간

//        보내줄것 3개 : result(String), memory(int), runtime(int)

        HashMap<String, Object> resultMap = new HashMap<>(); // 결과값 저장 자료구조

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        // mfile to file (변환)
        // 윈도우
        //File convFile = new File(".\\src\\main\\resources\\file\\Solution.java");
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
        for (int i=1; i<=5; i++) {
            SolveResult solveResult = solveServiceJava.solve(solveInfo, type, no, "in"+i+".txt", "out"+i+".txt");

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

        logger.debug("자바 테스트 케이스 채점 컨트롤러 ");

        try {
            logger.info(mfile.getOriginalFilename()); // 클라이언트에게 넘어온 파일 이름 출력
        } catch (Exception e) {
            System.out.println("file is null [mintChoco]"); // 클라이언트에게 넘어온 파일이 null인 경우
        }

        HashMap<String, Object> resultMap = new HashMap<>(); // 결과값 저장 자료구조

        logger.info("type, no, memory, runtime : {}" , type + " " + no + " " + memory + " " + runtime); // 클라이언트에서 넘어온 type, no, memory, runtime 출력

        // mfile to file (변환)
        // 윈도우
        //File convFile = new File(".\\src\\main\\resources\\file\\Solution.java");
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
}
