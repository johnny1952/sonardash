package sonardash;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.Sets;

@Controller
public class DeltaController {

    private final SonarQubeService sonarQubeService;

    @Autowired
    public DeltaController(SonarQubeService sonarQubeService) {
        this.sonarQubeService = sonarQubeService;
    }

    @ModelAttribute("projectFilter")
    public Predicate<Project> createProjectFilter(@RequestParam(required = false) String[] key) {
        return project -> {
            final Set<String> validKeys = Optional.ofNullable(key).map(Sets::newHashSet).orElse(newHashSet());
            return validKeys.isEmpty() || validKeys.contains(project.getKey());
        };
    }

    @ModelAttribute("interval")
    public Interval createInterval(@RequestParam Optional<String> from /* 2016-01-24T00:00 */, //
                                   @RequestParam Optional<String> to /* 2016-01-29T23:59 */, //
                                   @RequestParam(defaultValue = "14") int days) {
        final DateTime toDate = to.map(DateTime::parse).orElse(DateTime.now());
        final DateTime fromDate = from.map(DateTime::parse).orElse(toDate.minusDays(days));
        return new Interval(fromDate, toDate);
    }

    @RequestMapping("/delta")
    public String delta(Model model, //
                        @ModelAttribute("projectFilter") Predicate<Project> projectFilter, //
                        @ModelAttribute("interval") Interval interval //
    ) {
        final List<ProjectMetrics> projectMetrics = sonarQubeService.getAllProjects().stream() //
                .filter(projectFilter) //
                .map(ProjectMetrics::new) //
                .map(projectMetric -> {
                    final String key = projectMetric.getKey();
                    projectMetric.setValues(sonarQubeService.getResource(key));
                    projectMetric.setDeltas(sonarQubeService.getDeltas(key, interval));
                    return projectMetric;
                }) //
                .collect(toList());

        model.addAttribute("projectMetrics", projectMetrics);
        return "delta";
    }
}