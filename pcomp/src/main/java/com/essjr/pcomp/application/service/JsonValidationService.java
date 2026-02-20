package com.essjr.pcomp.application.service;


import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;



@Service
public class JsonValidationService {


    private final ObjectMapper objectMapper;

    public JsonValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public void validarJsonOuLancar(String json, String nomeCampo){
        if(json == null || json.isBlank()){
            throw new IllegalArgumentException(nomeCampo + " está vazio");
        }

        try {
            objectMapper.readTree(json);
        }catch (Exception e){
            throw new IllegalArgumentException(nomeCampo + " não é um JSON válido. Erro " + e.getMessage(), e);

        }
    }

}
