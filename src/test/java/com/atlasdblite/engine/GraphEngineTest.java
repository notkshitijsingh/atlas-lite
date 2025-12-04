package com.atlasdblite.engine;

import com.atlasdblite.models.Node;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public class GraphEngineTest {

    private static final String TEST_DB_DIR = "test_atlas_db";
    private GraphEngine engine;

    @BeforeMethod
    public void setup() {
        deleteTestDir();
        engine = new GraphEngine(TEST_DB_DIR);
    }

    @AfterMethod
    public void tearDown() {
        deleteTestDir();
    }

    private void deleteTestDir() {
        try {
            if (Files.exists(Paths.get(TEST_DB_DIR))) {
                Files.walk(Paths.get(TEST_DB_DIR))
                    .sorted(Comparator.reverseOrder())
                    .map(java.nio.file.Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception ignored) {}
    }

    @Test
    public void testShardingPersists() {
        for(int i=0; i<50; i++) {
            engine.persistNode(new Node("node_"+i, "Test"));
        }
        
        GraphEngine reloaded = new GraphEngine(TEST_DB_DIR);
        Assert.assertEquals(reloaded.getAllNodes().size(), 50);
        Assert.assertNotNull(reloaded.getNode("node_0"));
        Assert.assertNotNull(reloaded.getNode("node_49"));
    }

    @Test
    public void testCrossShardTraversal() {
        engine.persistNode(new Node("src", "Source"));
        
        engine.persistNode(new Node("tgt", "Target"));
        
        engine.persistRelation("src", "tgt", "JUMP");
        
        List<Node> results = engine.traverse("src", "JUMP");
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getId(), "tgt");
    }

    @Test
    public void testCascadeDeleteAcrossShards() {
        engine.persistNode(new Node("A", "Root"));
        engine.persistNode(new Node("B", "Child")); 
        engine.persistRelation("A", "B", "PARENT_OF");
        
        engine.deleteNode("A");
        
        Assert.assertNotNull(engine.getNode("B"));
        
        Assert.assertTrue(engine.traverse("A", "PARENT_OF").isEmpty());
    }
}