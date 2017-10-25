package fi.thl.termed.service.node.internal;

import fi.thl.termed.domain.NodeAttributeValueId;
import fi.thl.termed.domain.NodeId;
import fi.thl.termed.domain.Revision;
import fi.thl.termed.domain.RevisionId;
import fi.thl.termed.util.query.AbstractSqlSpecification;
import fi.thl.termed.util.query.ParametrizedSqlQuery;
import java.util.Objects;

public class NodeReferenceAttributeValuesLessOrEqualToNodeRevision extends
    AbstractSqlSpecification<RevisionId<NodeAttributeValueId>, Revision<NodeAttributeValueId, NodeId>> {

  private NodeId nodeId;
  private Long revision;

  NodeReferenceAttributeValuesLessOrEqualToNodeRevision(RevisionId<NodeId> revisionId) {
    this.nodeId = revisionId.getId();
    this.revision = revisionId.getRevision();
  }

  @Override
  public boolean test(RevisionId<NodeAttributeValueId> key,
      Revision<NodeAttributeValueId, NodeId> value) {
    return Objects.equals(key.getId().getNodeId(), nodeId) && key.getRevision() <= revision;
  }

  @Override
  public ParametrizedSqlQuery sql() {
    return ParametrizedSqlQuery.of(
        "node_graph_id = ? and node_type_id = ? and node_id = ? and revision <= ?",
        nodeId.getTypeGraphId(), nodeId.getTypeId(), nodeId.getId(), revision);
  }

}
