package br.com.iagoomes.app.service;

import br.com.iagoomes.domain.entity.Agencia;
import br.com.iagoomes.infra.restprovider.SituacaoCadastralProvider;
import br.com.iagoomes.infra.restprovider.dto.AgenciaDTO;
import br.com.iagoomes.infra.restprovider.dto.SituacaoCadastralEnum;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de negócio para Agências.
 *
 * Implementação temporária usando lista em memória.
 * TODO: Migrar para persistência com Panache Repository quando configurar o banco de dados.
 */
@ApplicationScoped
public class AgenciaService {

    @RestClient
    private SituacaoCadastralProvider situacaoCadastralProvider;

    private final List<Agencia> agencias = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Lista todas as agências cadastradas
     */
    public List<Agencia> listarTodas() {
        return new ArrayList<>(agencias);
    }

    /**
     * Busca uma agência por ID
     */
    public Optional<Agencia> buscarPorId(Long id) {
        return agencias.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
    }

    /**
     * Cria uma nova agência
     * Valida a situação cadastral antes de criar
     */
    public Agencia criar(Agencia agencia) {
        // Valida situação cadastral se tiver CNPJ
        if (agencia.getCnpj() != null && !agencia.getCnpj().isBlank()) {
            validarSituacaoCadastral(agencia.getCnpj());
        }

        // Define ID e timestamps
        agencia.setId(idGenerator.getAndIncrement());
        agencia.setDataCadastro(LocalDateTime.now());
        agencia.setDataAtualizacao(LocalDateTime.now());

        agencias.add(agencia);
        return agencia;
    }

    /**
     * Atualiza uma agência existente
     */
    public Agencia atualizar(Agencia agencia) {
        agencia.setDataAtualizacao(LocalDateTime.now());
        return agencia;
    }

    /**
     * Remove uma agência por ID
     */
    public void deletar(Long id) {
        agencias.removeIf(a -> a.getId().equals(id));
    }

    /**
     * Valida a situação cadastral da agência através do provider externo
     */
    private void validarSituacaoCadastral(String cnpj) {
        AgenciaDTO agenciaDTO = situacaoCadastralProvider.buscarPorCnpj(cnpj);

        if (agenciaDTO == null || !agenciaDTO.getSituacaoCadastral().equals(SituacaoCadastralEnum.ATIVO)) {
            throw new IllegalArgumentException("Agência com CNPJ " + cnpj + " não está ativa.");
        }
    }
}
