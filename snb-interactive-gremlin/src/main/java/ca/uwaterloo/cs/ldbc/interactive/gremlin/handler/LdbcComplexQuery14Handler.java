package ca.uwaterloo.cs.ldbc.interactive.gremlin.handler;

import ca.uwaterloo.cs.ldbc.interactive.gremlin.Entity;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinDbConnectionState;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinUtils;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery14;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery14Result;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class LdbcComplexQuery14Handler implements OperationHandler<LdbcQuery14, DbConnectionState> {
  @Override
  public void executeOperation( LdbcQuery14 ldbcQuery14, DbConnectionState dbConnectionState, ResultReporter resultReporter ) throws DbException {

    Client client = ((GremlinDbConnectionState) dbConnectionState).getClient();
    Map<String, Object> params = new HashMap<>();
    params.put( "person1_id", GremlinUtils.makeIid( Entity.PERSON, ldbcQuery14.person1Id() ) );
    params.put( "person2_id", GremlinUtils.makeIid( Entity.PERSON, ldbcQuery14.person2Id() ) );
    params.put( "person_label", Entity.PERSON.getName() );

    String statement =
    "static double calculateWeight(GraphTraversalSource g, Long v1, Long v2) {" +
            "long postForward = g.V(v1).in('hasCreator').hasLabel('post').in('replyOf').out('hasCreator').hasId(v2).count().next(); " +
            "long postBackward = g.V(v2).in('hasCreator').hasLabel('post').in('replyOf').out('hasCreator').hasId(v1).count().next(); " +
            "long commentForward = g.V(v1).in('hasCreator').hasLabel('comment').in('replyOf').out('hasCreator').hasId(v2).count().next(); " +
            "long  commentBackward = g.V(v2).in('hasCreator').hasLabel('comment').in('replyOf').out('hasCreator').hasId(v1).count().next(); " +
            "long score = postForward + postBackward + 0.5 * (commentForward + commentBackward); return score;}; "
            +"scoreMap = [:];"
            +"shortestPathLength = g.V().has(person_label, 'iid', person1_id).repeat(out('knows').simplePath())" +
            ".until(has(person_label, 'iid', person2_id)).path().limit(1).count(local).next();"
            +"g.V().has(person_label, 'iid', person1_id).repeat(out('knows').simplePath()).until(loops().is(gte(shortestPathLength - 1))).   "
            +"filter(has(person_label, 'iid', person2_id)).path().as('shortestPaths').sideEffect{"
            +"        path = it.get(); "
            +"        totalScore = 0;"
            +"        for(int i = 0; i < path.size() - 2; i++) "
            +"            totalScore += calculateWeight(g, path.get(i).id(), path.get(i + 1).id());"
            +"        scoreMap.put(path, totalScore);   "
            +"};"
            +"scoreMap;";

    List<Result> results = null;

    try {
      results = client.submit( statement, params ).all().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new DbException( "Remote execution failed", e );
    }


    //TODO: sort results by weight, decr
    List<LdbcQuery14Result> resultList = new ArrayList<>();
    for (Result r : results) {
      HashMap map = r.get( HashMap.class );
      Path path = (Path) map.get( "path" );
      double weight = (double) map.get( "weight" );

      List<Long> idsInPath = new ArrayList<>();
      for (Object o : path) {
        Vertex v = (Vertex) o;
        idsInPath.add( GremlinUtils.getSNBId( v ) );
      }
      LdbcQuery14Result ldbcQuery14Result = new LdbcQuery14Result(
        idsInPath,
        weight );

      resultList.add( ldbcQuery14Result );
    }
    resultReporter.report( resultList.size(), resultList, ldbcQuery14 );
  }
}

