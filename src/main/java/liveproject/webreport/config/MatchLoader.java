package liveproject.webreport.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import liveproject.webreport.match.Match;
import liveproject.webreport.match.MatchRepository;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;

@Log
@Component
public class MatchLoader implements
        ApplicationListener<ContextRefreshedEvent> {

    @Value("${testdata.loadfile}")
    private String loadfileResourceName;
    @Value("${testdata.season}")
    private String loadFileSeason;

    private MatchRepository repository;
    private ApplicationContext appContext;

    @Autowired
    public MatchLoader(MatchRepository repository, ApplicationContext ctx) {
        this.repository = repository;
        this.appContext = ctx;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // load test data if specified
        if (StringUtils.isEmpty(loadFileSeason)) return;
        // if we get here, we load test data
        Resource resource =
                appContext.getResource(loadfileResourceName);
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Match.Result.class, new MatchResultDeserializer());
        objectMapper.registerModule(module);
        int newCount = 0;
        int alreadyCount = 0;
        try {
            List<Match> list = objectMapper.readValue(resource.getInputStream(), new TypeReference<List<Match>>() {
            });
            //        list.stream().map(m -> repository.save(m));
            for (Match m : list) {
                if (repository.findByHomeTeamAndAwayTeamAndGameDate(m.getHomeTeam(), m.getAwayTeam(), m.getGameDate()).isEmpty()) {
                    m.setSeason(loadFileSeason);
                    repository.save(m);
                    newCount++;
                    log.info("Adding new record: " + m.getHomeTeam() + " v " + m.getAwayTeam());
                } else {
                    alreadyCount++;
                    log.info("Skipping previous record: " + m.getHomeTeam() + " v " + m.getAwayTeam());
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error initializing database: ", ioe);
        }
        log.severe("Repo size: "+repository.count());
        log.severe("  new records: "+newCount);
        log.severe("  old records: "+alreadyCount);
    }
}
