package org.hobbit.core.rabbit;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.io.Charsets;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;

public class RabbitMQUtilsTest {

    @Test
    public void testStrings() {
        performStringTestUsingByteArray("");
        performStringTestUsingByteArrayWithOffset("");
        performStringTestUsingByteBuffer("");

        performStringTestUsingByteArray("test");
        performStringTestUsingByteArrayWithOffset("test");
        performStringTestUsingByteBuffer("test");
    }

    private void performStringTestUsingByteArray(String original) {
        byte[] data = RabbitMQUtils.writeString(original);
        String readString = RabbitMQUtils.readString(data);
        Assert.assertEquals(original, readString);
    }

    private void performStringTestUsingByteArrayWithOffset(String original) {
        int tempLength = 19;
        byte[] tempData = new byte[tempLength];
        byte[] data = RabbitMQUtils.writeString(original);
        tempData = Arrays.copyOf(tempData, tempLength + data.length);
        System.arraycopy(data, 0, tempData, tempLength, data.length);
        String readString = RabbitMQUtils.readString(tempData, tempLength, data.length);
        Assert.assertEquals(original, readString);
    }

    private void performStringTestUsingByteBuffer(String original) {
        byte[] data = RabbitMQUtils.writeString(original);
        ByteBuffer buffer = ByteBuffer.allocate(data.length + 4);
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.position(0);
        String readString = RabbitMQUtils.readString(buffer);
        Assert.assertEquals(original, readString);
    }

    @Test
    public void testByteArrays() {
        performByteArraysTest(new byte[][] { new byte[0], new byte[0], new byte[0] });
        performByteArraysTest(new byte[][] { new byte[0], new byte[0], new byte[100] });
        performByteArraysTest(new byte[][] { "test".getBytes(Charsets.UTF_8), new byte[0],
                "one more test".getBytes(Charsets.ISO_8859_1) });
    }

    private void performByteArraysTest(byte[][] arrays) {
        byte[] data = RabbitMQUtils.writeByteArrays(arrays);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        for (int i = 0; i < arrays.length; ++i) {
            Assert.assertArrayEquals(arrays[i], RabbitMQUtils.readByteArray(buffer));
        }
    }

    @Test
    public void testRdfModels() {
        performModelsTest(ModelFactory.createDefaultModel());

        Model model = ModelFactory.createDefaultModel();
        model.add(model.getResource("http://example.org/resource1"), model.getProperty("http://example.org/property1"),
                model.createTypedLiteral(true));
        performModelsTest(model);

        model.add(model.getResource("http://example.org/resource1"), model.getProperty("http://example.org/property2"),
                model.createTypedLiteral(3.99));
        model.add(model.getResource("http://example.org/resource1"), model.getProperty("http://example.org/property3"),
                model.getResource("http://example.org/resource3"));
        performModelsTest(model);
    }

    private void performModelsTest(Model model) {
        byte[] data = RabbitMQUtils.writeModel(model);
        compareModels(model, RabbitMQUtils.readModel(data));
        data = RabbitMQUtils.writeByteArrays(new byte[][] { data });
        compareModels(model, RabbitMQUtils.readModel(ByteBuffer.wrap(data)));
    }

    private void compareModels(Model expectedModel, Model actualModel) {
        String expectedModelString = expectedModel.toString();
        String actualModelString = expectedModel.toString();

        Assert.assertEquals("Different number of triples expectedModel=" + expectedModelString + "\nactualModel="
                + actualModelString, expectedModel.size(), actualModel.size());

        Assert.assertTrue(
                "The acutal model does not contain all tirples of the expected model. expectedModel="
                        + expectedModelString + "\nactualModel=" + actualModelString,
                expectedModel.containsAll(actualModel));
    }
}