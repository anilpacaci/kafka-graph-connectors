package ca.uwaterloo.cs.ldbc.interactive.gremlin.handler;

import ca.uwaterloo.cs.ldbc.interactive.gremlin.Entity;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinDbConnectionState;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinUtils;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery11;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery11Result;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by anilpacaci on 2016-07-23.
 */
public class LdbcComplexQuery11Handler implements OperationHandler<LdbcQuery11, DbConnectionState> {
    @Override
    public void executeOperation(LdbcQuery11 ldbcQuery11, DbConnectionState dbConnectionState, ResultReporter resultReporter) throws DbException {
        Client client = ((GremlinDbConnectionState) dbConnectionState).getClient();
        Map<String, Object> params = new HashMap<>();
        params.put("person_id", GremlinUtils.makeIid(Entity.PERSON, ldbcQuery11.personId()));
        params.put("person_label", Entity.PERSON.getName());
        params.put("country_name", ldbcQuery11.countryName());
        params.put("start_year", Integer.toString(ldbcQuery11.workFromYear()));
        params.put("result_limit", ldbcQuery11.limit());

        String statement= "g.V().has(person_label, 'iid', person_id).aggregate('0')."+
        "repeat(out('knows').aggregate('fof')).times(2).cap('fof').unfold()." +
        "where(without('0')).dedup().as('friend')."+
        "outE('workAt').has('workFrom', lte(start_year)).as('workEdge')."+
        "inV().as('organization').out('isLocatedIn').has('name', country_name)."+
        "order()." +
        "by(select('friend').values('iid_long'))."+
        "by(select('workEdge').values('workFrom'))."+
        "by(select('organization').values('name'), decr)."+
        "limit(result_limit)."+
        "select('friend', 'workEdge', 'organization')";

        /*
        statement= "g.V().has('person', 'iid', 'person:19791209325795').aggregate('0')."+
                "repeat(out('knows').aggregate('fof')).times(2).cap('fof').unfold()." +
                "where(without('0')).dedup().as('friend')."+
                "outE('workAt').has('workFrom', lte('2005')).as('workEdge')."+
                "inV().as('organization').out('isLocatedIn').has('name', 'Lithuania')."+
                "limit(20)."+
                "select('friend', 'workEdge', 'organization')";
                */

       // 1st Person-worksAt->.worksFrom (ascending)
       // 2nd Person.id (ascending)
       // 3st Person-worksAt->Organization.name (descending)
        List<Result> results = null;
        try {
            results = client.submit(statement, params).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DbException("Remote execution failed", e);
        }

        List<LdbcQuery11Result> resultList = new ArrayList<>();
        for(Result r : results) {
            HashMap map = r.get(HashMap.class);
            Vertex person = (Vertex) map.get("friend");
            Vertex organization = (Vertex) map.get("organization");
            Edge workAt = (Edge) map.get("workEdge");

            LdbcQuery11Result ldbcQuery11Result = new LdbcQuery11Result(GremlinUtils.getSNBId(person),
                    person.<String>property("firstName").value(),
                    person.<String>property("lastName").value(),
                    organization.<String>property("name").value(),
                    Integer.parseInt(workAt.<String>property("workFrom").value()));

            resultList.add(ldbcQuery11Result);
        }

        resultList.sort( ( o1, o2 ) ->
        {
            if (o1.organizationWorkFromYear() == o2.organizationWorkFromYear()) {
                if (o1.personId() == o2.personId())
                    return o2.organizationName().compareTo(o1.organizationName());
                else
                    return Long.compare(o1.personId(), o2.personId());
            } else
                return Integer.compare(o1.organizationWorkFromYear(), o2.organizationWorkFromYear());
        } );

        if(resultList.size() > ldbcQuery11.limit()) {
            resultList = resultList.subList(0, ldbcQuery11.limit());
        }

        resultReporter.report(resultList.size(), resultList, ldbcQuery11);

    }
}
