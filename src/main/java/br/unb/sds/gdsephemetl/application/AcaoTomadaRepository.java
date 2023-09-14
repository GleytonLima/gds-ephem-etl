package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.AcaoTomada;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "acoes-tomadas", path = "acoes-tomadas")
public interface AcaoTomadaRepository extends PagingAndSortingRepository<AcaoTomada, Long> {
}
