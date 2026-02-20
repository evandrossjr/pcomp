package com.essjr.pcomp.api.dto.response;

import java.math.BigDecimal;

public record AlertaItemDTO(
        String chave,
        String descricao,
        String statusQuantidade,   // OK | FALTA | SOBRA | ITEM_SEM_PEDIDO
        String statusPreco,        // OK | PRECO_DIFERENTE | ""
        BigDecimal qtdPedido,
        BigDecimal qtdNf,
        BigDecimal totalPedido,
        BigDecimal totalNf
) {}