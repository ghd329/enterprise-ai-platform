package com.enterprise.platform.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Component
public class PdfTextExtractor {

    public String extractText(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PDF 파일을 선택해주세요."
            );
        }

        try {
            byte[] fileBytes = file.getBytes();

            // PDF 파일인지 기본 확인
            if (fileBytes.length < 5
                    || !new String(fileBytes, 0, 5)
                    .equals("%PDF-")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "올바른 PDF 파일이 아닙니다."
                );
            }

            // PDF 열기
            try (PDDocument document = Loader.loadPDF(fileBytes)) {

                // PDF 내부 텍스트 추출
                PDFTextStripper stripper = new PDFTextStripper();

                String text = stripper.getText(document).trim();

                if (text.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "PDF에서 추출할 텍스트가 없습니다."
                    );
                }

                return text;
            }

        } catch (ResponseStatusException e) {
            throw e;

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PDF 파일을 읽을 수 없습니다."
            );
        }
    }
}
