package br.com.iagoomes.infra.restprovider;

import br.com.iagoomes.infra.restprovider.dto.AgenciaDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/situacao-cadastral")
@RegisterRestClient(configKey = "situacao-cadastral-api")
public interface SituacaoCadastralProvider {
    @GET
    @Path("/{cnpj}")
    AgenciaDTO buscarPorCnpj(@PathParam("cnpj") String cnpj);
}
