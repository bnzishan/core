package org.hobbit.core.components.stream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.concurrent.Semaphore;

import org.apache.commons.io.IOUtils;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractPlatformConnectorComponent;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.rabbit.DataSender;
import org.hobbit.core.rabbit.DataSenderImpl;
import org.hobbit.core.rabbit.IncomingStreamHandler;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.EnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.QueueingConsumer;

/**
 * This abstract class implements basic functions that can be used to implement
 * a task generator.
 * 
 * The following environment variables are expected:
 * <ul>
 * <li>{@link Constants#GENERATOR_ID_KEY}</li>
 * <li>{@link Constants#GENERATOR_COUNT_KEY}</li>
 * </ul>
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public abstract class AbstractStreamingTaskGenerator extends AbstractPlatformConnectorComponent
        implements StreamingGeneratedDataReceivingComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamingTaskGenerator.class);

    /**
     * Default value of the {@link #maxParallelProcessedMsgs} attribute.
     */
    private static final int DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES = 10;

    /**
     * Mutex used to wait for the start signal after the component has been started
     * and initialized.
     */
    private Semaphore startTaskGenMutex = new Semaphore(0);
    /**
     * Mutex used to wait for the terminate signal.
     */
    private Semaphore terminateMutex = new Semaphore(0);
    /**
     * The id of this generator.
     */
    protected int generatorId = -1;
    /**
     * The number of task generators created by the benchmark controller.
     */
    protected int numberOfGenerators = -1;
    /**
     * The task id that will be assigned to the next task generated by this
     * generator.
     */
    private long nextTaskId;
    /**
     * The maximum number of incoming messages that are processed in parallel.
     * Additional messages have to wait.
     */
    private final int maxParallelProcessedMsgs;
    /**
     * Sender for sending tasks to the benchmarked system.
     */
    protected DataSender sender2System;
    /**
     * Sender for transferring data to the evaluation storage.
     */
    protected DataSender sender2EvalStore;
    /**
     * Receiver for receiving data from the data generators.
     */
    protected DataReceiver dataReceiver;

    /**
     * Default constructor creating an {@link AbstractStreamingTaskGenerator}
     * processing up to {@link #DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES}=
     * {@value #DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES} messages in parallel.
     */
    public AbstractStreamingTaskGenerator() {
        this(DEFAULT_MAX_PARALLEL_PROCESSED_MESSAGES);
    }

    /**
     * Constructor setting the maximum number of parallel processed messages. Note
     * that this parameter has to be larger or equal to 1 or the {@link #init()}
     * method will throw an exception. Setting
     * <code>maxParallelProcessedMsgs=1</code> leads to the usage of a
     * {@link QueueingConsumer}.
     * 
     * @param maxParallelProcessedMsgs
     *            the number of messaegs that are processed in parallel
     */
    public AbstractStreamingTaskGenerator(int maxParallelProcessedMsgs) {
        this.maxParallelProcessedMsgs = maxParallelProcessedMsgs;
        defaultContainerType = Constants.CONTAINER_TYPE_BENCHMARK;
    }

    @Override
    public void init() throws Exception {
        super.init();

        // If the generator ID is not predefined
        if (generatorId < 0) {
            generatorId = EnvVariables.getInt(Constants.GENERATOR_ID_KEY, LOGGER);
        }
        nextTaskId = generatorId;

        // If the number of generators is not predefined
        if (numberOfGenerators < 0) {
            numberOfGenerators = EnvVariables.getInt(Constants.GENERATOR_COUNT_KEY);
        }

        if (sender2System == null) {
            // We don't need to define an id generator since we will set the IDs
            // while sending data
            sender2System = DataSenderImpl.builder().name("TG" + getGeneratorId() + "-DS-to-SA")
                    .queue(getFactoryForOutgoingDataQueues(),
                            generateSessionQueueName(Constants.TASK_GEN_2_SYSTEM_QUEUE_NAME))
                    .build();
        }
        if (sender2EvalStore == null) {
            // We don't need to define an id generator since we will set the IDs
            // while sending data
            sender2EvalStore = DataSenderImpl.builder().name("TG" + getGeneratorId() + "-DS-to-ES")
                    .queue(getFactoryForOutgoingDataQueues(),
                            generateSessionQueueName(Constants.TASK_GEN_2_EVAL_STORAGE_DEFAULT_QUEUE_NAME))
                    .build();
        }

        if (maxParallelProcessedMsgs > 0) {
            if (dataReceiver == null) {
                dataReceiver = DataReceiverImpl.builder().dataHandler(new GeneratedDataHandler())
                        .name("TG" + getGeneratorId() + "-DR-from-DG")
                        .maxParallelProcessedMsgs(maxParallelProcessedMsgs).queue(getFactoryForOutgoingDataQueues(),
                                generateSessionQueueName(Constants.DATA_GEN_2_TASK_GEN_QUEUE_NAME))
                        .build();
            } else {
                // XXX here we could set the data handler if the data receiver
                // would
                // offer such a method
            }
        } else {
            throw new IllegalArgumentException("The maximum number of messages processed in parallel has to be > 0.");
        }
    }

    @Override
    public void run() throws Exception {
        sendToCmdQueue(Commands.TASK_GENERATOR_READY_SIGNAL);
        // Wait for the start message
        startTaskGenMutex.acquire();

        // Wait for termination message
        terminateMutex.acquire();
        // wait until all messages have been read from the queue
        dataReceiver.closeWhenFinished();
        sender2System.closeWhenFinished();
        sender2EvalStore.closeWhenFinished();
    }

    @Override
    public void receiveGeneratedData(InputStream dataStream) {
        try {
            generateTask(dataStream);
        } catch (Exception e) {
            LOGGER.error("Exception while generating task.", e);
        }
    }

    /**
     * Generates a task from the given data, sends it to the system, takes the
     * timestamp of the moment at which the message has been sent to the system and
     * sends it together with the expected response to the evaluation storage.
     * 
     * @param dataStream
     *            stream containing incoming data generated by a data generator
     * @throws Exception
     *             if a sever error occurred
     */
    protected abstract void generateTask(InputStream dataStream) throws Exception;

    /**
     * Generates the next unique ID for a task.
     * 
     * @return the next unique task ID
     */
    protected synchronized String getNextTaskId() {
        String taskIdString = Long.toString(nextTaskId);
        nextTaskId += numberOfGenerators;
        return taskIdString;
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        // If this is the signal to start the data generation
        if (command == Commands.TASK_GENERATOR_START_SIGNAL) {
            LOGGER.info("Received signal to start.");
            // release the mutex
            startTaskGenMutex.release();
        } else if (command == Commands.DATA_GENERATION_FINISHED) {
            LOGGER.info("Received signal to finish.");
            // release the mutex
            terminateMutex.release();
        }
        super.receiveCommand(command, data);
    }

    /**
     * This method sends the data read from the given {@link InputStream} and the
     * given timestamp of the task with the given task id to the evaluation storage.
     * 
     * @param taskIdString
     *            the id of the task
     * @param timestamp
     *            the timestamp of the moment in which the task has been sent to the
     *            system
     * @param stream
     *            stream from which the expected response for the task with the
     *            given id will be read
     * @throws IOException
     *             if there is an error during the sending
     */
    protected void sendTaskToEvalStorage(String taskIdString, long timestamp, InputStream stream) throws IOException {
        InputStream precedingStream = new ByteArrayInputStream(RabbitMQUtils.writeByteArrays(
                RabbitMQUtils.writeLong(timestamp), new byte[][] { RabbitMQUtils.writeString(taskIdString) }, null));
        SequenceInputStream concatenatedStreams = new SequenceInputStream(precedingStream, stream);
        sender2EvalStore.sendData(concatenatedStreams, taskIdString);
    }

    /**
     * Sends the given task with the given task id and data to the system.
     * 
     * @param taskIdString
     *            the id of the task
     * @param data
     *            the data of the task
     * @throws IOException
     *             if there is an error during the sending
     */
    protected void sendTaskToSystemAdapter(String taskIdString, InputStream stream) throws IOException {
        InputStream precedingStream = new ByteArrayInputStream(
                RabbitMQUtils.writeByteArrays(new byte[][] { RabbitMQUtils.writeString(taskIdString) }));
        SequenceInputStream concatenatedStreams = new SequenceInputStream(precedingStream, stream);
        sender2System.sendData(concatenatedStreams, taskIdString);
    }

    public int getGeneratorId() {
        return generatorId;
    }

    public int getNumberOfGenerators() {
        return numberOfGenerators;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(dataReceiver);
        IOUtils.closeQuietly(sender2EvalStore);
        IOUtils.closeQuietly(sender2System);
        super.close();
    }

    /**
     * A simple internal handler class that calls
     * {@link AbstractStreamingTaskGenerator#generateTask(InputStream)} with the
     * given {@link InputStream}.
     * 
     * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
     *
     */
    protected class GeneratedDataHandler implements IncomingStreamHandler {

        @Override
        public void handleIncomingStream(String streamId, InputStream stream) {
            try {
                generateTask(stream);
            } catch (Exception e) {
                LOGGER.error("Exception while generating task. Aborting.", e);
                System.exit(1);
            }
        }
    }
}
