package me.callsen.taylor.osm2graph_neo4j.data;

import java.io.File;

import org.json.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class GraphDb {

	private enum NodeLabels implements Label { INTERSECTION; } 
	private enum RelationshipTypes implements RelationshipType { CONNECTS; }
	
	public GraphDatabaseService db;
	
	public GraphDb(String graphDbPath) throws Exception {
    db = new GraphDatabaseFactory().newEmbeddedDatabase( new File( graphDbPath ) );
    System.out.println("Graph DB @ " + graphDbPath + " initialized");
  }

  public void createNode(JSONObject nodeJsonObject) {
		
		Transaction tx = null;
		try {
		
			tx = this.db.beginTx();	
			Node newIntersectionNode = this.db.createNode(NodeLabels.INTERSECTION);
			
			//apply properties to Node object
			for (String key : nodeJsonObject.keySet()) {
        newIntersectionNode.setProperty(key, nodeJsonObject.get(key));
			}
			
			tx.success();
			// System.out.println("created intersection for node id " + nodeJsonObject.getLong("osm_id"));
		} catch (Exception e) { 
			System.out.println("FAILED to create intersection for node id " + nodeJsonObject.getInt("osm_id")); System.out.println(e.toString());
		} finally {
			tx.close();
		}
		
  }

  public void createRelationship(JSONObject wayJsonObject, long wayStartOsmId, long wayEndOsmId) {
	
		Transaction tx = null;
		try {
			
			tx = this.db.beginTx();
			
			//retrieve start and stop nodes by osm_id (osm_Id within graph); create relationship between nodes that will correspond to road / way
			Node startNode = this.db.findNode( NodeLabels.INTERSECTION , "osm_id", wayStartOsmId );
			Node endNode = this.db.findNode( NodeLabels.INTERSECTION , "osm_id", wayEndOsmId );
			Relationship newRelationship = startNode.createRelationshipTo( endNode , RelationshipTypes.CONNECTS );
			
			//apply properties to newly created Relationship (representing a road / way)
			for (String key : wayJsonObject.keySet()) {
				newRelationship.setProperty(key, wayJsonObject.get(key));
			}
			
			tx.success();
			
			// System.out.println("creating road relationship in Graph for node osm_ids " + wayStartOsmId + " and "+ wayEndOsmId + "; road relationship graph id " + newRelationship.getId() );
			
		} catch (Exception e) {
			System.out.println("FAILED to create road relationship in Graph for node osm_ids " + wayStartOsmId + " and "+ wayEndOsmId + "; road relationship id road id not available" ); 
			e.printStackTrace();
		} finally {
			tx.close();
		}
		
	}

  public void shutdown(){
    this.db.shutdown();
    System.out.println("Graph DB shutdown");
  }

  public void truncateGraphNodes() {
		
		System.out.println("truncating graph nodes..");
		
		Transaction tx = null;
		try {
			
			tx = this.db.beginTx();
			
			this.db.execute( "MATCH (n) DETACH DELETE n" );

			tx.success();
			
		} catch (Exception e) {
			System.out.println("failed to truncateGraph nodes"); 
			e.printStackTrace();
		} finally {
			tx.close();
		}
		
	}
	
	public void truncateGraphRelationships() {
		
		System.out.println("truncating graph relationships..");
		
		Transaction tx = null;
		try {
			
			tx = this.db.beginTx();
			
			this.db.execute( "MATCH (a)-[r]-(b) DETACH DELETE r" );

			tx.success();
			
		} catch (Exception e) {
			System.out.println("failed to truncateGraph relationships"); 
			e.printStackTrace();
		} finally {
			tx.close();
		}
		
  }
  
  public void createNodeIdOsmIndex() {
		
		System.out.println("creating node index for quick retrieval with osm_id");
		
		//create node to create index off of
		Node indexNode = null;
		Transaction tx = null;
		try {
			
			tx = this.db.beginTx();
			
			indexNode = this.db.createNode(NodeLabels.INTERSECTION);

			tx.success();
			
		} catch (Exception e) {
			System.out.println("failed to create index node!"); 
			e.printStackTrace();
		} finally {
			tx.close();
		}
		
		//create index
		Transaction txB = null;
		try {
			
			txB = this.db.beginTx();
			
			this.db.execute( "CREATE INDEX ON :INTERSECTION(osm_id)" );

			txB.success();
			
		} catch (Exception e) {
			System.out.println("failed to create index node!"); 
			e.printStackTrace();
		} finally {
			txB.close();
		}
		
		//delete node that index was created with
		Transaction txC = null;
		try {
			
			txC = this.db.beginTx();
			
			indexNode.delete();

			txC.success();
			
		} catch (Exception e) {
			System.out.println("failed to create index node!"); 
			e.printStackTrace();
		} finally {
			txC.close();
		}
		
  }
  
  public void dropNodeOsmIdIndex() {
		
		System.out.println("dropping node index for quick retrieval with osm_Id");
		
		//drop index if exists
		Transaction txdrop = null;
		try {
			
			txdrop = this.db.beginTx();
			
			this.db.execute( "DROP INDEX ON :INTERSECTION(osm_id)" );

			txdrop.success();
			
		} catch (Exception e) {
			System.out.println("warning - failed to drop osm_Id index; index may not exist (not necessarily an issue"); 
		} finally {
			txdrop.close();
		}
		
	}

}