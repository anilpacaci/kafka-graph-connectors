package ca.uwaterloo.cs.ldbc.interactive.gremlin;

import ca.uwaterloo.cs.ldbc.interactive.gremlin.handler.LdbcShortyQuery3Handler;
import com.ldbc.driver.Db;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.control.LoggingService;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery3PersonFriends;

import java.io.IOException;
import java.util.Map;

/**
 * An implementation for LDBC SNB Interactive Benchmark.
 * Queries implemented in Gremlin traversal language and issued against a Gremlin Server
 *
 *
 * @author apacaci
 */
public class GremlinDb extends Db{
    private GremlinDbConnectionState connection;

    @Override
    protected void onInit(Map<String, String> map, LoggingService loggingService) throws DbException {

        connection = new GremlinDbConnectionState(map);

        // Complex Queries

        // Short Queries
        registerOperationHandler(LdbcShortQuery3PersonFriends.class, LdbcShortyQuery3Handler.class);

    }

    @Override
    protected void onClose() throws IOException {
        connection.close();
    }

    @Override
    protected DbConnectionState getConnectionState() throws DbException {
        return connection;
    }
}
