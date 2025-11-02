package br.com.iagoomes.infra.restprovider.dto;

public class AgenciaDTO {
    private String nome;
    private String razaoSocial;
    private String cnpj;
    private SituacaoCadastralEnum situacaoCadastral;

    public String getNome() {
        return nome;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public SituacaoCadastralEnum getSituacaoCadastral() {
        return situacaoCadastral;
    }
}
