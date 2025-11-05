package br.com.iagoomes.app.resource;

import br.com.iagoomes.app.api.AgenciaApi;
import br.com.iagoomes.app.api.model.AgenciaRequest;
import br.com.iagoomes.app.mapper.AgenciaMapper;
import br.com.iagoomes.app.service.AgenciaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/**
 * Implementação da API de Agências.
 *
 * Esta classe implementa a interface AgenciaApi gerada automaticamente
 * pelo OpenAPI Generator e contém a lógica de integração entre a camada
 * de apresentação (API) e a camada de negócio (Service).
 */
@ApplicationScoped
public class AgenciaApiImpl implements AgenciaApi {

    @Inject
    AgenciaService agenciaService;

    @Inject
    AgenciaMapper agenciaMapper;

    @Override
    public Response listarAgencias(Integer page, Integer size) {
        // TODO: Implementar paginação
        var agencias = agenciaService.listarTodas();
        var response = agencias.stream()
                .map(agenciaMapper::toResponse)
                .toList();

        return Response.ok(response).build();
    }

    @Override
    public Response criarAgencia(AgenciaRequest agenciaRequest) {
        var agencia = agenciaMapper.toEntity(agenciaRequest);
        var agenciaCriada = agenciaService.criar(agencia);
        var response = agenciaMapper.toResponse(agenciaCriada);

        return Response.status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @Override
    public Response buscarAgenciaPorId(Long id) {
        return agenciaService.buscarPorId(id)
                .map(agenciaMapper::toResponse)
                .map(response -> Response.ok(response).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Override
    public Response atualizarAgencia(Long id, AgenciaRequest agenciaRequest) {
        return agenciaService.buscarPorId(id)
                .map(agenciaExistente -> {
                    agenciaMapper.updateEntityFromRequest(agenciaRequest, agenciaExistente);
                    var agenciaAtualizada = agenciaService.atualizar(agenciaExistente);
                    var response = agenciaMapper.toResponse(agenciaAtualizada);
                    return Response.ok(response).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @Override
    public Response deletarAgencia(Long id) {
        return agenciaService.buscarPorId(id)
                .map(agencia -> {
                    agenciaService.deletar(id);
                    return Response.noContent().build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
}
