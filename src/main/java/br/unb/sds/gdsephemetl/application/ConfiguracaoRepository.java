package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.Configuracao;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "configuracoes", path = "configuracoes")
public interface ConfiguracaoRepository extends PagingAndSortingRepository<Configuracao, Long> {
}
