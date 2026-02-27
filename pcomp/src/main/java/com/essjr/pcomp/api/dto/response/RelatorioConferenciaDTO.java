package com.essjr.pcomp.api.dto.response;


import java.math.BigDecimal;
import java.util.List;

public record RelatorioConferenciaDTO(
        String pedidoId,
        String nfId,
        String status,

        int itensPedido,
        int itensNf,

        BigDecimal totalPedidoGeral,
        BigDecimal totalNfGeral,

        List<AlertaItemDTO> alertas
) {}