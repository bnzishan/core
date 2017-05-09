/**
 * This file is part of core.
 *
 * core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with core.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hobbit.storage.queries;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.modify.request.QuadAcc;
import org.apache.jena.sparql.modify.request.UpdateDeleteInsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides static SPRAQL queries that are loaded from predefined
 * resources.
 *
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class SparqlQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlQueries.class);

    private static final String CHALLENGE_PLACEHOLDER = "%CHALLENGE_URI%";
    private static final String CHALLENGE_TASK_PLACEHOLDER = "%CHALLENGE_TASK_URI%";
    private static final String EXPERIMENT_PLACEHOLDER = "%EXPERIMENT_URI%";
    private static final String GRAPH_PLACEHOLDER = "%GRAPH_URI%";
    private static final String SYSTEM_PLACEHOLDER = "%SYSTEM_URI%";
    private static final int DEFAULT_MAX_UPDATE_QUERY_TRIPLES = 500;

    /**
     * Loads the given resource, e.g., a SPARQL query, as String.
     *
     * @param resourceName
     *            name of the resource that should be loaded
     * @return the resource as String or <code>null</code> if an error occurs
     */
    private static final String loadQuery(String resourceName) {
        InputStream is = SparqlQueries.class.getClassLoader().getResourceAsStream(resourceName);
        if (is != null) {
            try {
                return IOUtils.toString(is);
            } catch (IOException e) {
                LOGGER.error("Couldn't read query from resource \"" + resourceName + "\". Returning null.");
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            LOGGER.error("Couldn't find needed resource \"" + resourceName + "\". Returning null.");
        }
        return null;
    }

    /**
     * A construct query that retrieves the graph of a challenge.
     */
    private static final String GET_CHALLENGE_GRAPH_QUERY = loadQuery("org/hobbit/storage/queries/getChallenge.query");

    /**
     * Returns a SPARQL query for retrieving the graph of a challenge.
     *
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getChallengeGraphQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(GET_CHALLENGE_GRAPH_QUERY, new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { challengeUri, graphUri });
    }

    /**
     * A construct query that retrieves a shallow of a challenge.
     */
    private static final String GET_SHALLOW_CHALLENGE_GRAPH_QUERY = loadQuery(
            "org/hobbit/storage/queries/getShallowChallenge.query");

    /**
     * Returns a SPARQL query for retrieving a shallow graph of a challenge.
     *
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getShallowChallengeGraphQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(GET_SHALLOW_CHALLENGE_GRAPH_QUERY,
                new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { challengeUri, graphUri });
    }

    /**
     * A construct query that retrieves the tasks of a challenge.
     */
    private static final String GET_CHALLENGE_TASKS_QUERY = loadQuery(
            "org/hobbit/storage/queries/getChallengeTasks.query");

    /**
     * Returns a SPARQL query for retrieving the tasks of a challenge.
     *
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getChallengeTasksQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(GET_CHALLENGE_TASKS_QUERY, new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { challengeUri, graphUri });
    }

    /**
     * A construct query that retrieves the graph of a challenge.
     */
    private static final String GET_CHALLENGE_PUBLISH_INFO_QUERY = loadQuery(
            "org/hobbit/storage/queries/getChallengePublishInfo.query");

    /**
     * Returns a SPARQL query for retrieving a graph comprising the task URIs
     * and the publication date of a challenge if this challenge is closed.
     *
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getChallengePublishInfoQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(GET_CHALLENGE_PUBLISH_INFO_QUERY,
                new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { challengeUri, graphUri });
    }

    /**
     * An update query that closes a challenge.
     */
    private static final String CLOSE_CHALLENGE_UPDATE_QUERY = loadQuery(
            "org/hobbit/storage/queries/closeChallenge.query");

    /**
     * Returns a SPARQL query for adding the closed triple to the challenge with
     * the given URI.
     *
     * @param challengeUri
     *            URI of the challenge that should be closed. <code>null</code>
     *            works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL update query that performs the insertion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getCloseChallengeQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(CLOSE_CHALLENGE_UPDATE_QUERY,
                new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { challengeUri, graphUri });
    }

    /**
     * A construct query that selects the graph of an experiment.
     */
    private static final String GET_EXPERIMENT_QUERY = loadQuery("org/hobbit/storage/queries/getExperiment.query");

    /**
     * Returns a SPARQL query for retrieving the graph of an experiment.
     *
     * @param experimentUri
     *            URI of the experiment that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getExperimentGraphQuery(String experimentUri, String graphUri) {
        return replacePlaceholders(GET_EXPERIMENT_QUERY, new String[] { EXPERIMENT_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { experimentUri, graphUri });
    }

    /**
     * A construct query that selects a shallow graph of an experiment.
     */
    private static final String GET_SHALLOW_EXPERIMENT_QUERY = loadQuery(
            "org/hobbit/storage/queries/getShallowExperiment.query");

    /**
     * Returns a SPARQL query for retrieving a shallow graph of an experiment
     * containing the links to the system instance, the benchmark and the
     * challenge task and the labels of them.
     *
     * @param experimentUri
     *            URI of the experiment that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getShallowExperimentGraphQuery(String experimentUri, String graphUri) {
        return replacePlaceholders(GET_SHALLOW_EXPERIMENT_QUERY,
                new String[] { EXPERIMENT_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { experimentUri, graphUri });
    }

    /**
     * Returns a SPARQL query for retrieving the graphs of experiments that
     * involve one of the given systems.
     *
     * @param systemUris
     *            URIs of the systems that might be involved in the experiment.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getExperimentGraphOfSystemsQuery(List<String> systemUris, String graphUri) {
        if ((systemUris == null) || (systemUris.size() == 0)) {
            return null;
        }
        // Replace the system triple in the normal select query by a set of
        // possible systems
        StringBuilder queryBuilder = new StringBuilder();
        int pos = GET_EXPERIMENT_QUERY.indexOf("WHERE");
        if (pos < 0) {
            return null;
        }
        // Add everything before the WHERE
        queryBuilder.append(GET_EXPERIMENT_QUERY.subSequence(0, pos));
        int oldpos = pos;
        // For every selection triple, insert the list of systems in front of it
        final String EXPERIMENT_SELECTION = "%EXPERIMENT_URI% a hobbit:Experiment .";
        pos = GET_EXPERIMENT_QUERY.indexOf(EXPERIMENT_SELECTION, oldpos);
        while (pos > 0) {
            queryBuilder.append(GET_EXPERIMENT_QUERY.substring(oldpos, pos));
            queryBuilder.append('{');
            boolean first = true;
            for (String systemUri : systemUris) {
                if (first) {
                    first = false;
                } else {
                    queryBuilder.append("} UNION {");
                }
                queryBuilder.append("%EXPERIMENT_URI% hobbit:involvesSystemInstance <");
                queryBuilder.append(systemUri);
                queryBuilder.append('>');
            }
            queryBuilder.append("} . \n");
            oldpos = pos;
            pos = GET_EXPERIMENT_QUERY.indexOf(EXPERIMENT_SELECTION, oldpos + EXPERIMENT_SELECTION.length());
        }
        queryBuilder.append(GET_EXPERIMENT_QUERY.substring(oldpos));
        return replacePlaceholders(queryBuilder.toString(), new String[] { EXPERIMENT_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { null, graphUri });
    }

    /**
     * A construct query that selects the graph of an experiment that is part of
     * a given challenge task.
     */
    private static final String GET_EXPERIMENT_OF_TASK_QUERY = loadQuery(
            "org/hobbit/storage/queries/getExperimentOfTask.query");

    /**
     * Returns a SPARQL query for retrieving the graph of an experiment that is
     * part of the given challenge task.
     *
     * @param experimentUri
     *            URI of the experiment that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param challengeTaskUri
     *            URI of the challenge task from which the experiment should be
     *            created. <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String getExperimentOfTaskQuery(String experimentUri, String challengeTaskUri,
            String graphUri) {
        return replacePlaceholders(GET_EXPERIMENT_OF_TASK_QUERY,
                new String[] { EXPERIMENT_PLACEHOLDER, CHALLENGE_TASK_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { experimentUri, challengeTaskUri, graphUri });
    }

    /**
     * A construct query that selects the subgraph of a challenge task that
     * defines an experiment of this task.
     */
    private static final String CREATE_EXPERIMENT_FROM_CHALLENGE_TASK = loadQuery(
            "org/hobbit/storage/queries/createExperimentFromTask.query");

    /**
     * Returns a SPARQL query for creating a subgraph of a challenge task that
     * defines an experiment of this task.
     *
     * @param experimentUri
     *            URI of the newly created experiment.
     * @param challengeTaskUri
     *            URI of the challenge task from which the experiment should be
     *            created. <code>null</code> works like a wildcard.
     * @param systemUri
     *            URI of the system of the experiment. <code>null</code> works
     *            like a wildcard.
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL construct query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     * @throws IllegalArgumentException
     *             if the given experimentUri is null
     */
    public static final String getCreateExperimentFromTaskQuery(String experimentUri, String challengeTaskUri,
            String systemUri, String graphUri) {
        if (experimentUri == null) {
            throw new IllegalArgumentException("The given experiment URI must not be null.");
        }
        return replacePlaceholders(
                CREATE_EXPERIMENT_FROM_CHALLENGE_TASK, new String[] { EXPERIMENT_PLACEHOLDER,
                        CHALLENGE_TASK_PLACEHOLDER, SYSTEM_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { experimentUri, challengeTaskUri, systemUri, graphUri });
    }

    /**
     * An update query that deletes the graph of an experiment.
     */
    private static final String DELETE_EXPERIMENT_GRAPH_QUERY = loadQuery(
            "org/hobbit/storage/queries/deleteExperiment.query");

    /**
     * Returns a SPARQL update query for deleting the graph of an experiment.
     *
     * @param experimentUri
     *            URI of the experiment that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored.
     * @return the SPARQL update query that performs the deletion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String deleteExperimentGraphQuery(String experimentUri, String graphUri) {
        return replacePlaceholders(DELETE_EXPERIMENT_GRAPH_QUERY,
                new String[] { EXPERIMENT_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { experimentUri, graphUri });
    }

    /**
     * An update query that deletes the graph of a challenge.
     */
    private static final String DELETE_CHALLENGE_GRAPH_QUERY = loadQuery(
            "org/hobbit/storage/queries/deleteChallenge.query");

    /**
     * Returns a SPARQL update query for deleting the graph of a challenge.
     *
     * @param challengeUri
     *            URI of the challenge that should be retrieved.
     *            <code>null</code> works like a wildcard.
     * @param graphUri
     *            URI of the graph the challenge is stored.
     * @return the SPARQL update query that performs the deletion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String deleteChallengeGraphQuery(String challengeUri, String graphUri) {
        return replacePlaceholders(DELETE_CHALLENGE_GRAPH_QUERY,
                new String[] { CHALLENGE_PLACEHOLDER, GRAPH_PLACEHOLDER }, new String[] { challengeUri, graphUri });
    }

    /**
     * An update query that cleans up the graph of containing challenges.
     */
    @Deprecated
    private static final String CLEAN_UP_CHALLENGE_GRAPH_QUERY = loadQuery(
            "org/hobbit/storage/queries/cleanUpChallengeGraph.query");

    /**
     * Returns a SPARQL update query for cleaning up the graph of a challenge
     * configs.
     *
     * @param graphUri
     *            URI of the graph the challenge is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL update query that performs the deletion or
     *         <code>null</code> if the query hasn't been loaded correctly
     * @deprecated A single large query does not seem to be supported by all
     *             triple stores. Use
     *             {@link #cleanUpChallengeGraphQueries(String)} instead.
     */
    @Deprecated
    public static final String cleanUpChallengeGraphQuery(String graphUri) {
        return replacePlaceholders(CLEAN_UP_CHALLENGE_GRAPH_QUERY, new String[] { GRAPH_PLACEHOLDER },
                new String[] { graphUri });
    }

    /**
     * An update query that cleans up the graph of containing challenges.
     */
    private static final String CLEAN_UP_CHALLENGE_GRAPH_QUERIES[] = new String[] {
            loadQuery("org/hobbit/storage/queries/cleanUpChallengeGraph_Benchmark.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpChallengeGraph_SystemInstance.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpChallengeGraph_System.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpChallengeGraph_API.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpChallengeGraph_KPI.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpChallengeGraph_Parameter.query") };

    /**
     * Returns a SPARQL update query for cleaning up the graph of a challenge
     * configs.
     *
     * @param graphUri
     *            URI of the challenge configuration graph.
     * @return the SPARQL update query that performs the deletion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String[] cleanUpChallengeGraphQueries(String graphUri) {
        String queries[] = new String[CLEAN_UP_CHALLENGE_GRAPH_QUERIES.length];
        for (int i = 0; i < queries.length; ++i) {
            queries[i] = replacePlaceholders(CLEAN_UP_CHALLENGE_GRAPH_QUERIES[i], new String[] { GRAPH_PLACEHOLDER },
                    new String[] { graphUri });
        }
        return queries;
    }

    /**
     * An update query that cleans up the graph of containing challenges.
     */
    private static final String CLEAN_UP_PRIVATE_GRAPH_QUERIES[] = new String[] {
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_Benchmark.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_SystemInstance.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_System.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_Hardware.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_API.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_KPI.query"),
            loadQuery("org/hobbit/storage/queries/cleanUpPrivateGraph_Parameter.query") };

    /**
     * Returns a SPARQL update query for cleaning up the private result graph.
     *
     * @param graphUri
     *            URI of the private result graph.
     * @return the SPARQL update query that performs the deletion or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String[] cleanUpPrivateGraphQueries(String graphUri) {
        String queries[] = new String[CLEAN_UP_PRIVATE_GRAPH_QUERIES.length];
        for (int i = 0; i < queries.length; ++i) {
            queries[i] = replacePlaceholders(CLEAN_UP_PRIVATE_GRAPH_QUERIES[i], new String[] { GRAPH_PLACEHOLDER },
                    new String[] { graphUri });
        }
        return queries;
    }

    /**
     * A select query for counting the number of experiments of a challenge
     * task.
     */
    private static final String COUNT_EXP_OF_TASK_QUERY = loadQuery(
            "org/hobbit/storage/queries/selectChallengeTaskExpCount.query");

    /**
     * Returns a SPARQL query for counting the number of experiments of a
     * challenge task.
     *
     * @param challengeTaskUri
     *            URI of the challenge task
     * @param graphUri
     *            URI of the graph the experiment is stored. <code>null</code>
     *            works like a wildcard.
     * @return the SPARQL select query that performs the retrieving or
     *         <code>null</code> if the query hasn't been loaded correctly
     */
    public static final String countExperimentsOfTaskQuery(String challengeTaskUri, String graphUri) {
        return replacePlaceholders(COUNT_EXP_OF_TASK_QUERY,
                new String[] { CHALLENGE_TASK_PLACEHOLDER, GRAPH_PLACEHOLDER },
                new String[] { challengeTaskUri, graphUri });
    }

    /**
     * Replaces the given placeholders in the given query with the given
     * replacements. If a replacement is <code>null</code>, it is replaced by a
     * variable.
     *
     * @param query
     *            the query containing placeholders
     * @param placeholders
     *            the placeholders that should be replaced
     * @param replacements
     *            the replacements that should be used to replace the
     *            placeholders.
     * @return the newly created query or <code>null</code> if the given query
     *         was <code>null</code>.
     */
    private static final String replacePlaceholders(String query, String[] placeholders, String[] replacements) {
        if (query == null) {
            return null;
        }
        if (placeholders.length != replacements.length) {
            throw new IllegalArgumentException("The length of the placeholders != length of replacements.");
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < replacements.length; ++i) {
            if (replacements[i] == null) {
                // create a variable name
                builder.append("?v");
                builder.append(i);
            } else {
                // create <URI>
                builder.append('<');
                builder.append(replacements[i]);
                builder.append('>');
            }
            replacements[i] = builder.toString();
            builder.delete(0, builder.length());
        }
        return StringUtils.replaceEachRepeatedly(query, placeholders, replacements);
    }

    /**
     * Generates a SPARQL UPDATE query based on the differences between the two
     * given models. Triples that are present in the original model but not in
     * the updated model will be put into the DELETE part of the query. Triples
     * that are present in the updated model but can not be found in the
     * original model will be put into the INSERT part of the query.
     *
     * <p>
     * <b>Note</b> that some stores might have a maximum number of triples that
     * can be processed with a single query. In these cases
     * {@link #getUpdateQueriesFromDiff(Model, Model, String, int)} should be
     * used.
     * </p>
     *
     * @param original
     *            the original RDF model
     * @param updated
     *            the updated RDF model
     * @param graphUri
     *            the URI of the graph to which the UPDATE query should be
     *            applied or <code>null</code>
     * @return The SPARQL UPDATE query
     */
    public static final String getUpdateQueryFromDiff(Model original, Model updated, String graphUri) {
        return getUpdateQueryFromStatements(original.difference(updated).listStatements().toList(),
                updated.difference(original).listStatements().toList(), original, graphUri);
    }

    /**
     * Generates a SPARQL UPDATE query based on the given list of statements
     * that should be deleted and that should be added in the graph with the
     * given URI.
     *
     * @param deleted
     *            statements that should be deleted from the graph
     * @param inserted
     *            statements that should be added to the graph
     * @param mapping
     *            A prefix mapping used for the query
     * @param graphUri
     *            the URI of the graph which should be updated with the
     *            generated query
     * @return the update query
     */
    public static final String getUpdateQueryFromStatements(List<Statement> deleted, List<Statement> inserted,
            PrefixMapping mapping, String graphUri) {
        UpdateDeleteInsert update = new UpdateDeleteInsert();
        Node graph = null;
        if (graphUri != null) {
            graph = NodeFactory.createURI(graphUri);
            update.setWithIRI(graph);
        }
        Iterator<Statement> iterator;

        // deleted statements
        iterator = deleted.iterator();
        QuadAcc quads = update.getDeleteAcc();
        while (iterator.hasNext()) {
            quads.addTriple(iterator.next().asTriple());
        }

        // inserted statements
        iterator = inserted.iterator();
        quads = update.getInsertAcc();
        while (iterator.hasNext()) {
            quads.addTriple(iterator.next().asTriple());
        }

        return update.toString(mapping);
    }

    /**
     * Generates one or several SPARQL UPDATE queries based on the differences
     * between the two given models. Triples that are present in the original
     * model but not in the updated model will be put into the DELETE part of
     * the query. Triples that are present in the updated model but can not be
     * found in the original model will be put into the INSERT part of the
     * query. The changes might be carried out using multiple queries if a
     * single query could hit a maximum number of triples.
     *
     * @param original
     *            the original RDF model
     * @param updated
     *            the updated RDF model
     * @param graphUri
     *            the URI of the graph to which the UPDATE query should be
     *            applied or <code>null</code>
     * @return The SPARQL UPDATE query
     */
    public static final String[] getUpdateQueriesFromDiff(Model original, Model updated, String graphUri) {
        return getUpdateQueriesFromDiff(original, updated, graphUri, DEFAULT_MAX_UPDATE_QUERY_TRIPLES);
    }

    /**
     * Generates one or several SPARQL UPDATE queries based on the differences
     * between the two given models. Triples that are present in the original
     * model but not in the updated model will be put into the DELETE part of
     * the query. Triples that are present in the updated model but can not be
     * found in the original model will be put into the INSERT part of the
     * query. The changes will be carried out using multiple queries if a single
     * query would hit the given maximum number of triples per query.
     *
     * @param original
     *            the original RDF model
     * @param updated
     *            the updated RDF model
     * @param graphUri
     *            the URI of the graph to which the UPDATE query should be
     *            applied or <code>null</code>
     * @return The SPARQL UPDATE query
     */
    public static final String[] getUpdateQueriesFromDiff(Model original, Model updated, String graphUri,
            int maxTriplesPerQuery) {
        List<Statement> deleted = original.difference(updated).listStatements().toList();
        List<Statement> inserted = updated.difference(original).listStatements().toList();

        int numberOfDelStmts = deleted.size();
        int totalSize = Math.toIntExact(numberOfDelStmts + inserted.size());
        int queries = (totalSize / maxTriplesPerQuery) + 1;
        String[] results = new String[queries];
        int startIndex = 0;
        int endIndex = Math.min(maxTriplesPerQuery, totalSize);
        List<Statement> delStatements, addStatements;
        List<Statement> emptyList = new ArrayList<>(0);
        for (int i = 0; i < queries; i++) {
            // If we can fill the next query with deleted statements
            if (endIndex < numberOfDelStmts) {
                delStatements = deleted.subList(startIndex, endIndex);
                addStatements = emptyList;
            } else {
                if (startIndex < numberOfDelStmts) {
                    delStatements = deleted.subList(startIndex, numberOfDelStmts);
                    addStatements = inserted.subList(0, endIndex - numberOfDelStmts);
                } else {
                    delStatements = emptyList;
                    addStatements = inserted.subList(startIndex - numberOfDelStmts, endIndex - numberOfDelStmts);
                }
            }
            String query = getUpdateQueryFromStatements(delStatements, addStatements, original, graphUri);
            results[i] = query;
            // get the indexes of the next query
            startIndex = endIndex;
            endIndex = Math.min(endIndex + maxTriplesPerQuery, totalSize);
        }

        return results;
    }
}
