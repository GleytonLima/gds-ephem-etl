package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.RecomendacaoTecnica;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "recomendacoes-tecnicas", path = "recomendacoes-tecnicas")
public interface RecomendacaoTecnicaRepository extends PagingAndSortingRepository<RecomendacaoTecnica, Long> {
}
