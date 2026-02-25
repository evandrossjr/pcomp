package com.essjr.pcomp.api.dto.response;

public record XmlExtracaoResponse(
        int arquivosRecebidos,
        int linhasGeradas,
        java.util.List<LinhaXmlDTO> linhas
) {}
