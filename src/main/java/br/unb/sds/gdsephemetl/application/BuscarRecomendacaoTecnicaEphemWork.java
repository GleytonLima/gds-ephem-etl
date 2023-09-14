package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.ModelApiResponse;
import br.unb.sds.gdsephemetl.application.model.RecomendacaoTecnica;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuscarRecomendacaoTecnicaEphemWork {
    public static final long ID_DEFAULT = 1L;
    private final RecomendacaoTecnicaRepository recomendacaoTecnicaRepository;
    private final ConfiguracaoRepository configuracaoRepository;

    private final RestTemplate restTemplate;

    @Transactional
    public void processar() {
        recomendacaoTecnicaRepository.deleteAll();

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
                final var url = urlRemoteDb + "/api-integracao/v1/models/eoc.signal.technical.recommendation?page=" + page + "&size=" + size;
                log.info("Iniciando busca de recomendacoes tecnicas em: {}", url);
                log.info("Buscando recomendacoes tecnicas página {}...", page);
                final var response = restTemplate.getForObject(url, ModelApiResponse.class);
                log.info("Buscando recomendacoes tecnicas página {} finalizada", page);
                if (response != null && response.getEmbedded() != null && !response.getEmbedded().getModels().isEmpty()) {
                    for (final var signalData : response.getEmbedded().getModels()) {
                        final var recomendacao = new RecomendacaoTecnica();
                        recomendacao.setDados(signalData.getAttributes());
                        recomendacaoTecnicaRepository.save(recomendacao);
                    }
                    log.info("{} recomendacoes tecnicas salvas com sucesso.", response.getEmbedded().getModels().size());
                    page++;
                } else {
                    log.info("Dados recomendacoes tecnicas retornados vazios. Encerrando busca.");
                    hasMoreSignals = false;
                }
            } catch (Exception e) {
                log.error("Erro ao buscar recomendacoes tecnicas da página " + page + ".", e);
                throw e;
            }
        }
        log.info("Busca de recomendacoes tecnicas finalizada.");
    }
}
