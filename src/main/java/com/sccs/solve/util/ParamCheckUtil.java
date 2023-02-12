package com.sccs.solve.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
public class ParamCheckUtil {

    private final Logger logger = LoggerFactory.getLogger(ParamCheckUtil.class);

    /** 파일 null 체크 **/
    public boolean isValidFile(MultipartFile mfile) {
        if (mfile == null || mfile.isEmpty()) {
            logger.debug("[isValidFile]file is null");
            return false;
        }
        logger.debug("[isValidFile] file name : {}", mfile.getOriginalFilename());
        return true;
    }

    /** 파라미터 null 체크 **/
    public boolean isValidParameter(String type, String no, String runtime, String memory) {

        if (type == null || no == null || runtime == null || memory == null ||
            type.equals("") || no.equals("") || runtime.equals("") || memory.equals("")
        ) {
            logger.debug("parameter is not valid");
            return false;
        }
        logger.debug("{} {} {} {}", type, no, runtime, memory);
        return true;
    }

}
