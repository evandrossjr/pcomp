package com.essjr.pcomp.api.dto.response;

import java.math.BigDecimal;

public record AlertaItemDTO(
        String chave,
        String descricao,
        String skuPedido,
        String skuNf,

        // Qtd (diferença) + status
        String statusQuantidade,     // OK | FALTA | SOBRA | FALTA_PARCIAL | ITEM_SEM_PEDIDO
        BigDecimal qtdDiferenca,     // pendente (>0) ou a mais (<0) ou 0

        // Preços do item
        BigDecimal valorUnitPedido,
        BigDecimal valorUnitNf,

        // Status preço (texto pronto: "+ R$ 1,23 (A MAIS)" etc)
        String statusPrecoTexto,

        // Cadastro
        String avisoCadastro,

        // Quantidades e totais
        BigDecimal qtdPedido,
        BigDecimal qtdNf,

        BigDecimal totalPedido,
        BigDecimal totalNf
) {}