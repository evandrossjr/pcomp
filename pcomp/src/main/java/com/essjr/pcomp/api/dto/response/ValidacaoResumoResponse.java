package com.essjr.pcomp.api.dto.response;

public record ValidacaoResumoResponse(
        boolean ok,
        String mensagem,
        int totalItensPedido,
        int totalItensNf
) {}
