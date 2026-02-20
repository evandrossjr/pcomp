package com.essjr.pcomp.api.dto.response;

import java.math.BigDecimal;

public record AlertaItemDTO(
        String chave,
        String tipo, // FALTA, SOBRA, VALOR_DIVERGENTE, ITEM_NAO_PEDIDO
        BigDecimal qtdPedido,
        BigDecimal qtdNf,
        BigDecimal valorUnitPedido,
        BigDecimal valorUnitNf,
        BigDecimal totalPedido,
        BigDecimal totalNf,
        String mensagem
) {}