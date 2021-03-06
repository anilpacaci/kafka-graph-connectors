package ca.uwaterloo.cs.ldbc.interactive.gremlin.handler;

import ca.uwaterloo.cs.ldbc.interactive.gremlin.Entity;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinDbConnectionState;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinUtils;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPosts;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPostsResult;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by apacaci on 7/20/16.
 */
public class LdbcShortQuery2Handler implements OperationHandler<LdbcShortQuery2PersonPosts, DbConnectionState> {
    @Override
    public void executeOperation(LdbcShortQuery2PersonPosts ldbcShortQuery2PersonPosts, DbConnectionState dbConnectionState, ResultReporter resultReporter) throws DbException {
        Client client = ((GremlinDbConnectionState) dbConnectionState).getClient();
        Map<String, Object> params = new HashMap<>();
        params.put("person_id", GremlinUtils.makeIid(Entity.PERSON, ldbcShortQuery2PersonPosts.personId()));
        params.put("result_limit", ldbcShortQuery2PersonPosts.limit());
        params.put("person_label", Entity.PERSON.getName());

        String statement = "g.V().has(person_label, 'iid', person_id)" +
                ".in('hasCreator').order().by('creationDate', decr).by('iid_long', decr).limit(result_limit).as('message')" +
                ".until(hasLabel('post')).repeat(out('replyOf')).as('original')" +
                ".out('hasCreator').as('owner')" +
                ".select('message', 'original', 'owner')";

        List<Result> results = null;
        try {
            results = client.submit(statement, params).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DbException("Remote execution failed", e);
        }

        List<LdbcShortQuery2PersonPostsResult> resultList = new ArrayList<>();

        for(Result r : results) {
            HashMap resultMap = r.get(HashMap.class);
            Vertex message = (Vertex) resultMap.get("message");
            Vertex original = (Vertex) resultMap.get("original");
            Vertex owner = (Vertex) resultMap.get("owner");

            String content = message.<String>property("content").value();
            if(content == null || content.isEmpty()) {
                content = message.<String>property("imageFile").value();
            }

            LdbcShortQuery2PersonPostsResult result = new LdbcShortQuery2PersonPostsResult(GremlinUtils.getSNBId(message),
                    content,
                    message.<Long>property("creationDate").value(),
                    GremlinUtils.getSNBId(original),
                    GremlinUtils.getSNBId(owner),
                    owner.<String>property("firstName").value(),
                    owner.<String>property("lastName").value());

            resultList.add(result);
        }

        resultReporter.report(resultList.size(), resultList, ldbcShortQuery2PersonPosts);

    }
}
