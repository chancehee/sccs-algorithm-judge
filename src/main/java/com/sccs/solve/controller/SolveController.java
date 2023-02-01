package com.sccs.solve.controller;

import com.sccs.solve.dto.SolveInfo;
import com.sccs.solve.dto.SolveResult;
import com.sccs.solve.service.SolveService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import lombok.RequiredArgsConstructor;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/solve")
@RequiredArgsConstructor
public class SolveController {

    private final SolveService solveService; // RequiredConstructor : final이나 @NonNull인 필드값만 파라미터로 받는 생성자를 만들어준다.
    private static PythonInterpreter interpreter;

    @GetMapping("/python")
    public ResponseEntity<?> solveWithPython() throws IOException, InterruptedException {
        // 파이썬 코드를 돌리는 로직
        interpreter = new PythonInterpreter();
        interpreter.execfile("C:\\Users\\SSAFY\\Desktop\\test.py");

        Instant beforeTime = Instant.now();
        System.gc();
        Runtime.getRuntime().gc();

        PyFunction pyFunction = interpreter.get("main", PyFunction.class);
        PyObject pyObject = pyFunction.__call__();

        System.gc();
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Instant afterTime = Instant.now();

        long secDiffTime = Duration.between(beforeTime, afterTime).toNanos();

        System.out.println("실행 시간 : " + ((double)secDiffTime) / 1000000000);
        System.out.println("[GC]사용 메모리 : " + (usedMemory) + " kb"); // 1kb = 0.001 mb
        System.out.println(pyObject.toString());
        System.out.println(pyObject.getType()); // int

        return new ResponseEntity<>(pyObject.toString(), HttpStatus.OK);
    }

    @PostMapping("/testPython")
    public ResponseEntity<?> testPython(MultipartFile mfile) throws IOException {
//        File file = new File("C:\\Users\\SSAFY\\Desktop\\test.py");
        System.out.println(mfile);
        System.out.println(mfile.getOriginalFilename());

        String fullPath = "";
        if (!mfile.isEmpty()) {
            fullPath = "C:\\Users\\SSAFY\\Desktop\\" + mfile.getOriginalFilename();
            System.out.println(fullPath);
            mfile.transferTo(new File(fullPath));
        }

        interpreter = new PythonInterpreter();
        interpreter.execfile(fullPath);
        PyFunction pyFunction = interpreter.get("main", PyFunction.class);
        PyObject pyObject = pyFunction.__call__();

        System.out.println(pyObject.toString());

        HashMap<String, String> resultMap = new HashMap<>();
        resultMap.put("result", "success");
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

    @GetMapping("/java")
    public ResponseEntity<?> solveWithJava() throws IOException, InterruptedException {

        SolveInfo solveInfo = new SolveInfo("ssafy", "class Main {"
                + "public static void main(String args[]) {return 1;;}"
                + "}", 256, 2);

        System.out.println(solveInfo);

        SolveResult solveResult = solveService.solve(solveInfo);
        System.out.println(solveResult);

        return new ResponseEntity<>(
                "return"
                , HttpStatus.OK);
    }
}
