package ca.uwaterloo.cs.ldbc.interactive.gremlin.handler;

import ca.uwaterloo.cs.ldbc.interactive.gremlin.Entity;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinDbConnectionState;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinUtils;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery11Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery9;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery9Result;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by anilpacaci on 2016-07-23.
 */
public class LdbcComplexQuery9Handler implements OperationHandler<LdbcQuery9, DbConnectionState> {
    @Override
    public void executeOperation(LdbcQuery9 ldbcQuery9, DbConnectionState dbConnectionState, ResultReporter resultReporter) throws DbException {
        Client client = ((GremlinDbConnectionState) dbConnectionState).getClient();
        Map<String, Object> params = new HashMap<>();
        params.put("person_id", GremlinUtils.makeIid(Entity.PERSON, ldbcQuery9.personId()));
        params.put("max_date", Long.toString(ldbcQuery9.maxDate().getTime()));
        params.put("result_limit", ldbcQuery9.limit());


        List<Result> results = null;
        try {
            results = client.submit("g.V().has('iid', person_id).repeat(out('knows').simplePath()).until(loops().is(gt(1))).as('person').in('hasCreator').has('creationDate', lt(max_date)).limit(result_limit).as('message').order().by('creationDate', decr).by('iid', incr).select('person', 'message')", params).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DbException("Remote execution failed", e);
        }


        List<LdbcQuery9Result> resultList = new ArrayList<>();
        for(Result r : results) {
            HashMap map = r.get(HashMap.class);
            Vertex person = (Vertex) map.get("person");
            Vertex message = (Vertex) map.get("message");

            LdbcQuery9Result ldbcQuery9Result = new LdbcQuery9Result(GremlinUtils.getSNBId(person),
                    person.<String>property("firstName").value(),
                    person.<String>property("lastName").value(),
                    GremlinUtils.getSNBId(message),
                    message.<String>property("content").value(),
                    Long.parseLong(message.<String>property("creationDate").value()));

            resultList.add(ldbcQuery9Result);
        }
        resultReporter.report(resultList.size(), resultList, ldbcQuery9);
    }
}