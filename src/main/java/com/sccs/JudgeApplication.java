package com.sccs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import org.apache.tomcat.jni.Proc;
import org.python.core.PyFunction;
import org.python.util.PythonInterpreter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JudgeApplication {
    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(JudgeApplication.class, args);
    }

}
