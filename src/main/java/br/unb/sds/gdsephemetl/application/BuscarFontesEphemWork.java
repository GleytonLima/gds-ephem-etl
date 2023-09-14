package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.Fonte;
import br.unb.sds.gdsephemetl.application.model.ModelApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuscarFontesEphemWork {
    public static final long ID_DEFAULT = 1L;
    private final FonteRepository fonteRepository;
    private final ConfiguracaoRepository configuracaoRepository;

    private final RestTemplate restTemplate;

    @Transactional
    public void processar() {
        fonteRepository.deleteAll();

        int page = 0;
        int size = 100;
        boolean hasMoreSignals = true;

        final var configuracao = configuracaoRepository.findById(ID_DEFAULT);
        if (configuracao.isEmpty()) {
            log.error("URL do banco de dados remoto não configurada.");
            return;
        }
        while (hasMoreSignals) {
            try {
                final var urlRemoteDb = configuracao.get().getDominioRemoto();
                final var url = urlRemoteDb + "/api-integracao/v1/models/eoc.signal.sources?page=" + page + "&size=" + size;
                log.info("Iniciando busca de fontes em: {}", url);
                log.info("Buscando fontes página {}...", page);
                final var response = restTemplate.getForObject(url, ModelApiResponse.class);
                log.info("Buscando fontes página {} finalizada", page);
                if (response != null && response.getEmbedded() != null && !response.getEmbedded().getModels().isEmpty()) {
                    for (final var signalData : response.getEmbedded().getModels()) {
                        final var fonte = new Fonte();
                        fonte.setDados(signalData.getAttributes());
                        fonteRepository.save(fonte);
                    }
                    log.info("{} fontes salvas com sucesso.", response.getEmbedded().getModels().size());
                    page++;
                } else {
                    log.info("Dados fontes retornados vazios. Encerrando busca.");
                    hasMoreSignals = false;
                }
            } catch (Exception e) {
                log.error("Erro ao buscar fontes da página " + page + ".", e);
                throw e;
            }
        }
        log.info("Busca de fontes finalizada.");
    }
}
