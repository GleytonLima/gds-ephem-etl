package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.Sinal;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "sinais", path = "sinais")
public interface SinalRepository extends PagingAndSortingRepository<Sinal, Long> {
}
