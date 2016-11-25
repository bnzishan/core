package org.hobbit.storage.queries;

import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.hobbit.utils.test.ModelComparisonHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Abstract test class that loads a named graph that serves as storage content
 * and an RDF model that contains the expected result, applies a given query
 * (using the abstract {@link #executeQuery(String, Dataset)} method) to the
 * storage content and compares the result with the expected result. The name of
 * the graph containing the graph loaded from a given resource is defined by
 * {@link #FIRST_GRAPH_NAME}. The storage contains a second, empty graph with a
 * name defined by {@link #SECOND_GRAPH_NAME}.
 * 
 * If one of the given resource names is null, the named graph in the storage or
 * the result model will be empty.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractQueryTest {

    protected static final String FIRST_GRAPH_NAME = "http://example.org/graph1";
    protected static final String SECOND_GRAPH_NAME = "http://example.org/graph2";

    /**
     * Name of the resource from which the store content is loaded.
     */
    private String storeContentResource;
    /**
     * Name of the resource from which the expected result is loaded.
     */
    private String expectedResultResource;
    /**
     * SPARQL query that is executed on the store content to create the expected
     * result model.
     */
    private String query;

    public AbstractQueryTest(String storeContentResource, String query, String expectedResultResource) {
        super();
        this.storeContentResource = storeContentResource;
        this.query = query;
        this.expectedResultResource = expectedResultResource;
    }

    @Test
    public void test() {
        // Make sure the query has been loaded correctly
        Assert.assertNotNull(query);
        // load the models
        Dataset storeContent = DatasetFactory.createTxnMem();
        // If the named graph is not empty, load it
        if (storeContentResource != null) {
            storeContent.addNamedModel(FIRST_GRAPH_NAME, loadModel(storeContentResource));
        } else {
            storeContent.addNamedModel(FIRST_GRAPH_NAME, ModelFactory.createDefaultModel());
        }
        storeContent.addNamedModel(SECOND_GRAPH_NAME, ModelFactory.createDefaultModel());
        // load/create expected result
        Model expectedResult = null;
        if (expectedResultResource != null) {
            expectedResult = loadModel(expectedResultResource);
        } else {
            // an empty result is expected
            expectedResult = ModelFactory.createDefaultModel();
        }

        // execute query
        Model result = executeQuery(query, storeContent);

        // Compare the models
        String expectedModelString = expectedResult.toString();
        String resultModelString = result.toString();
        // Check the recall
        Set<Statement> statements = ModelComparisonHelper.getMissingStatements(expectedResult, result);
        Assert.assertTrue("The result does not contain the expected statements " + statements.toString()
                + ". expected model:\n" + expectedModelString + "\nresult model:\n" + resultModelString,
                statements.size() == 0);
        // Check the precision
        statements = ModelComparisonHelper.getMissingStatements(result, expectedResult);
        Assert.assertTrue("The result contains the unexpected statements " + statements.toString()
                + ". expected model:\n" + expectedModelString + "\nresult model:\n" + resultModelString,
                statements.size() == 0);
    }

    protected abstract Model executeQuery(String query, Dataset storeContent);

    protected static Model loadModel(String resourceName) {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = AbstractQueryTest.class.getClassLoader().getResourceAsStream(resourceName);
        Assert.assertNotNull(is);
        try {
            RDFDataMgr.read(model, is, Lang.TTL);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return model;
    }

}
