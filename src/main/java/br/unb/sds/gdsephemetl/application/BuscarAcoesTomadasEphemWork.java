package br.unb.sds.gdsephemetl.application;

import br.unb.sds.gdsephemetl.application.model.AcaoTomada;
import br.unb.sds.gdsephemetl.application.model.ModelApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BuscarAcoesTomadasEphemWork {
    public static final long ID_DEFAULT = 1L;
    private final AcaoTomadaRepository acaoTomadaRepository;
    private final ConfiguracaoRepository configuracaoRepository;

    private final RestTemplate restTemplate;

    @Transactional
    public void processar() {
        acaoTomadaRepository.deleteAll();

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
                final var url = urlRemoteDb + "/api-integracao/v1/models/eoc.signal.actions.taken?page=" + page + "&size=" + size;
                log.info("Iniciando busca de acoes tomadas em: {}", url);
                log.info("Buscando acoes tomadas página {}...", page);
                final var response = restTemplate.getForObject(url, ModelApiResponse.class);
                log.info("Buscando acoes tomadas página {} finalizada", page);
                if (response != null && response.getEmbedded() != null && !response.getEmbedded().getModels().isEmpty()) {
                    for (final var signalData : response.getEmbedded().getModels()) {
                        final var acaoTomada = new AcaoTomada();
                        acaoTomada.setDados(signalData.getAttributes());
                        acaoTomadaRepository.save(acaoTomada);
                    }
                    log.info("{} acoes tomadas salvas com sucesso.", response.getEmbedded().getModels().size());
                    page++;
                } else {
                    log.info("Dados acoes tomadas retornados vazios. Encerrando busca.");
                    hasMoreSignals = false;
                }
            } catch (Exception e) {
                log.error("Erro ao buscar acoes tomadas da página " + page + ".", e);
                throw e;
            }
        }
        log.info("Busca de acoes tomadas finalizada.");
    }
}
