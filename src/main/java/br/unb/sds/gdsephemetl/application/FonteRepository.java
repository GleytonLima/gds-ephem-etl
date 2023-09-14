package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.Fonte;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "fontes", path = "fontes")
public interface FonteRepository extends PagingAndSortingRepository<Fonte, Long> {
}
