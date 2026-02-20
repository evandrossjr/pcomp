package com.essjr.pcomp.api.controller;

import com.essjr.pcomp.api.dto.request.ValidarJsonRequest;
import com.essjr.pcomp.api.dto.response.ValidacaoResumoResponse;
import com.essjr.pcomp.application.service.EstruturaMinimaValidationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conferencia")
public class ValidacaoController {

    private final EstruturaMinimaValidationService estruturaService;

    public ValidacaoController(EstruturaMinimaValidationService estruturaService) {
        this.estruturaService = estruturaService;
    }

    @PostMapping("/validar")
    public ResponseEntity<ValidacaoResumoResponse> validar(
            @Valid @RequestBody ValidarJsonRequest req) {

        estruturaService.validarPedido(req.pedidoJson());
        estruturaService.validarNf(req.nfJson());

        int totalPedido = req.pedidoJson()
                .path("produtos_consulta")
                .size();

        int totalNf = req.nfJson()
                .path("itensRecebimento")
                .size();

        return ResponseEntity.ok(
                new ValidacaoResumoResponse(
                        true,
                        "JSONs válidos e com estrutura mínima OK.",
                        totalPedido,
                        totalNf
                )
        );
    }
}
