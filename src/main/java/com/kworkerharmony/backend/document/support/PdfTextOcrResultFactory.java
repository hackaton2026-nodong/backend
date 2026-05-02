package com.kworkerharmony.backend.document.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfTextOcrResultFactory {

    private final ObjectMapper objectMapper;

    public PdfTextOcrResultFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode create(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode layoutParsingResults = root.putArray("layoutParsingResults");
            PDFTextStripper stripper = new PDFTextStripper();

            for (int page = 1; page <= document.getNumberOfPages(); page += 1) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = stripper.getText(document).replace("\r", "\n").trim();
                ObjectNode pageResult = layoutParsingResults.addObject();
                pageResult.put("page", page);
                pageResult.putObject("markdown").put("text", pageText);

                ArrayNode blocks = pageResult.putObject("prunedResult").putArray("parsing_res_list");
                int pageNumber = page;
                pageText.lines()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .forEach(line -> addBlock(blocks, pageNumber, line));
            }

            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to extract text from PDF", ex);
        }
    }

    private void addBlock(ArrayNode blocks, int page, String text) {
        ObjectNode block = blocks.addObject();
        block.put("page", page);
        block.put("block_content", text);
        block.put("confidence", 0.9);
        ArrayNode bbox = block.putArray("block_bbox");
        bbox.add(0);
        bbox.add(0);
        bbox.add(0);
        bbox.add(0);
    }
}
