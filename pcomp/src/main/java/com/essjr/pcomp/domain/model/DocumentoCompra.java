package com.essjr.pcomp.domain.model;

import java.util.List;

public record DocumentoCompra(
        String id,
        List<ItemDoc> itens
) {}
