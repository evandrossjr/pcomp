package com.essjr.pcomp.api.controller;

import com.essjr.pcomp.api.dto.response.XmlExtracaoResponse;
import com.essjr.pcomp.application.service.NfeXmlExtractorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/xml")
public class XmlExtractorController {

    private final NfeXmlExtractorService service;

    public XmlExtractorController(NfeXmlExtractorService service) {
        this.service = service;
    }

    @PostMapping(value = "/extrair", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public XmlExtracaoResponse extrair(@RequestParam("files") MultipartFile[] files) {
        return service.extrair(files);
    }
}
