package ca.uwaterloo.cs.ldbc.interactive.gremlin;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by apacaci on 7/14/16.
 */
public class GremlinUtils {

    /*
     * Returns the original LDBC SNB assigned 64-bit ID of the given vertex (this
     * is not the ID that is assigned to the vertex by TitanDB during the data
     * loading phase).
     */
    public static Long getSNBId(Vertex v) {
        return Long.decode(v.<String>property("iid").value().split(":")[1]);
    }

    /*
     * Return a String representing the globally unique Iid property on all
     * vertices in the graph. This Iid property is a function of both the Entity
     * type and the 64-bit LDBC SNB assigned ID to the node (which is only unique
     * across vertices of that type).
     */
    public static String makeIid(Entity type, long id) {
        return type.getName() + ":" + String.valueOf(id);
    }

    /*
     * Return String representing the globally unique Iid property on all
     * vertices in the graph. This Iid property is a function of both the Entity
     * type and the 64-bit LDBC SNB assigned ID to the node (which is only unique
     * across vertices of that type).
     */
    public static List<String> makeIid(Entity type, List<Long> ids) {
        String name = type.getName();
        List<String> stringIds = new ArrayList<>(ids.size());

        ids.forEach(id -> {
            stringIds.add(name + ":" + String.valueOf(id));
        });

        return stringIds;
    }
}
