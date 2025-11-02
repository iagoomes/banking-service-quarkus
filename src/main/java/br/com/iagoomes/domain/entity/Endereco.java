package br.com.iagoomes.domain.entity;

public class Endereco {
    private Integer id;
    private String rua;
    private String logradouro;
    private String complemento;
    private Integer numero;

    public Integer getId() {
        return id;
    }

    public String getRua() {
        return rua;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getComplemento() {
        return complemento;
    }

    public Integer getNumero() {
        return numero;
    }
}
