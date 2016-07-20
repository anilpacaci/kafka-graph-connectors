package ca.uwaterloo.cs.ldbc.interactive.gremlin.handler;

import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinDbConnectionState;
import ca.uwaterloo.cs.ldbc.interactive.gremlin.GremlinUtils;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery1PersonProfile;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery1PersonProfileResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.db.DummyLdbcSnbInteractiveDb;
import net.ellitron.ldbcsnbimpls.interactive.core.Entity;
import net.ellitron.ldbcsnbimpls.interactive.titan.TitanDbConnectionState;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by apacaci on 7/14/16.
 */
public class LdbcShortyQuery1Handler implements OperationHandler<LdbcShortQuery1PersonProfile, DbConnectionState> {
    @Override
    public void executeOperation(LdbcShortQuery1PersonProfile ldbcShortQuery1PersonProfile, DbConnectionState dbConnectionState, ResultReporter resultReporter) throws DbException {
        Client client = ((GremlinDbConnectionState) dbConnectionState).getClient();
        Map<String, Object> params = new HashMap<>();
        params.put("person_id", ldbcShortQuery1PersonProfile.personId());

        List<Result> results = null;
        try {
            results = client.submit("g.V().has('iid', person_id).as('person').outE('isLocatedIn').inV().as('place').select('person', 'place')", params).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DbException("Remote execution failed", e);
        }

        Result r = results.get(0); // ShortQuery1 should return a single result

        HashMap map = r.get(HashMap.class);
        Vertex person = (Vertex) r.get(HashMap.class).get("person");
        Vertex place = (Vertex) r.get(HashMap.class).get("place");

        Iterator<VertexProperty<String>> props = person.properties();
        Map<String, String> propertyMap = new HashMap<>();
        props.forEachRemaining((prop) -> {
            propertyMap.put(prop.key(), prop.value());
        });

        long placeId = GremlinUtils.getSNBId(place);

        LdbcShortQuery1PersonProfileResult res =
                new LdbcShortQuery1PersonProfileResult(
                        propertyMap.get("firstName"),
                        propertyMap.get("lastName"),
                        Long.parseLong(propertyMap.get("birthday")),
                        propertyMap.get("locationIP"),
                        propertyMap.get("browserUsed"),
                        placeId,
                        propertyMap.get("gender"),
                        Long.parseLong(propertyMap.get("creationDate")));

        resultReporter.report(0, res, ldbcShortQuery1PersonProfile);
    }
}
