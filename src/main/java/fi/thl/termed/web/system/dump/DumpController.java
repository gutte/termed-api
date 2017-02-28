package fi.thl.termed.web.system.dump;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import fi.thl.termed.domain.Dump;
import fi.thl.termed.domain.User;
import fi.thl.termed.service.dump.internal.DumpService;
import fi.thl.termed.util.spring.annotation.GetJsonMapping;
import fi.thl.termed.util.spring.annotation.PostJsonMapping;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dump")
public class DumpController {

  @Autowired
  private DumpService dumpService;

  @GetJsonMapping
  public Dump dump(@AuthenticationPrincipal User user) {
    return dumpService.dump(user);
  }

  @GetJsonMapping(params = "graphId")
  public Dump dump(@RequestParam("graphId") List<UUID> ids, @AuthenticationPrincipal User user) {
    return dumpService.dump(ids, user);
  }

  @PostJsonMapping(produces = {})
  @ResponseStatus(NO_CONTENT)
  public void restore(@RequestBody Dump dump, @AuthenticationPrincipal User user) {
    dumpService.restore(dump, user);
  }

}
