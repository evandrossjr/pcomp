package com.essjr.pcomp.api.dto.response;

import java.math.BigDecimal;

public record LinhaXmlDTO(
        Integer sequencia,
        String numeroNf,
        String arquivo,
        String fornecedorNome,
        String dataEmissao,
        String ean,
        String skuFornecedor,
        String descricao,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal ipiUnitario,
        BigDecimal valorTotal,
        String cfop,
        String naturezaOperacao
        ) {}
