package com.essjr.pcomp.application.infrastructure.parser;


import com.essjr.pcomp.domain.model.DocumentoCompra;
import com.essjr.pcomp.domain.model.ItemDoc;
import tools.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class OmiePedidoParser {

    public DocumentoCompra parse(JsonNode pedidoJson) {
        String id = pedidoJson.path("cabecalho_consulta").path("cNumero").asText(null);

        List<ItemDoc> itens = new ArrayList<>();
        for (JsonNode p : pedidoJson.path("produtos_consulta")) {
            String sku = text(p, "cProduto");
            String ean = text(p, "cEAN");
            String desc = text(p, "cDescricao");

            BigDecimal qtd = dec(p, "nQtde");
            BigDecimal unit = dec(p, "nValUnit");
            BigDecimal total = dec(p, "nValTot");

            if (total == null && qtd != null && unit != null) total = unit.multiply(qtd);

            itens.add(new ItemDoc(sku, ean, desc, qtd, unit, total));
        }

        return new DocumentoCompra(id, itens);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static BigDecimal dec(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.decimalValue();
        String s = v.asText();
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        if (s.contains(",")) s = s.replace(".", "").replace(",", ".");
        return new BigDecimal(s);
    }
}
