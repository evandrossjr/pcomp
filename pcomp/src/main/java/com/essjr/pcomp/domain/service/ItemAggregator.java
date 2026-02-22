package com.essjr.pcomp.domain.service;


import com.essjr.pcomp.domain.model.ItemDoc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class ItemAggregator {

    public static Map<String, ItemDoc> agruparPorChave(List<ItemDoc> itens) {
        Map<String, MutableAgg> acc = new LinkedHashMap<>();

        for (ItemDoc i : itens) {
            String chave = chaveProduto(i);

            MutableAgg m = acc.computeIfAbsent(chave, k -> new MutableAgg(i.sku(), i.ean(), i.descricao()));
            m.qtd = m.qtd.add(nz(i.quantidade()));
            m.total = m.total.add(nz(i.valorTotal()));

            // se o total do item vier zero/null, tenta somar por unit*qtd
            if ((i.valorTotal() == null || i.valorTotal().compareTo(BigDecimal.ZERO) == 0)
                    && i.valorUnitario() != null && i.quantidade() != null) {
                m.total = m.total.add(i.valorUnitario().multiply(i.quantidade()));
            }
        }

        Map<String, ItemDoc> out = new LinkedHashMap<>();
        for (var e : acc.entrySet()) {
            MutableAgg m = e.getValue();

            BigDecimal unitMedio = BigDecimal.ZERO;
            if (m.qtd.compareTo(BigDecimal.ZERO) > 0) {
                unitMedio = m.total.divide(m.qtd, 6, RoundingMode.HALF_UP);
            }

            out.put(e.getKey(), new ItemDoc(
                    m.sku, m.ean, m.desc,
                    m.qtd, unitMedio, m.total
            ));
        }

        return out;
    }

    public static String chaveProduto(ItemDoc i) {
        if (i.ean() != null && !i.ean().isBlank()) return "EAN:" + i.ean().trim(); // não normaliza EAN
        if (i.sku() != null && !i.sku().isBlank()) return "SKU:" + normalizarSku(i.sku());
        return "DESC:" + ((i.descricao() == null) ? "" : i.descricao().trim().toUpperCase());
    }

    private static String normalizarSku(String sku) {
        String s = sku.trim();
        // remove zeros à esquerda (somente para SKU)
        return s.replaceFirst("^0+(?!$)", "");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class MutableAgg {
        String sku;
        String ean;
        String desc;
        BigDecimal qtd = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        MutableAgg(String sku, String ean, String desc) {
            this.sku = sku;
            this.ean = ean;
            this.desc = desc;
        }
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null) return null;

        String limpo = codigo.trim();

        // remove zeros à esquerda
        limpo = limpo.replaceFirst("^0+(?!$)", "");

        return limpo;
    }
}