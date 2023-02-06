package com.sccs.solve.controller;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import com.sccs.solve.service.SolveService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.jni.Proc;
import org.python.antlr.ast.Str;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/solve")
@RequiredArgsConstructor
public class SolveController {

    private final Logger logger = LoggerFactory.getLogger(SolveController.class);
    private final SolveService solveService; // RequiredConstructor : final이나 @NonNull인 필드값만 파라미터로 받는 생성자를 만들어준다.
    private static PythonInterpreter interpreter;

    @GetMapping("/python")
    public ResponseEntity<?> solveWithPython(MultipartFile mfile , String type, String no, int memory, int runtime) throws IOException, InterruptedException {
        HashMap<String, Object> resultMap = new HashMap<>();
        SolveInfo solveInfo = null;

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


        SolveResult solveResult = new SolveResult((int) ((secDiffTime) / 1000000000), "정답", (int) usedMemory);

        resultMap.put("result", solveResult.getResult());
        resultMap.put("runtime", solveResult.getTime());
        resultMap.put("memory", solveResult.getMemory());

        return new ResponseEntity<>(
                resultMap
                , HttpStatus.OK);
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




    @PostMapping("/java")
    public ResponseEntity<?> solveWithJava(MultipartFile mfile , String type, String no, int memory, int runtime) throws IOException, InterruptedException {
        SolveInfo solveInfo = null;

        try {
            System.out.println(mfile.getOriginalFilename());
        } catch (Exception e) {
            System.out.println("file is null mintChoco");
        }

        //SolveInfo solveInfo = new SolveInfo("ssafy", "class Solution { public static void main(String[] args) { System.out.print(8); } }", 256, 2);

//        type     = "1"; // 클라이언트에게 넘겨받을 값
//        no       = "1"; // 문제 번호
//        memory   = 256; // 메모리
//        runtime   = 2; // 시간

        // 보내줄것 3개
        // result
        // memory
        // 런타임
        HashMap<String, Object> resultMap = new HashMap<>();

        System.out.println(type + " " + no + " " + memory + " " + runtime);

        // mfile to file (변환)
        //File convFile = new File(".\\src\\main\\resources\\file\\Solution.java");


        File convFile = new File( File.separator + "judgeonline" + File.separator + "sccs-online" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.java");
        logger.info("넘어온 파일명 : {}", mfile.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(mfile.getBytes());
        fos.close();

        // 파일에서 String 추출
        try {
            // Path path = Paths.get(".\\src\\main\\resources\\file\\Solution.java");
            Path path = Paths.get(File.separator + "judgeonline" + File.separator + "sccs-online" + File.separator + "src" + File.separator + "main" + File.separator+ "resources" + File.separator + "file" + File.separator + "Solution.java");
            Stream<String> lines = Files.lines(path);

            String content = lines.collect(Collectors.joining(System.lineSeparator()));
            logger.info("소스코드 : \n {}", content);
            solveInfo = new SolveInfo("chan", content, memory, runtime);
            lines.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        SolveResult solveResult = solveService.solve(solveInfo, type, no);

        resultMap.put("result", solveResult.getResult());
        resultMap.put("runtime", solveResult.getTime());
        resultMap.put("memory", solveResult.getMemory());


        return new ResponseEntity<>(
                resultMap
                , HttpStatus.OK);
    }
}
