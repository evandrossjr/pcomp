package com.essjr.pcomp.domain.model;

import java.math.BigDecimal;

public record ItemDoc(
        String sku,
        String ean,
        String descricao,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal valorTotal
) {}