package fi.thl.termed.service.node.internal;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;

import fi.thl.termed.domain.Graph;
import fi.thl.termed.domain.GraphId;
import fi.thl.termed.domain.Node;
import fi.thl.termed.domain.NodeId;
import fi.thl.termed.domain.Type;
import fi.thl.termed.domain.TypeId;
import fi.thl.termed.domain.User;
import fi.thl.termed.service.node.specification.NodeById;
import fi.thl.termed.service.node.specification.NodesByGraphId;
import fi.thl.termed.service.node.specification.NodesByTypeId;
import fi.thl.termed.util.query.AndSpecification;
import fi.thl.termed.util.service.ForwardingService;
import fi.thl.termed.util.service.SaveMode;
import fi.thl.termed.util.service.SequenceService;
import fi.thl.termed.util.service.Service;
import fi.thl.termed.util.service.WriteOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class ExtIdsInitializingNodeService extends ForwardingService<NodeId, Node> {

  private SequenceService<TypeId> nodeSequenceService;
  private BiFunction<TypeId, User, Optional<Type>> typeSource;
  private BiFunction<GraphId, User, Optional<Graph>> graphSource;

  public ExtIdsInitializingNodeService(Service<NodeId, Node> delegate,
      SequenceService<TypeId> nodeSequenceService,
      BiFunction<TypeId, User, Optional<Type>> typeSource,
      BiFunction<GraphId, User, Optional<Graph>> graphSource) {
    super(delegate);
    this.nodeSequenceService = nodeSequenceService;
    this.typeSource = typeSource;
    this.graphSource = graphSource;
  }

  @Override
  public List<NodeId> save(List<Node> nodes, SaveMode mode, WriteOptions opts, User user) {
    addSerialNumbersWithDefaultCodesAndUris(nodes, user);
    return super.save(nodes, mode, opts, user);
  }

  @Override
  public NodeId save(Node node, SaveMode mode, WriteOptions opts, User user) {
    addSerialNumbersWithDefaultCodesAndUris(singletonList(node), user);
    return super.save(node, mode, opts, user);
  }

  @Override
  public List<NodeId> deleteAndSave(List<NodeId> deletes, List<Node> saves, SaveMode mode,
      WriteOptions opts, User user) {
    addSerialNumbersWithDefaultCodesAndUris(saves, user);
    return super.deleteAndSave(deletes, saves, mode, opts, user);
  }

  private void addSerialNumbersWithDefaultCodesAndUris(List<Node> nodes, User user) {
    nodes.stream().collect(groupingBy(Node::getType)).forEach((type, instances) -> {
      List<Node> newNodes = new ArrayList<>();

      for (Node node : instances) {
        Optional<Node> old = getValues(new AndSpecification<>(
            new NodesByGraphId(node.getTypeGraphId()),
            new NodesByTypeId(node.getTypeId()),
            new NodeById(node.getId())), user).findAny();

        if (old.isPresent()) {
          node.setNumber(old.get().getNumber());
        } else {
          newNodes.add(node);
        }
      }

      if (!newNodes.isEmpty()) {
        int number = nodeSequenceService.getAndAdvance(type, newNodes.size(), user);
        for (Node node : newNodes) {
          node.setNumber(number++);
          addDefaultCodeIfMissing(node, user);
          addDefaultUriIfMissing(node, user);
        }
      }
    });
  }

  private void addDefaultCodeIfMissing(Node node, User user) {
    if (isNullOrEmpty(node.getCode())) {
      Type type = typeSource.apply(node.getType(), user)
          .orElseThrow(IllegalStateException::new);

      node.setCode(type.getNodeCodePrefix()
          .map(prefix -> prefix + node.getNumber())
          .orElseGet(
              () -> UPPER_CAMEL.to(LOWER_HYPHEN, node.getTypeId()) + "-" + node.getNumber()));
    }
  }

  private void addDefaultUriIfMissing(Node node, User user) {
    if (isNullOrEmpty(node.getUri())) {
      Graph graph = graphSource.apply(node.getTypeGraph(), user)
          .orElseThrow(IllegalStateException::new);

      graph.getUri().ifPresent(ns -> node.setUri(ns + node.getCode()));
    }
  }

}
