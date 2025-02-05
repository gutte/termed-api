package fi.thl.termed.web.node;

import static fi.thl.termed.util.collect.StreamUtils.toListAndClose;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

import fi.thl.termed.domain.Graph;
import fi.thl.termed.domain.GraphId;
import fi.thl.termed.domain.Node;
import fi.thl.termed.domain.NodeId;
import fi.thl.termed.domain.Type;
import fi.thl.termed.domain.TypeId;
import fi.thl.termed.domain.User;
import fi.thl.termed.service.node.util.NodeRdfGraphWrapper;
import fi.thl.termed.service.type.specification.TypesByGraphId;
import fi.thl.termed.util.jena.StreamRDFWriterUtils;
import fi.thl.termed.util.query.Query;
import fi.thl.termed.util.query.Specification;
import fi.thl.termed.util.rdf.RdfMediaTypes;
import fi.thl.termed.util.service.Service;
import fi.thl.termed.util.spring.exception.NotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graphs/{graphId}/nodes")
public class NodeRdfStreamReadController {

  @Autowired
  private Service<GraphId, Graph> graphService;

  @Autowired
  private Service<TypeId, Type> typeService;

  @Autowired
  private Service<NodeId, Node> nodeService;

  @Value("${fi.thl.termed.defaultNamespace:}")
  private String defaultNamespace;

  @GetMapping(produces = RdfMediaTypes.N_TRIPLES_VALUE)
  public void streamNTriples(
      @PathVariable(name = "graphId") UUID graphId,
      @RequestParam(name = "download", defaultValue = "false") boolean download,
      @AuthenticationPrincipal User user,
      HttpServletResponse response) throws IOException {

    Graph graph = graphService.get(GraphId.of(graphId), user)
        .orElseThrow(NotFoundException::new);

    if (download) {
      String filename = LocalDate.now().toString() + "-" + graph.getCode().orElse("nodes") + ".nt";
      response.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    }

    response.setContentType(RdfMediaTypes.N_TRIPLES_VALUE);
    response.setCharacterEncoding(UTF_8.toString());

    try (OutputStream out = response.getOutputStream()) {
      List<Type> types = toListAndClose(
          typeService.values(new Query<>(new TypesByGraphId(graphId)), user));
      Function<Specification<NodeId, Node>, Stream<Node>> nodes =
          s -> nodeService.values(new Query<>(s), user);

      StreamRDFWriterUtils.writeAndCloseIterator(out,
          new NodeRdfGraphWrapper(defaultNamespace, types, nodes).find(null, null, null),
          Lang.NTRIPLES);
    }
  }

  @GetMapping(produces = RdfMediaTypes.TURTLE_VALUE)
  public void streamTurtle(
      @PathVariable(name = "graphId") UUID graphId,
      @RequestParam(name = "download", defaultValue = "false") boolean download,
      @AuthenticationPrincipal User user,
      HttpServletResponse response) throws IOException {

    Graph graph = graphService.get(GraphId.of(graphId), user)
        .orElseThrow(NotFoundException::new);

    if (download) {
      String filename = LocalDate.now().toString() + "-" + graph.getCode().orElse("nodes") + ".ttl";
      response.setHeader(CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
    }

    response.setContentType(RdfMediaTypes.TURTLE_VALUE);
    response.setCharacterEncoding(UTF_8.toString());

    try (OutputStream out = response.getOutputStream()) {
      List<Type> types = toListAndClose(
          typeService.values(new Query<>(new TypesByGraphId(graphId)), user));
      Function<Specification<NodeId, Node>, Stream<Node>> nodes =
          s -> nodeService.values(new Query<>(s), user);

      StreamRDFWriterUtils.writeAndCloseIterator(out,
          new NodeRdfGraphWrapper(defaultNamespace, types, nodes).find(null, null, null),
          Lang.TURTLE);
    }
  }

}
