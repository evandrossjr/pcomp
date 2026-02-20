package com.essjr.pcomp.api.dto.response;


import java.util.List;

public record RelatorioConferenciaDTO(
        String pedidoId,
        String nfId,
        String status, // OK | ALERTA
        int itensPedido,
        int itensNf,
        List<AlertaItemDTO> alertas
) {}