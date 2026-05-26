package com.orasaka.e2e;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Live AMQP infrastructure accessor for E2E assertions.
 *
 * <p>Connects directly to the provisioned RabbitMQ container via system properties injected by
 * Failsafe. No mocking, no Spring context — raw amqp-client against the live RabbitMQ instance.
 */
final class E2eAmqpClient {

  private static final String AMQP_HOST = System.getProperty("amqp.host", "localhost");
  private static final int AMQP_PORT = Integer.parseInt(System.getProperty("amqp.port", "5672"));

  private E2eAmqpClient() {}

  /**
   * Checks if RabbitMQ is connectable by opening and immediately closing a connection.
   *
   * @return true if the TCP handshake and AMQP protocol negotiation succeed.
   */
  static boolean isConnectable() {
    ConnectionFactory factory = createFactory();
    try (Connection conn = factory.newConnection()) {
      return conn.isOpen();
    } catch (IOException | TimeoutException e) {
      return false;
    }
  }

  /**
   * Checks if a queue with the given name exists in RabbitMQ.
   *
   * @param queueName The queue name to probe.
   * @return true if the queue exists and is declared.
   */
  static boolean queueExists(String queueName) {
    ConnectionFactory factory = createFactory();
    try (Connection conn = factory.newConnection();
        Channel channel = conn.createChannel()) {
      channel.queueDeclarePassive(queueName);
      return true;
    } catch (IOException | TimeoutException e) {
      return false;
    }
  }

  /**
   * Returns the message count of a queue.
   *
   * @param queueName The queue name to inspect.
   * @return Number of messages, or -1 if the queue does not exist.
   */
  static long getQueueMessageCount(String queueName) {
    ConnectionFactory factory = createFactory();
    try (Connection conn = factory.newConnection();
        Channel channel = conn.createChannel()) {
      return channel.queueDeclarePassive(queueName).getMessageCount();
    } catch (IOException | TimeoutException e) {
      return -1;
    }
  }

  private static ConnectionFactory createFactory() {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(AMQP_HOST);
    factory.setPort(AMQP_PORT);
    factory.setUsername("guest");
    factory.setPassword("guest");
    factory.setConnectionTimeout(5000);
    return factory;
  }
}
