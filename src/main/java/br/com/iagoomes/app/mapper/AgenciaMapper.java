package br.com.iagoomes.app.mapper;

import br.com.iagoomes.app.api.model.AgenciaRequest;
import br.com.iagoomes.app.api.model.AgenciaResponse;
import br.com.iagoomes.domain.entity.Agencia;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Mapper para conversão entre entidade Agencia e os DTOs gerados pelo OpenAPI.
 *
 * O MapStruct gera automaticamente a implementação desta interface em tempo de compilação.
 * componentModel = "cdi" - Permite injeção via CDI (@Inject)
 * nullValuePropertyMappingStrategy = IGNORE - Ignora valores nulos no update
 * unmappedTargetPolicy = IGNORE - Ignora campos não mapeados (id, timestamps, etc)
 */
@Mapper(
    componentModel = "cdi",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AgenciaMapper {

    /**
     * Converte AgenciaRequest para entidade Agencia
     * Ignora campos que serão preenchidos pelo Service (id, timestamps)
     */
    Agencia toEntity(AgenciaRequest agenciaRequest);

    /**
     * Converte entidade Agencia para AgenciaResponse
     * Converte LocalDateTime para OffsetDateTime automaticamente
     */
    AgenciaResponse toResponse(Agencia agencia);

    /**
     * Atualiza uma entidade existente com dados do AgenciaRequest
     * Propriedades nulas no request são ignoradas
     */
    void updateEntityFromRequest(AgenciaRequest request, @MappingTarget Agencia agencia);

    /**
     * Converte LocalDateTime para OffsetDateTime (UTC)
     * Usado automaticamente pelo MapStruct quando necessário
     */
    default OffsetDateTime map(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atOffset(ZoneOffset.UTC);
    }

    /**
     * Converte OffsetDateTime para LocalDateTime
     * Usado automaticamente pelo MapStruct quando necessário
     */
    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : offsetDateTime.toLocalDateTime();
    }
}
