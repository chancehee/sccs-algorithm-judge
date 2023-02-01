package com.sccs.solve.controller;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import com.sccs.solve.service.SolveService;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import com.sun.tools.jdeprscan.scan.Scan;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.jni.Proc;
import org.python.antlr.ast.Str;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
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
@RequestMapping("/api/solve")
@RequiredArgsConstructor
public class SolveController {

    private final Logger logger = LoggerFactory.getLogger(SolveController.class);
    private final SolveService solveService; // RequiredConstructor : final이나 @NonNull인 필드값만 파라미터로 받는 생성자를 만들어준다.
    private static PythonInterpreter interpreter;

    @GetMapping("/python")
    public ResponseEntity<?> solveWithPython() throws IOException, InterruptedException {
        interpreter = new PythonInterpreter();
        interpreter.execfile("C:\\Users\\workspace\\sccs-online-judge\\src\\main\\resources\\usercode\\test.py");

        Instant beforeTime = Instant.now(); // Time (1)
        System.gc();
        Runtime.getRuntime().gc(); // GC(1)

        PyFunction pyFunction = interpreter.get("main", PyFunction.class);
        PyObject pyObject = pyFunction.__call__();

        System.gc();
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); // GC(2)

        Instant afterTime = Instant.now(); // Time(2)

        long secDiffTime = Duration.between(beforeTime, afterTime).toNanos(); // Time(3)

        System.out.println("실행 시간 : " + ((double)secDiffTime) / 1000000000); // Time(4)
        System.out.println("[GC]사용 메모리 : " + (usedMemory) + " kb"); // 1kb = 0.001 mb // GC(3)
        System.out.println(pyObject.toString());

        return new ResponseEntity<>(pyObject.toString(), HttpStatus.OK);
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


        StringBuilder output = new StringBuilder();

        if (result.equals("2")) resultMap.put("result", "정답");
        else resultMap.put("result", "오답");

        resultMap.put("message", "소스코드 실행 성공");
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }






    @GetMapping("/java")
    public ResponseEntity<?> solveWithJava(MultipartFile mfile) throws IOException, InterruptedException {
        SolveInfo solveInfo = new SolveInfo("ssafy", "class Main { public static void main(String[] args) { System.out.print(1); } }", 256, 2);

        System.out.println(solveInfo);

        SolveResult solveResult = solveService.solve(solveInfo);
        System.out.println(solveResult);

        return new ResponseEntity<>(
                "return"
                , HttpStatus.OK);
    }



    @PostMapping("/testJava")
    public ResponseEntity<?> testJava() throws IOException, InterruptedException {
        HashMap<String, String> resultMap = new HashMap<>();

        File file = new File("/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/usercode/test.java");
        System.out.println(file.getName());


        ProcessBuilder pb = new ProcessBuilder("javac", "/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/usercode/test.java");
        Process process = pb.start();
        process.waitFor();

        if(process.exitValue() != 0 ) {
            System.out.println("컴파일 에러");
            return null;
        }

        pb = new ProcessBuilder("java", "-Xmx" + 256 + "m", "-cp", "/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/usercode/", "test");
        pb.redirectInput(new File("/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/input/input.txt"));

        process = pb.start();

        StringBuilder output = new StringBuilder();
        try (Scanner sc = new Scanner(process.getInputStream())) {
            System.out.println(sc);
            while (sc.hasNextLine()) {
                System.out.println("hi");
                output.append(sc.nextLine()).append("\n");
            }
        }

        // 실제 정답도 동일한 과정을 거칩니다.
        StringBuilder expectedOutput = new StringBuilder();
        try (Scanner sc = new Scanner(new File("/Users/leechanhee/Desktop/sccs-online-judge/src/main/resources/output/output.txt"))) {
            System.out.println(sc);
            while (sc.hasNextLine()) {
                System.out.println("bye");
                expectedOutput.append(sc.nextLine()).append("\n");
            }
        }

        System.out.println("실행 결과 : " + output.toString());
        System.out.println("예상 결과 : " + expectedOutput.toString());


        resultMap.put("message", "success");
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

}
