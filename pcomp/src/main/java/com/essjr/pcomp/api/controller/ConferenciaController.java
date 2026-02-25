package com.essjr.pcomp.api.controller;


import com.essjr.pcomp.api.dto.request.ConferenciaJsonRequest;
import com.essjr.pcomp.api.dto.response.RelatorioConferenciaDTO;
import com.essjr.pcomp.application.service.ConferirNotaVsPedidoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conferencia")
public class ConferenciaController {

    private final ConferirNotaVsPedidoService service;

    public ConferenciaController(ConferirNotaVsPedidoService service) {
        this.service = service;
    }

    @PostMapping("/analisar")
    public RelatorioConferenciaDTO analisar(@Valid @RequestBody ConferenciaJsonRequest req) {
        return service.conferir(req.pedidoJson(), req.nfsJson(), req.toleranciaValor());
    }
}