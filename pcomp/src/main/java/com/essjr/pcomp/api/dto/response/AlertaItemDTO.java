package com.essjr.pcomp.api.dto.response;

import java.math.BigDecimal;

public record AlertaItemDTO(
        String chave,              // EAN:... ou SKU:...
        String descricao,

        String skuPedido,          // ✅ novo
        String skuNf,              // ✅ novo

        String statusQuantidade,   // OK | FALTA | SOBRA | ITEM_SEM_PEDIDO
        String statusPreco,        // OK | PRECO_DIFERENTE | ""
        String avisoCadastro,      // OK | CODIGO_DIFERENTE_MESMO_EAN | EAN_DIFERENTE_MESMO_CODIGO

        BigDecimal qtdPedido,
        BigDecimal qtdNf,
        BigDecimal totalPedido,
        BigDecimal totalNf
) {}